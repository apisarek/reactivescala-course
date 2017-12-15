package shop

import java.net.URI

import akka.actor.{ActorSystem, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import cart.Item
import com.typesafe.config.ConfigFactory
import net.liftweb.json.Serialization.write
import net.liftweb.json._
import shop.ProductCatalogMessages.TopMatching

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.io.StdIn

object BigDecimalSerializer extends Serializer[BigDecimal] {
  private val Class = classOf[BigDecimal]

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), BigDecimal] = {
    case (TypeInfo(Class, _), json) => json match {
      case JInt(iv) => BigDecimal(iv)
      case JDouble(dv) => BigDecimal(dv)
      case value => throw new MappingException("Can't convert " + value + " to " + Class)
    }
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case d: BigDecimal => JDouble(d.doubleValue)
  }
}

object URISerializer extends Serializer[URI] {
  private val Class = classOf[URI]

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), URI] = {
    case (TypeInfo(Class, _), json) => json match {
      case JString(d) => URI.create(d)
      case value => throw new MappingException("Can't convert " + value + " to " + Class)
    }
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case d: URI => JString(d.toASCIIString)
  }
}


final case class Response(items: List[Item])

case class PublishRequest(request: String)

object ProductCatalogService extends Directives {
  def main(args: Array[String]) {

    val config = ConfigFactory.load()
    implicit val system: ActorSystem = ActorSystem(config.getString("application.name"), config)
    val productCatalogRouter = system.actorOf(Props[ProductCatalogRouter])
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher


    implicit val formats = DefaultFormats + BigDecimalSerializer + URISerializer

    val mediator = DistributedPubSub(system).mediator
    val route =
      path("hello") {
        parameters('query) { (query) =>
          get {
            mediator ! Publish("requests", PublishRequest(query.toString()))
            implicit val timeout = Timeout(10 seconds)
            val future = productCatalogRouter ? TopMatching(query, 10)
            val result = Await.result(future, timeout.duration).asInstanceOf[List[Item]]

            val responseString = write(Response(result))
            complete(HttpEntity(ContentTypes.`application/json`, responseString))

          }

        }

      }

    val ip = config.getString("clustering.ip")
    val port = config.getInt("application.exposed-port")
    val bindingFuture = Http().bindAndHandle(route, ip, port)

    println(s"Server online at http://$ip:$port/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}