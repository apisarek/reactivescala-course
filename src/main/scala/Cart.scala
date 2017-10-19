import akka.actor.Actor
import akka.actor.Timers
import scala.concurrent.duration._

class Cart(expirationTime: FiniteDuration = 10.seconds) extends Actor with Timers {
  private var items: Int = 0

  override def receive: Receive = empty

  import Cart._

  def empty: Receive = {
    case ItemAdded =>
      items += 1
      context.become(nonEmpty)
      timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)

  }

  def nonEmpty: Receive = {
    case ItemAdded =>
      items += 1
      timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
    case ItemRemoved =>
      items -= 1
      if (items == 0)
        context.become(empty)
      timers.startSingleTimer(CartTimerKey, CartTimerExpired, expirationTime)
    case CartTimerExpired =>
      context.become(empty)
    case CheckoutStarted =>
      context.become(inCheckout)
  }

  def inCheckout: Receive = {
    case CheckoutCanceled =>
      context.become(nonEmpty)
    case CheckoutClosed =>
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
