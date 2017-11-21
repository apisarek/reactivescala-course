package shop

import java.net.URI

import cart.Item

case class ProductCatalog(path: String) {
  private val lines = scala.io.Source.fromFile(path).getLines

  def topMatching(query: String, k: Int = 10): List[Item] = {
    getProductCatalog(path)
      .sortBy(-score(_, query))
      .take(k)
      .toList
  }

  private def getProductCatalog(path: String): Stream[Item] = {
    lines.toStream.tail
      .map(_.split(",").map(_.replace("\"", "")))
      .filter(_.length > 2)
      .map(
        x => {
          val name = x(1) + " " + x(2).trim
          val uri = URI.create(x(0))
          val price = x(2).length
          val count = name.length
          Item(uri, name, price, count)
        }
      )
      .filterNot(x => x.name contains "NULL")
  }

  private def score(item: Item, query: String): Int = {
    val itemParts = item.name.split(" ").map(_.toLowerCase)
    val queryParts = query.split(" ").map(_.toLowerCase)
    queryParts.map(itemParts contains _).map(if (_) 1 else 0).sum
  }

}
