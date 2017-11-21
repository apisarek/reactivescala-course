package shop

import akka.actor.Actor
import shop.ProductCatalogMessages.TopMatching

class ProductCatalogManager(catalog: ProductCatalog) extends Actor {
  override def receive: Receive = {
    case TopMatching(query, k) => sender() ! catalog.topMatching(query, k)
  }
}

object ProductCatalogMessages {

  case class TopMatching(query: String, k: Int)

}
