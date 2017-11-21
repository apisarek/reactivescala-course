package checkout

import akka.actor.ActorRef

sealed trait CheckoutData {
  def addDelivery(method: String): CheckoutData

  def addPayment(method: String): CheckoutData

  def addPaymentService(service: ActorRef): CheckoutData
}

final case class CheckoutParameters(
  delivery: String = "",
  payment: String = ""
) extends CheckoutData {
  override def addDelivery(method: String): CheckoutData = copy(delivery = method)

  override def addPayment(method: String): CheckoutData = copy(payment = method)

  override def addPaymentService(service: ActorRef): CheckoutData = {
    CheckoutParametersWithPaymentService(this.delivery, this.payment, service)
  }
}



final case class CheckoutParametersWithPaymentService(
  delivery: String = "",
  payment: String = "",
  paymentService: ActorRef
) extends CheckoutData {
  override def addDelivery(method: String): CheckoutData = copy(delivery = method)

  override def addPayment(method: String): CheckoutData = copy(payment = method)

  override def addPaymentService(service: ActorRef): CheckoutData = copy(paymentService = service)
}