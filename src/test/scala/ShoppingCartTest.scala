import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class ShoppingCartTest extends FlatSpec
  with Matchers
  with BeforeAndAfterAll with Eventually {


  val bigos = "bigos"
  val jajka = "jajka"

  "ShoppingCart" should "start in Empty state" in {
    val shoppingCart = ShoppingCart()
    shoppingCart.items shouldBe Map.empty
  }
}

