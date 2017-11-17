import akka.actor.ActorSystem
import akka.testkit.{TestFSMRef, TestKit}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import shop.{Customer, CustomerMessages}

class CustomerTest extends TestKit(ActorSystem("CustomerTest"))
  with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll with Eventually {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Customer" should "start with empty Cart" in {
    val customer = TestFSMRef(new Customer())
    customer ! CustomerMessages.AddItem("BANANA")
    customer ! CustomerMessages.StartCheckout
    Thread.sleep(500)
    customer ! CustomerMessages.DeliveryMethodSelected("delivery")
    customer ! CustomerMessages.PaymentSelected("payment")
    Thread.sleep(500)
    customer.stateName shouldNot be (FirstState)
    customer ! CustomerMessages.DoPayment
    Thread.sleep(1000)
    customer.stateName shouldBe FirstState
  }
}