import java.net.URI

import akka.actor.ActorSystem
import akka.testkit.{TestFSMRef, TestKit}
import cart.Item
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import shop.{Customer, CustomerMessages, FirstState}

class CustomerTest extends TestKit(ActorSystem("PaymentServiceTest"))
  with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll with Eventually {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Customer" should "start with empty Cart" in {
    val customer = TestFSMRef(new Customer())

    customer ! CustomerMessages.AddItem(Item(URI.create("BANAN"), "BANAN", 0))
    customer ! CustomerMessages.StartCheckout
    Thread.sleep(1000)
    customer ! CustomerMessages.DeliveryMethodSelected("delivery")
    Thread.sleep(1000)
    customer ! CustomerMessages.PaymentSelected("paypal")
    Thread.sleep(1000)
    customer.stateName shouldNot be (FirstState)
    customer ! CustomerMessages.DoPayment
    Thread.sleep(1000)
    customer.stateName shouldBe FirstState
  }
}