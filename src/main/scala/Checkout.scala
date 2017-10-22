object Checkout {
  case class DeliveryMethodSelected(method: String)

  case object Cancelled

  case object CheckoutTimerKey

  case class PaymentSelected(payment: String)

  case object PaymentReceived
}
