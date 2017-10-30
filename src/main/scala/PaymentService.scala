import PaymentService.DoPayment
import akka.actor.{Actor, ActorRef}

class PaymentService(checkout: ActorRef) extends Actor {
  override def receive: Receive = {
    case DoPayment =>
      sender() ! Customer.PaymentConfirmed
      checkout ! Checkout.PaymentReceived
  }
}

object PaymentService {
  case object DoPayment
}
