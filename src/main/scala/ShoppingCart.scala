case class ShoppingCart(items: Map[String, Int] = Map.empty) {
  def addItem(item: String): ShoppingCart = {
    val currentCount: Int = items.getOrElse(item, 0)
    val newCount = currentCount + 1
    ShoppingCart(items.updated(item, newCount))
  }
  def removeItem(item: String): ShoppingCart = {
    val currentCount = items.get(item)
    currentCount match {
      case None =>
        this // removing non-existing item results in no action
      case Some(1) =>
        ShoppingCart(items - item)
      case Some(count) =>
        ShoppingCart(items.updated(item, count - 1))
    }
  }
  def count: Int = items.values.sum
}
