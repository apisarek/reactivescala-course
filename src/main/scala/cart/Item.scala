package cart

import java.net.URI

final case class Item(id: URI, name: String, price: BigDecimal, count: Int = 1)
