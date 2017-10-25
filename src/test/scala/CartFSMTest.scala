import akka.actor.ActorSystem
import akka.testkit.{TestFSMRef, TestKit}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._

class CartFSMTest extends TestKit(ActorSystem("CartFSMTest"))
  with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll with Eventually {

  import Cart._

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }


  "Cart" should "start in Empty state" in {
    val cart = TestFSMRef(new CartFSM())
    cart.stateName shouldBe Empty
    cart.stateData shouldBe CartContent(0)
  }

  "Cart" should "go to NonEmpty state after adding product" in {
    val cart = TestFSMRef(new CartFSM())
    cart ! ItemAdded
    cart.stateName shouldBe NonEmpty
    cart.isStateTimerActive shouldBe true
    cart.stateData shouldBe CartContent(1)
  }

  "Cart" should "stay in NonEmpty state after adding products" in {
    val cart = TestFSMRef(new CartFSM())
    cart ! ItemAdded
    cart ! ItemAdded
    cart.stateName shouldBe NonEmpty
    cart.stateData shouldBe CartContent(2)
  }

  "Cart" should "stay in NonEmpty state after adding two products and removing one" in {
    val cart = TestFSMRef(new CartFSM())
    cart ! ItemAdded
    cart ! ItemAdded
    cart.stateName shouldBe NonEmpty
    cart.stateData shouldBe CartContent(2)
    cart ! ItemRemoved
    cart.stateName shouldBe NonEmpty
    cart.stateData shouldBe CartContent(1)

  }
  "Cart" should "return to Empty state after adding and removing one item" in {
    val cart = TestFSMRef(new CartFSM())
    cart ! ItemAdded
    cart ! ItemRemoved
    cart.stateName shouldBe Empty
  }

  "Cart" should "go to InCheckout after adding product and checking out" in {
    val cart = TestFSMRef(new CartFSM())
    cart ! ItemAdded
    cart ! CheckoutStarted
    cart.stateName shouldBe InCheckout
  }

  "Cart" should "go to NonEmpty after checking with product and canceling" in {
    val cart = TestFSMRef(new CartFSM())
    cart ! ItemAdded
    cart ! CheckoutStarted
    cart ! CheckoutCanceled
    cart.stateName shouldBe NonEmpty
    cart.stateData shouldBe CartContent(1)
  }


  "Cart" should "go to Empty after adding checking with product and closing" in {
    val cart = TestFSMRef(new CartFSM())
    cart ! ItemAdded
    cart ! CheckoutStarted
    cart ! CheckoutClosed
    cart.stateName shouldBe Empty
    cart.stateData shouldBe CartContent(0)
  }

  "Cart" should "go to Empty after adding product and expiring" in {
    val cart = TestFSMRef(new CartFSM(100.millis))
    cart ! ItemAdded
    eventually { // waits 150 milliseconds by default
      cart.stateName shouldBe Empty
      cart.stateData shouldBe CartContent(0)
    }
  }
}

