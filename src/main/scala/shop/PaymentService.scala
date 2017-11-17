package shop

import akka.actor.{Actor, ActorRef}
import checkout.CheckoutMessages
import shop.PaymentService.DoPayment

class PaymentService(checkout: ActorRef) extends Actor {
  override def receive: Receive = {
    case DoPayment =>
      sender() ! CustomerMessages.PaymentConfirmed
      checkout ! CheckoutMessages.PaymentReceived
  }
}

object PaymentService {
  case object DoPayment
}
