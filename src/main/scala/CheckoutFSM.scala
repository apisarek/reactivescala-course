import Checkout.{DeliveryMethodSelected, PaymentReceived, PaymentSelected}
import akka.actor.{ActorRef, FSM, Props}

import scala.concurrent.duration._

sealed trait CheckoutData

sealed trait CheckoutState

class CheckoutFSM(
  cart: ActorRef,
  checkoutExpirationTime: FiniteDuration = 10.seconds,
  paymentExpirationTime: FiniteDuration = 10.seconds
) extends FSM[CheckoutState, CheckoutData] {

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

  when(Closed) {
    case _ => stay
  }

  when(SelectingPaymentMethod, stateTimeout = checkoutExpirationTime) {
    case Event(StateTimeout | Checkout.Cancelled, _) =>
      goto(Cancelled) using CheckoutParameters()
    case Event(PaymentSelected(method), CheckoutParameters(delivery, _)) =>
      val paymentService = context.system.actorOf(Props(new PaymentService(context.self)))
      goto(ProcessingPayment) using CheckoutParametersWithPaymentService(delivery, method, paymentService)
  }

  when(ProcessingPayment, stateTimeout = paymentExpirationTime) {
    case Event(PaymentReceived, _) =>

      goto(Closed)
    case Event(StateTimeout | Checkout.Cancelled, _) =>
      goto(Cancelled) using CheckoutParameters()

  }


  onTransition {
    case _ -> ProcessingPayment =>
      val paymentService = nextStateData.asInstanceOf[CheckoutParametersWithPaymentService].paymentService
      sender() ! Customer.PaymentServiceStarted(paymentService)
    case _ -> Closed =>
      cart ! Cart.CheckoutClosed
  }

  initialize()
}

final case class CheckoutParameters(
  delivery: String = "",
  payment: String = ""
) extends CheckoutData
final case class CheckoutParametersWithPaymentService(
  delivery: String = "",
  payment: String = "",
  paymentService: ActorRef
) extends CheckoutData

case object Cancelled extends CheckoutState

case object SelectingDelivery extends CheckoutState

case object SelectingPaymentMethod extends CheckoutState

case object ProcessingPayment extends CheckoutState

case object Closed extends CheckoutState