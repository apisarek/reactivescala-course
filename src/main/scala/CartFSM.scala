import akka.actor.{ActorRef, FSM, Props}

import scala.concurrent.duration._

class CartFSM(customer: ActorRef, cartExpirationTime: FiniteDuration = 10.seconds)
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
    case Event(CheckoutStarted, CartContent(content)) =>
      val checkout = context.system.actorOf(Props(new CheckoutFSM(context.self, customer)))
      goto(InCheckout) using CartContentWithCheckout(content, checkout)
  }

  onTransition {
    case _ -> InCheckout =>
      val checkout = nextStateData.asInstanceOf[CartContentWithCheckout].checkout
      sender() ! Customer.CheckoutStarted(checkout)
    case InCheckout -> Empty =>
      customer ! Customer.CartEmpty
  }

  when(InCheckout) {
    case Event(CheckoutCanceled, CartContentWithCheckout(content, _)) =>
      goto(NonEmpty) using CartContent(content)
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
final case class CartContentWithCheckout(content: Int, checkout: ActorRef) extends CartData