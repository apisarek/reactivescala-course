package shop

import akka.actor.{ActorSystem, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import cart.Item
import com.typesafe.config.ConfigFactory
import net.liftweb.json.Serialization.write
import net.liftweb.json._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.io.StdIn




object StatsApp extends Directives {


  def main(args: Array[String]) {

    val config = ConfigFactory.load()
    implicit val system: ActorSystem = ActorSystem(config.getString("application.name"), config)
    val statActor = system.actorOf(Props[RequestsStatsActor], "stats")
    val logActor = system.actorOf(Props[RequestsLogActor], "logo")
    val productCatalogRouter = system.actorOf(Props[ProductCatalogRouter])
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher


    implicit val formats = DefaultFormats
    val mediator = DistributedPubSub(system).mediator
    val route =
      path("stats") {
        get {

          implicit val timeout = Timeout(10 seconds)
          val future = statActor ? Stats
          logActor ! Stats
          val result = Await.result(future, timeout.duration).asInstanceOf[Map[String, Int]]

          val responseString: String = write(result)
          complete(HttpEntity(ContentTypes.`application/json`, responseString))

        }
      }

    val ip = config.getString("clustering.ip")
    val bindingFuture = Http().bindAndHandle(route, ip, 10000)

    println(s"Server online at http://$ip:10000/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}

