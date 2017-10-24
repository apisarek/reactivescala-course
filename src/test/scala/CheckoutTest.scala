import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._

class CheckoutTest extends TestKit(ActorSystem("CheckoutTest"))
  with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll with Eventually {

  import Checkout._

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }


  "Checkout" should "start with uninitialized methods" in {
    val checkoutRef = TestActorRef(new Checkout())
    val checkout = checkoutRef.underlyingActor
    checkout.deliveryMethod shouldBe ""
    checkout.paymentMethod shouldBe ""
  }

  "Checkout" should "have choosable deliveryMethod" in {
    val checkoutRef = TestActorRef(new Checkout())
    val checkout = checkoutRef.underlyingActor
    checkoutRef ! DeliveryMethodSelected("deliveryMethod")
    checkout.deliveryMethod shouldBe "deliveryMethod"
  }

  "Checkout" should "be cancellable" in {
    val checkoutRef = TestActorRef(new Checkout())
    val checkout = checkoutRef.underlyingActor
    checkoutRef ! DeliveryMethodSelected("deliveryMethod")
    checkoutRef ! Checkout.Cancelled
    checkout.deliveryMethod shouldBe ""
  }

  "Checkout" should "be expirable" in {
    val checkoutRef = TestActorRef(new Checkout(checkoutTime = 100.millis))
    val checkout = checkoutRef.underlyingActor
    checkoutRef ! DeliveryMethodSelected("deliveryMethod")
    eventually {
      checkout.deliveryMethod shouldBe ""
    }
  }

  "Checkout" should "have choosable deliveryMethod and paymentMethod" in {
    val checkoutRef = TestActorRef(new Checkout())
    val checkout = checkoutRef.underlyingActor
    checkoutRef ! DeliveryMethodSelected("deliveryMethod")
    checkoutRef ! PaymentSelected("paymentMethod")
    checkout.deliveryMethod shouldBe "deliveryMethod"
    checkout.paymentMethod shouldBe "paymentMethod"
  }

  "Checkout" should "be cancellable after choosing deliveryMethod and paymentMethod" in {
    val checkoutRef = TestActorRef(new Checkout())
    val checkout = checkoutRef.underlyingActor
    checkoutRef ! DeliveryMethodSelected("deliveryMethod")
    checkoutRef ! PaymentSelected("paymentMethod")
    checkoutRef ! Checkout.Cancelled
    checkout.deliveryMethod shouldBe ""
    checkout.paymentMethod shouldBe ""
  }

  "Checkout" should "be expirable after choosing deliveryMethod and paymentMethod" in {
    val checkoutRef = TestActorRef(new Checkout(paymentTime = 100.millis))
    val checkout = checkoutRef.underlyingActor
    checkoutRef ! DeliveryMethodSelected("deliveryMethod")
    checkoutRef ! PaymentSelected("paymentMethod")
    eventually {
      checkout.deliveryMethod shouldBe ""
      checkout.paymentMethod shouldBe ""

    }
  }

  "Checkout" should "not be expirable when finished" in {
    val checkoutRef = TestActorRef(new Checkout(checkoutTime = 100.millis, paymentTime = 100.millis))
    val checkout = checkoutRef.underlyingActor
    checkoutRef ! DeliveryMethodSelected("deliveryMethod")
    checkoutRef ! PaymentSelected("paymentMethod")
    within(200 millis) {
      awaitAssert(() => {
        if (checkout.deliveryMethod != "deliveryMethod") throw new UnknownError()
        if (checkout.paymentMethod != "paymentMethod") throw new UnknownError()
      }, interval = 20.millis)
    }
  }

}

