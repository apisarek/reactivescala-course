import akka.actor.Timers
import akka.event.LoggingReceive

import scala.concurrent.duration.{FiniteDuration, _}

class Checkout(
                checkoutTime: FiniteDuration = 10.seconds,
                paymentTime: FiniteDuration = 5.seconds)
  extends Timers {

  import Checkout._

  private var _deliveryMethod: String = ""
  private var _paymentMethod: String = ""

  def deliveryMethod: String = _deliveryMethod

  def paymentMethod: String = _paymentMethod

  override def receive: Receive = selectingDelivery


  def selectingDelivery: Receive = LoggingReceive {
    case DeliveryMethodSelected(method) =>
      _deliveryMethod = method
      timers.startSingleTimer(CheckoutTimerKey, CheckoutTimerExpired, checkoutTime)
      context.become(selectingPaymentMethod)
    case CheckoutTimerExpired | Checkout.Cancelled =>
      _deliveryMethod = ""
      _paymentMethod = ""
      context.become(cancelled)
  }


  def processingPayment: Receive = LoggingReceive {
    case PaymentTimerExpired | Checkout.Cancelled =>
      _deliveryMethod = ""
      _paymentMethod = ""
      context.become(cancelled)
    case PaymentReceived =>
      context.become(closed)
  }

  def selectingPaymentMethod: Receive = LoggingReceive {
    case CheckoutTimerExpired | Checkout.Cancelled =>
      _deliveryMethod = ""
      _paymentMethod = ""
      context.become(cancelled)
    case PaymentSelected(method) =>
      _paymentMethod = method
      timers.startSingleTimer(PaymentTimerKey, PaymentTimerExpired, paymentTime)
      context.become(processingPayment)
  }

  def cancelled: Receive = LoggingReceive {
    case _ =>
  }

  def closed: Receive = cancelled // behavior is the same here

}

object Checkout {

  case class DeliveryMethodSelected(method: String)

  case class PaymentSelected(payment: String)

  case object CheckoutTimerExpired

  case object PaymentTimerKey

  case object Cancelled

  case object CheckoutTimerKey

  case object PaymentReceived

  case object PaymentTimerExpired

}

