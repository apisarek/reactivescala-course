import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestFSMRef, TestKit, TestProbe}
import checkout.{Cancelled, CheckoutFSM, CheckoutMessages, SelectingDelivery}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import shop.Utils.generateID

import scala.concurrent.duration._

class CheckoutFSMTest extends TestKit(ActorSystem("CheckoutFSMTest"))
  with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll
  with Eventually
  with ImplicitSender {

  val customer = TestProbe()
  val cart = TestProbe()

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Checkout" should "start in SelectingDelivery" in {
    val id = "checkout-1"
    val checkout = system.actorOf(Props(new CheckoutFSM(cart.ref, customer.ref, id)))
    checkout ! CheckoutMessages.GetStateName
    expectMsg(SelectingDelivery)
  }

  "Checkout" should "be cancellable after creation" in {
    val id = "checkout-2"
    val checkout = system.actorOf(Props(new CheckoutFSM(cart.ref, customer.ref, id)))
    checkout ! CheckoutMessages.Cancelled
    checkout ! CheckoutMessages.GetStateName
    expectMsg(Cancelled)

  }

  "Checkout" should "expire and goto Cancelled " in {
    val id = "checkout-3"
    val checkout = system.actorOf(Props(new CheckoutFSM(cart.ref, customer.ref, id, checkoutExpirationTime = 100.millis)))
    Thread.sleep(100)
    checkout ! CheckoutMessages.GetStateName
    expectMsg(Cancelled)
  }

  "Checkout" should "should restore timers" in {
    val id = "checkout-4"
    val checkout = system.actorOf(Props(new CheckoutFSM(cart.ref, customer.ref, id, checkoutExpirationTime = 1.seconds)))
    checkout ! CheckoutMessages.GetStateName
    expectMsg(SelectingDelivery)

    checkout ! PoisonPill

    val checkout2 = system.actorOf(Props(new CheckoutFSM(cart.ref, customer.ref, id, checkoutExpirationTime = 1.seconds)))
    Thread.sleep(1500)
    checkout2 ! CheckoutMessages.GetStateName
    expectMsg(Cancelled)
  }


}









//    "Checkout" should "go to SelectingPaymentMethod after delivery method selection" in {
//      val checkout = TestFSMRef(new CheckoutFSM(cart, customer))
//      checkout ! CheckoutMessages.DeliveryMethodSelected("deliveryMethod")
//      checkout.stateName shouldBe SelectingPaymentMethod
//      checkout.stateData shouldBe CheckoutParameters("deliveryMethod")
//    }
//
//  "Checkout" should "be cancellable after delivery method selection" in {
//    val checkout = TestFSMRef(new CheckoutFSM(cart, customer))
//    checkout ! CheckoutMessages.DeliveryMethodSelected("deliveryMethod")
//    checkout.stateName shouldBe SelectingPaymentMethod
//
//    checkout ! CheckoutMessages.Cancelled
//    checkout.stateName shouldBe Cancelled
//  }
//
//  "Checkout" should "expire and goto Cancelled after delivery method selection" in {
//    val checkout = TestFSMRef(new CheckoutFSM(cart, customer, checkoutExpirationTime = 100 millis))
//    checkout ! CheckoutMessages.DeliveryMethodSelected("deliveryMethod")
//    checkout.stateName shouldBe SelectingPaymentMethod
//    eventually {
//      checkout.stateName shouldBe Cancelled
//    }
//  }
//
//  "Checkout" should "go to ProcessingPayment after payment selection" in {
//    val checkout = TestFSMRef(new CheckoutFSM(cart, customer))
//    checkout ! CheckoutMessages.DeliveryMethodSelected("deliveryMethod")
//    checkout ! CheckoutMessages.PaymentSelected("payment")
//    checkout.stateName shouldBe ProcessingPayment
//    val state = checkout.stateData.asInstanceOf[CheckoutParametersWithPaymentService]
//    state.delivery shouldBe "deliveryMethod"
//    state.payment shouldBe "payment"
//  }
//
//  "Checkout" should "be cancellable after payment method selection" in {
//    val checkout = TestFSMRef(new CheckoutFSM(cart, customer))
//    checkout ! CheckoutMessages.DeliveryMethodSelected("deliveryMethod")
//    checkout ! CheckoutMessages.PaymentSelected("payment")
//    checkout ! CheckoutMessages.Cancelled
//    checkout.stateName shouldBe Cancelled
//  }
//
//  "Checkout" should "expire and goto Cancelled after payment method selection" in {
//    val checkout = TestFSMRef(new CheckoutFSM(cart, customer, paymentExpirationTime = 100 millis))
//    checkout ! CheckoutMessages.DeliveryMethodSelected("deliveryMethod")
//    checkout ! CheckoutMessages.PaymentSelected("payment")
//
//    checkout.stateName shouldBe ProcessingPayment
//    eventually {
//      checkout.stateName shouldBe Cancelled
//    }
//  }
//
//  "Checkout" should "be closeable" in {
//    val checkout = TestFSMRef(new CheckoutFSM(cart, customer))
//    checkout ! CheckoutMessages.DeliveryMethodSelected("deliveryMethod")
//    checkout ! CheckoutMessages.PaymentSelected("payment")
//    checkout ! CheckoutMessages.PaymentReceived
//    checkout.stateName shouldBe Closed
//  }
//
//  "Checkout" should "should send CheckoutClosed to cart" in {
//    val cart = TestProbe()
//    val checkout = TestFSMRef(new CheckoutFSM(cart.ref, customer))
//    checkout ! CheckoutMessages.DeliveryMethodSelected("deliveryMethod")
//    checkout ! CheckoutMessages.PaymentSelected("payment")
//    checkout ! CheckoutMessages.PaymentReceived
//    cart.expectMsg(500 millis, CartMessages.CheckoutClosed)
//  }
