import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import shop.PaymentService
import shop.PaymentService.DoPayment

class PaymentServiceTest extends TestKit(ActorSystem("PaymentServiceTest"))
  with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll with Eventually {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "PaymentService" should "work" in {
    val checkout = TestProbe()
    val paymentService = system.actorOf(Props(new PaymentService(checkout.ref)))
    paymentService ! DoPayment("visa")
    Thread.sleep(20000)
  }
}