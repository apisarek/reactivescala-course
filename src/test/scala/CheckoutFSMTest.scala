import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestFSMRef, TestKit, TestProbe}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import Utils.generateID
import scala.concurrent.duration._

class CheckoutFSMTest extends TestKit(ActorSystem("CheckoutFSMTest"))
  with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll
  with Eventually {

  val customer = system.actorOf(Props[Customer])
  val cart = system.actorOf(Props(new CartManagerFSM(customer, generateID())))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Checkout" should "start in SelectingDelivery" in {
    val checkout = TestFSMRef(new CheckoutFSM(cart, customer))
    checkout.stateName shouldBe SelectingDelivery
    checkout.stateData shouldBe CheckoutParameters()
  }

  "Checkout" should "be cancellable after creation" in {
    val checkout = TestFSMRef(new CheckoutFSM(cart, customer))
    checkout ! Checkout.Cancelled
    checkout.stateName shouldBe Cancelled
  }

  "Checkout" should "expire and goto Cancelled " in {
    val checkout = TestFSMRef(new CheckoutFSM(cart, customer, checkoutExpirationTime = 100 millis))
    checkout.stateName shouldBe SelectingDelivery
    eventually {
      checkout.stateName shouldBe Cancelled
    }
  }

  "Checkout" should "go to SelectingPaymentMethod after delivery method selection" in {
    val checkout = TestFSMRef(new CheckoutFSM(cart, customer))
    checkout ! Checkout.DeliveryMethodSelected("deliveryMethod")
    checkout.stateName shouldBe SelectingPaymentMethod
    checkout.stateData shouldBe CheckoutParameters("deliveryMethod")
  }

  "Checkout" should "be cancellable after delivery method selection" in {
    val checkout = TestFSMRef(new CheckoutFSM(cart, customer))
    checkout ! Checkout.DeliveryMethodSelected("deliveryMethod")
    checkout.stateName shouldBe SelectingPaymentMethod

    checkout ! Checkout.Cancelled
    checkout.stateName shouldBe Cancelled
  }

  "Checkout" should "expire and goto Cancelled after delivery method selection" in {
    val checkout = TestFSMRef(new CheckoutFSM(cart, customer, checkoutExpirationTime = 100 millis))
    checkout ! Checkout.DeliveryMethodSelected("deliveryMethod")
    checkout.stateName shouldBe SelectingPaymentMethod
    eventually {
      checkout.stateName shouldBe Cancelled
    }
  }

  "Checkout" should "go to ProcessingPayment after payment selection" in {
    val checkout = TestFSMRef(new CheckoutFSM(cart, customer))
    checkout ! Checkout.DeliveryMethodSelected("deliveryMethod")
    checkout ! Checkout.PaymentSelected("payment")
    checkout.stateName shouldBe ProcessingPayment
    val state = checkout.stateData.asInstanceOf[CheckoutParametersWithPaymentService]
    state.delivery shouldBe "deliveryMethod"
    state.payment shouldBe "payment"
  }

  "Checkout" should "be cancellable after payment method selection" in {
    val checkout = TestFSMRef(new CheckoutFSM(cart, customer))
    checkout ! Checkout.DeliveryMethodSelected("deliveryMethod")
    checkout ! Checkout.PaymentSelected("payment")
    checkout ! Checkout.Cancelled
    checkout.stateName shouldBe Cancelled
  }

  "Checkout" should "expire and goto Cancelled after payment method selection" in {
    val checkout = TestFSMRef(new CheckoutFSM(cart, customer, paymentExpirationTime = 100 millis))
    checkout ! Checkout.DeliveryMethodSelected("deliveryMethod")
    checkout ! Checkout.PaymentSelected("payment")

    checkout.stateName shouldBe ProcessingPayment
    eventually {
      checkout.stateName shouldBe Cancelled
    }
  }

  "Checkout" should "be closeable" in {
    val checkout = TestFSMRef(new CheckoutFSM(cart, customer))
    checkout ! Checkout.DeliveryMethodSelected("deliveryMethod")
    checkout ! Checkout.PaymentSelected("payment")
    checkout ! Checkout.PaymentReceived
    checkout.stateName shouldBe Closed
  }

  "Checkout" should "should send CheckoutClosed to cart" in {
    val cart = TestProbe()
    val checkout = TestFSMRef(new CheckoutFSM(cart.ref, customer))
    checkout ! Checkout.DeliveryMethodSelected("deliveryMethod")
    checkout ! Checkout.PaymentSelected("payment")
    checkout ! Checkout.PaymentReceived
    cart.expectMsg(500 millis, CartMessages.CheckoutClosed)
  }
}

