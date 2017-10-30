import akka.actor.ActorSystem
import akka.testkit.{TestFSMRef, TestKit}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class CustomerTest extends TestKit(ActorSystem("CustomerTest"))
  with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll with Eventually {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Customer" should "start with empty Cart" in {
    val customer = TestFSMRef(new Customer())
    customer ! Customer.AddItem("BANANA")
    customer ! Customer.StartCheckout
    Thread.sleep(500)
    customer ! Customer.DeliveryMethodSelected("delivery")
    customer ! Customer.PaymentSelected("payment")
    Thread.sleep(500)
    customer ! Customer.DoPayment
    Thread.sleep(1000)
    customer.stateName shouldBe Customer.FirstState
  }
}