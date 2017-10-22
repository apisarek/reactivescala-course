import Checkout.{DeliveryMethodSelected, PaymentReceived, PaymentSelected}
import akka.actor.FSM

import scala.concurrent.duration._

class CheckoutFSM(checkoutExpirationTime: FiniteDuration = 10.seconds, paymentExpirationTime: FiniteDuration = 10.seconds)
  extends FSM[CheckoutState, CheckoutData] {


  startWith(SelectingDelivery, CheckoutParameters())

  when(SelectingDelivery, stateTimeout = checkoutExpirationTime) {
    case Event(StateTimeout | Checkout.Cancelled, _) =>
      goto(Cancelled) using CheckoutParameters()
    case Event(DeliveryMethodSelected(method), checkoutData: CheckoutParameters) =>
      goto(SelectingPaymentMethod) using checkoutData.copy(delivery = method)
  }

  when(Cancelled) {
    case _ => stay
  }

  when(SelectingPaymentMethod, stateTimeout = checkoutExpirationTime) {
    case Event(StateTimeout | Checkout.Cancelled, _) =>
      goto(Cancelled) using CheckoutParameters()
    case Event(PaymentSelected(method), checkoutData: CheckoutParameters) =>
      goto(ProcessingPayment) using checkoutData.copy(payment = method)
  }

  when(ProcessingPayment, stateTimeout = paymentExpirationTime) {
    case Event(StateTimeout | Checkout.Cancelled, _) =>
      goto(Cancelled) using CheckoutParameters()
    case Event(PaymentReceived, _) =>
      goto(Closed)

  }

  initialize()
}

sealed trait CheckoutData

final case class CheckoutParameters(delivery: String = "", payment: String = "") extends CheckoutData

sealed trait CheckoutState

case object Cancelled extends CheckoutState

case object SelectingDelivery extends CheckoutState

case object SelectingPaymentMethod extends CheckoutState

case object ProcessingPayment extends CheckoutState

case object Closed extends CheckoutState