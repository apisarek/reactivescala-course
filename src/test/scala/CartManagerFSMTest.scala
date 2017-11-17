import java.net.URI

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import cart._
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import shop.CustomerMessages

import scala.concurrent.duration._

class CartManagerFSMTest extends TestKit(ActorSystem("CartFSMTest"))
  with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll
  with Eventually
  with ImplicitSender
{
  import cart.CartMessages._
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  private val customer = TestProbe()
  private val bigosURI: URI = URI.create("bigos1")
  private val bigos: Item = Item(bigosURI, "bigos", BigDecimal(0))
  private val jajkaURI: URI = URI.create("jajka1")
  private val jajka: Item = Item(jajkaURI, "jajka", BigDecimal(0))

  "Cart" should "start in Empty state" in {
    val id = "cart-1"
    val cart = system.actorOf(Props(new CartManagerFSM(customer.ref, id)))
    cart ! ItemAdded(bigos)
    cart ! GetShoppingCart

    val shoppingCart = ShoppingCart(Map(bigosURI -> bigos))
    expectMsg(shoppingCart)
  }

  "Cart" should "persist state and data" in {
    val time = 20.seconds
    val id = "cart-2"
    val cart = system.actorOf(Props(new CartManagerFSM(customer.ref, id, time)))
    cart ! ItemAdded(bigos)
    val shoppingCart = ShoppingCart(Map(bigosURI -> bigos))

    cart ! GetShoppingCart
    expectMsg(shoppingCart)
    Thread.sleep(100)
    cart ! PoisonPill

    val cart2 = system.actorOf(Props(new CartManagerFSM(customer.ref, id, time)))

    cart2 ! GetShoppingCart
    expectMsg(shoppingCart)
  }

  "Cart" should "persist timer" in {
    val time = 1.seconds
    val id = "cart-3"
    val cart = system.actorOf(Props(new CartManagerFSM(customer.ref, id, time)))
    cart ! ItemAdded(bigos)
    val shoppingCart = ShoppingCart(Map(bigosURI -> bigos))

    cart ! GetShoppingCart
    expectMsg(shoppingCart)
    Thread.sleep(time.toMillis)
    cart ! PoisonPill

    val cart2 = system.actorOf(Props(new CartManagerFSM(customer.ref, id, time)))

    Thread.sleep((1.5 * time.toMillis).asInstanceOf[Int])
    cart2 ! GetShoppingCart
    expectMsg(ShoppingCart())
  }



  "Cart" should "stay in NonEmpty state after adding two products and removing one" in {
    val time = 1.seconds
    val id = "cart-4"
    val cart = system.actorOf(Props(new CartManagerFSM(customer.ref, id, time)))
    cart ! ItemAdded(bigos)
    cart ! ItemAdded(bigos)
    cart ! ItemRemoved(bigos.id)
    val shoppingCart = ShoppingCart(Map(bigosURI -> bigos))

    cart ! GetShoppingCart
    expectMsg(shoppingCart)
  }
  "Cart" should "return to Empty state after adding and removing one item" in {
    val time = 1.seconds
    val id = "cart-5"
    val cart = system.actorOf(Props(new CartManagerFSM(customer.ref, id, time)))
    cart ! ItemAdded(bigos)
    cart ! ItemRemoved(bigos.id)

    cart ! GetShoppingCart
    expectMsg(ShoppingCart())
  }

  "Cart" should "go to InCheckout after adding product and checking out" in {
    val time = 1.seconds
    val id = "cart-6"
    val cart = system.actorOf(Props(new CartManagerFSM(customer.ref, id, time)))
    cart ! ItemAdded(bigos)
    cart ! CartMessages.CheckoutStarted

    customer.expectMsgPF() {
      case CustomerMessages.CheckoutStarted(_)  => ()
    }
  }

  "Cart" should "go to NonEmpty after checking with product and canceling" in {
    val time = 1.seconds
    val id = "cart-7"
    val cart = system.actorOf(Props(new CartManagerFSM(customer.ref, id, time)))
    cart ! ItemAdded(bigos)
    cart ! CartMessages.CheckoutStarted
    cart ! CartMessages.CheckoutCanceled
    cart ! GetShoppingCart
    val shoppingCart = ShoppingCart(Map(bigosURI -> bigos))
    expectMsg(shoppingCart)
    cart ! GetStateData
    expectMsgPF() {
      case CartManagerContent(_) => ()
    }
  }

  "Cart" should "go to Empty after adding checking with product and closing" in {
    val time = 1.seconds
    val id = "cart-8"
    val cart = system.actorOf(Props(new CartManagerFSM(customer.ref, id, time)))
    cart ! ItemAdded(bigos)
    cart ! CheckoutStarted
    cart ! CheckoutClosed
    cart ! GetShoppingCart
    expectMsg(ShoppingCart())
  }

  "Cart" should "go to Empty after adding product and expiring" in {
    val time = 1.seconds
    val id = "cart-9"
    val cart = system.actorOf(Props(new CartManagerFSM(customer.ref, id, time)))
    cart ! ItemAdded(bigos)
    Thread.sleep(2 * time.toMillis)
    cart ! GetShoppingCart
    expectMsg(ShoppingCart())
  }
}

