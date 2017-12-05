package shop

import java.net.URI

import cart.Item

case class ProductCatalog(path: String) {
  private val lines = scala.io.Source.fromFile(path).getLines

  private val productCatalog =
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
      .toList

  def topMatching(query: String, k: Int = 10): List[Item] = {
    productCatalog
      .sortBy(-score(_, query))
      .take(k)
  }


  private def score(item: Item, query: String): Int = {
    val itemParts: Array[String] = item.name.split(" ").map(_.toLowerCase)
    val queryParts: Array[String] = query.split(" ").map(_.toLowerCase)
    queryParts.map(overlap(itemParts, _)).sum
  }

  private def overlap(itemParts: Array[String], queryPart: String) : Int = {
    itemParts.map(_ == queryPart).map(if (_) 1 else 0).sum
  }

}


