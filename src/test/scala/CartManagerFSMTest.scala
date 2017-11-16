import java.net.URI

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestFSMRef, TestKit}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._

class CartManagerFSMTest extends TestKit(ActorSystem("CartFSMTest"))
  with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll with Eventually {
  import Cart._
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  private val customer: ActorRef = system.actorOf(Props[Customer])
  private val bigosURI: URI = URI.create("bigos1")
  private val bigos: Item = Item(bigosURI, "bigos", BigDecimal(0))
  private val jajkaURI: URI = URI.create("jajka1")
  private val jajka: Item = Item(jajkaURI, "jajka", BigDecimal(0))

  "Cart" should "start in Empty state" in {
    val cart = TestFSMRef(new CartManagerFSM(customer))
    cart.stateName shouldBe Empty
    cart.stateData shouldBe CartManagerContent()
  }

  "Cart" should "go to NonEmpty state after adding product" in {
    val cart = TestFSMRef(new CartManagerFSM(customer))
    cart ! ItemAdded(bigos)
    cart.stateName shouldBe NonEmpty
    cart.isStateTimerActive shouldBe true
    cart.stateData shouldBe CartManagerContent(ShoppingCart(Map(bigosURI -> bigos)))
  }

  "Cart" should "stay in NonEmpty state after adding products" in {
    val cart = TestFSMRef(new CartManagerFSM(customer))
    cart ! ItemAdded(bigos)
    cart ! ItemAdded(bigos)
    cart.stateName shouldBe NonEmpty
    cart.stateData shouldBe CartManagerContent(ShoppingCart(Map(bigosURI -> bigos.copy(count = 2))))
  }

  "Cart" should "stay in NonEmpty state after adding two products and removing one" in {
    val cart = TestFSMRef(new CartManagerFSM(customer))
    cart ! ItemAdded(bigos)
    cart ! ItemAdded(jajka)
    cart.stateName shouldBe NonEmpty
    cart.stateData shouldBe CartManagerContent(ShoppingCart(Map(
      bigosURI -> bigos,
      jajkaURI -> jajka
    )))
    cart ! ItemRemoved(jajka)
    cart.stateName shouldBe NonEmpty
    cart.stateData shouldBe CartManagerContent(ShoppingCart(Map(
      bigosURI -> bigos,
    )))
  }
  "Cart" should "return to Empty state after adding and removing one item" in {
    val cart = TestFSMRef(new CartManagerFSM(customer))
    cart ! ItemAdded(bigos)
    cart ! ItemRemoved(bigos)
    cart.stateName shouldBe Empty
    cart.stateData shouldBe CartManagerContent(ShoppingCart(Map.empty))
  }

  "Cart" should "go to InCheckout after adding product and checking out" in {
    val cart = TestFSMRef(new CartManagerFSM(customer))
    cart ! ItemAdded(bigos)
    cart ! CheckoutStarted
    cart.stateName shouldBe InCheckout
  }

  "Cart" should "go to NonEmpty after checking with product and canceling" in {
    val cart = TestFSMRef(new CartManagerFSM(customer))
    cart ! ItemAdded(bigos)
    cart ! CheckoutStarted
    cart ! CheckoutCanceled
    cart.stateName shouldBe NonEmpty
    cart.stateData shouldBe CartManagerContent(ShoppingCart(Map(bigosURI -> bigos)))
  }

  "Cart" should "go to Empty after adding checking with product and closing" in {
    val cart = TestFSMRef(new CartManagerFSM(customer))
    cart ! ItemAdded(bigos)
    cart ! CheckoutStarted
    cart ! CheckoutClosed
    cart.stateName shouldBe Empty
    cart.stateData shouldBe CartManagerContent(ShoppingCart())
  }

  "Cart" should "go to Empty after adding product and expiring" in {
    val cart = TestFSMRef(new CartManagerFSM(customer, 100.millis))
    cart ! ItemAdded(bigos)
    eventually { // waits 150 milliseconds by default
      cart.stateName shouldBe Empty
      cart.stateData shouldBe CartManagerContent()
    }
  }
}

