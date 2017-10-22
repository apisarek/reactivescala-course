import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._

class CartTest extends TestKit(ActorSystem("CartTest"))
  with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll with Eventually {

  import Cart._

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }


  "Cart" should "start in Empty state" in {
    val cartRef = TestActorRef(new Cart())
    val cart = cartRef.underlyingActor
    cart.items shouldBe 0
  }

  "Cart" should "go to NonEmpty state after adding product" in {
    val cartRef = TestActorRef(new Cart())
    val cart = cartRef.underlyingActor
    cartRef ! ItemAdded
    cart.items shouldBe 1
  }
  "Cart" should "stay in NonEmpty state after adding products" in {
    val cartRef = TestActorRef(new Cart())
    val cart = cartRef.underlyingActor
    cartRef ! ItemAdded
    cartRef ! ItemAdded
    cart.items shouldBe 2
  }

  "Cart" should "stay in NonEmpty state after adding two products and removing one" in {
    val cartRef = TestActorRef(new Cart())
    val cart = cartRef.underlyingActor
    cartRef ! ItemAdded
    cartRef ! ItemAdded
    cart.items shouldBe 2
    cartRef ! ItemRemoved
    cart.items shouldBe 1
  }
  "Cart" should "return to Empty state after adding and removing one item" in {
    val cartRef = TestActorRef(new Cart())
    val cart = cartRef.underlyingActor
    cartRef ! ItemAdded
    cartRef ! ItemRemoved
    cart.items shouldBe 0
  }

  "Cart" should "not expire after going to checkout " in {
    val cartRef = TestActorRef(new Cart(100 millis))
    val cart = cartRef.underlyingActor
    within(200 millis) {
      awaitAssert(() => {
        if (cart.items < 1) throw new UnknownError()
      }, interval = 20.millis)
    }

  }
  "Cart" should "go to NonEmpty after checking with product and canceling" in {
    val cartRef = TestActorRef(new Cart(100 millis))
    val cart = cartRef.underlyingActor
    cartRef ! ItemAdded
    cartRef ! CheckoutStarted
    cartRef ! CheckoutCanceled
    cart.items shouldBe 1
  }


  "Cart" should "go to Empty after adding checking with product and closing" in {
    val cartRef = TestActorRef(new Cart(100 millis))
    val cart = cartRef.underlyingActor
    cartRef ! ItemAdded
    cartRef ! CheckoutStarted
    cartRef ! CheckoutClosed
    cart.items shouldBe 0
  }

  "Cart" should "go to Empty after adding product and expiring" in {
    val cartRef = TestActorRef(new Cart(100 millis))
    val cart = cartRef.underlyingActor
    cartRef ! ItemAdded
    eventually {
      cart.items shouldBe 0
    }
  }
}

