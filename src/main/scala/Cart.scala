import akka.actor.Timers
import akka.event.LoggingReceive

import scala.concurrent.duration._

class Cart(expirationTime: FiniteDuration = 10.seconds) extends Timers {
  private var _items: Int = 0

  def items: Int = _items

  override def receive: Receive = empty

  import Cart._

  def empty: Receive = LoggingReceive {
    case ItemAdded =>
      _items += 1
      context.become(nonEmpty)
      timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
  }

  def nonEmpty: Receive = LoggingReceive {
    case ItemAdded =>
      _items += 1
      timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
    case ItemRemoved =>
      _items -= 1
      if (_items == 0)
        timers.cancel(CartTimerKey)
      context.become(empty)
      timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
    case CartTimerExpired =>
      _items = 0
      timers.cancel(CartTimerKey)
      context.become(empty)
    case CheckoutStarted =>
      timers.cancel(CartTimerKey)
      context.become(inCheckout)
  }

  def inCheckout: Receive = LoggingReceive {
    case CheckoutCanceled =>
      context.become(nonEmpty)
    case CheckoutClosed =>
      _items = 0
      context.become(empty)
  }
}

object Cart {

  case object CartTimerKey

  case object ItemAdded

  case object ItemRemoved

  case object CartTimerExpired

  case object CheckoutStarted

  case object CheckoutCanceled

  case object CheckoutClosed

}
