import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestFSMRef, TestKit}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._

class CartManagerFSMTest extends TestKit(ActorSystem("CartFSMTest"))
  with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll with Eventually {

  import Cart._
  val customer = system.actorOf(Props[Customer])
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
  val bigos = "bigos"
  val jajka = "jajka"

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
    cart.stateData shouldBe CartManagerContent(ShoppingCart(Map(bigos -> 1)))
  }

  "Cart" should "stay in NonEmpty state after adding products" in {
    val cart = TestFSMRef(new CartManagerFSM(customer))
    cart ! ItemAdded(bigos)
    cart ! ItemAdded(bigos)
    cart.stateName shouldBe NonEmpty
    cart.stateData shouldBe CartManagerContent(ShoppingCart(Map(bigos -> 2)))
  }

  "Cart" should "stay in NonEmpty state after adding two products and removing one" in {
    val cart = TestFSMRef(new CartManagerFSM(customer))
    cart ! ItemAdded(bigos)
    cart ! ItemAdded("jajka")
    cart.stateName shouldBe NonEmpty
    cart.stateData shouldBe CartManagerContent(ShoppingCart(Map(
      bigos -> 1,
      jajka -> 1
    )))
    cart ! ItemRemoved("jajka")
    cart.stateName shouldBe NonEmpty
    cart.stateData shouldBe CartManagerContent(ShoppingCart(Map(
      bigos -> 1,
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
    cart.stateData shouldBe CartManagerContent(ShoppingCart(Map(bigos -> 1)))
  }

  "Cart" should "go to Empty after adding checking with product and closing" in {
    val cart = TestFSMRef(new CartManagerFSM(customer))
    cart ! ItemAdded
    cart ! CheckoutStarted
    cart ! CheckoutClosed
    cart.stateName shouldBe Empty
    cart.stateData shouldBe CartManagerContent(ShoppingCart)
  }
//
//  "Cart" should "go to Empty after adding product and expiring" in {
//    val cart = TestFSMRef(new CartManagerFSM(customer, 100 millis))
//    cart ! ItemAdded
//    eventually { // waits 150 milliseconds by default
//      cart.stateName shouldBe Empty
//      cart.stateData shouldBe CartManagerContent(0)
//    }
//  }
}

