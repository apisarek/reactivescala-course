import akka.actor.FSM

import scala.concurrent.duration._

class CartFSM(cartExpirationTime: FiniteDuration = 10.seconds)
  extends FSM[CartState, CartData] {
  import Cart._
  startWith(Empty, CartContent(0))

  when(Empty) {
    case Event(ItemAdded, CartContent(content)) =>
      goto(NonEmpty) using CartContent(content + 1)
  }

  when(NonEmpty, stateTimeout = cartExpirationTime) {
    case Event(ItemAdded, CartContent(content)) =>
      stay using CartContent(content + 1)
    case Event(StateTimeout, _) =>
      goto(Empty) using CartContent(0)
    case Event(ItemRemoved, CartContent(1)) =>
      goto(Empty) using CartContent(0)
    case Event(ItemRemoved, CartContent(content)) =>
      stay using CartContent(content - 1)
    case Event(CheckoutStarted, content: CartContent) =>
      goto(InCheckout) using content
  }

  when(InCheckout) {
    case Event(CheckoutCanceled, content: CartContent) =>
      goto(NonEmpty) using content
    case Event(CheckoutClosed, _) =>
      goto(Empty) using CartContent(0)
  }

  initialize()


}

sealed trait CartState

case object Empty extends CartState

case object NonEmpty extends CartState

case object InCheckout extends CartState

sealed trait CartData

final case class CartContent(content: Int) extends CartData