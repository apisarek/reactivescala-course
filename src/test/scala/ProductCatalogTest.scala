import java.net.URI

import cart.Item
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class ProductCatalogTest extends FlatSpec
  with Matchers
  with BeforeAndAfterAll with Eventually {

  "cart.ShoppingCart" should "start in Empty state" in {
    val catalog = getProductCatalog("../query_result")
    val top = topMatching(catalog, "dupa")
    top.foreach(println)
  }

  def getProductCatalog(path: String): Stream[Item] = {
    val lines = io.Source.fromFile(path).getLines
    lines.toStream.tail
      .map(
        line => line.split(",").map(_.replace("\"", ""))
      )
      .filter(x => x.length > 2)
      .map(
        x => {
          val name = x(1) + " " + x(2)
          val uri = URI.create(x(0))
          val price = x(2).length
          Item(uri, name.trim, price, name.length)
        }
      )
      .filterNot(x => x.name contains "NULL")
  }

  def topMatching(productCatalog: Stream[Item], query: String, k: Int = 10): Stream[Item] = {
    productCatalog
      .sortBy(-score(_, query))
      .take(k)
  }

  def score(item: Item, query: String): Int = {
    val itemParts = item.name.split(" ")
    val queryParts = query.split(" ")
    0
  }

}



