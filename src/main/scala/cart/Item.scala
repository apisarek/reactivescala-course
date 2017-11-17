package cart

import java.net.URI

case class Item(id: URI, name: String, price: BigDecimal, count: Int = 1)
