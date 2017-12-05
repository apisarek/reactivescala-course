package shop

import akka.actor.{Actor, Props, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import shop.ProductCatalogMessages.TopMatching

class ProductCatalogRouter extends Actor {
  var router = {
    val routees = Vector.fill(3) {
      val r = context.actorOf(Props(new ProductCatalogManager(ProductCatalog("../query_result"))))
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  override def receive = {
    case msg: TopMatching =>
      router.route(msg, sender())
    case Terminated(a) =>
      router = router.removeRoutee(a)
      val r = context.actorOf(Props[ProductCatalogManager])
      context watch r
      router = router.addRoutee(r)
  }
}
