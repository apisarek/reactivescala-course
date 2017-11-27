package shop

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import cart.Item
import com.typesafe.config.ConfigFactory
import shop.ProductCatalogMessages.TopMatching

object Main extends App {
  val config = ConfigFactory.load()
  val catalogSystem = ActorSystem("productCatalog", config.getConfig("productcatalog").withFallback(config))
  val mainSystem = ActorSystem("main", config.getConfig("main").withFallback(config))
  catalogSystem.actorOf(
    Props(new ProductCatalogManager(ProductCatalog("../query_result"))),
    "productCatalog"
  )
  val mainActor = mainSystem.actorOf(Props[MainActor])
  mainActor ! TopMatching("gillette", 10)
}

class MainActor extends Actor {
  private val catalog = context.system.actorSelection("akka.tcp://productCatalog@127.0.0.1:2553/user/productCatalog")
  def receive: Receive = {
    case msg: TopMatching => catalog ! msg
    case result: List[Item] => result.foreach(println)
  }
}
