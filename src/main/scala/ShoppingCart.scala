import java.net.URI

case class ShoppingCart(items: Map[URI, Item] = Map.empty) {
  def addItem(item: Item): ShoppingCart = {
    val currentCount = if (items contains item.id) items(item.id).count else 0
    copy(items = items.updated(item.id, item.copy(count = currentCount + item.count)))
  }
  def removeItem(id: URI, count: Int = 1): ShoppingCart = {
    val itemSeeked = items.get(id)
    itemSeeked match {
      case None => this
      case Some(foundItem) =>
        val foundCount = foundItem.count
        val countAfter = foundCount - count
        if (countAfter <= 0)
          copy(items = items - id)
        else
          copy(items = items.updated(id, foundItem.copy(count = countAfter)))
    }
  }
  def count: Int = items.values.map(_.count).sum
  def isEmpty: Boolean = count <= 0
}

case class Item(id: URI, name: String, price: BigDecimal, count: Int = 1)