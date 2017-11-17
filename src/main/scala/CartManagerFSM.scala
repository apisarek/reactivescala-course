import java.net.URI

import akka.actor.{ActorRef, Props}
import akka.persistence.fsm.PersistentFSM

import scala.concurrent.duration._
import scala.reflect._


class CartManagerFSM(
  customer: ActorRef,
  id: String,
  cartExpirationTime: FiniteDuration = 1.seconds
)(
  implicit val domainEventClassTag: ClassTag[CartDomainEvent]
)
  extends PersistentFSM[CartState, CartData, CartDomainEvent] {

  import CartMessages._

  override def persistenceId: String = id

  when(Empty) {
    case Event(ItemAdded(item), _) =>
      goto(NonEmpty) applying AddingItem(item)
  }

  when(NonEmpty, stateTimeout = cartExpirationTime) {
    case Event(StateTimeout, _) =>
      goto(Empty) applying FreeingCart
    case Event(ItemAdded(item), _) =>
      stay applying AddingItem(item)
    case Event(ItemRemoved(itemName), CartManagerContent(cart)) if cart.removeItem(itemName).count == 0 =>
      goto(Empty) applying FreeingCart
    case Event(ItemRemoved(item), CartManagerContent(_)) =>
      stay applying RemovingItem(item)
    case Event(CheckoutStarted, _: CartManagerContent) =>
      val checkout = context.system.actorOf(Props(new CheckoutFSM(context.self, customer)))
      goto(InCheckout) applying AddingCheckout(checkout)
  }

  onTransition {
    case _ -> InCheckout =>
      val checkout = nextStateData.asInstanceOf[CartManagerContentWithCheckout].checkout
      customer ! Customer.CheckoutStarted(checkout)
    case InCheckout -> Empty =>
      customer ! Customer.CartEmpty
  }

  when(InCheckout) {
    case Event(CheckoutCanceled, _: CartManagerContentWithCheckout) =>
      goto(NonEmpty) applying RemovingCheckout
    case Event(CheckoutClosed, _) =>
      goto(Empty) applying FreeingCart
  }

  whenUnhandled {
    case Event(GetShoppingCart, _) =>
      sender() ! stateData.shoppingCart
      stay
    case Event(PrintStateName, _) =>
      println(stateName)
      println(stateData)
      stay
    case Event(GetStateData, _) =>
      sender() ! stateData
      stay
    case Event(GetStateName, _) =>
      sender() ! stateName.toString
      stay
  }


  override def applyEvent(domainEvent: CartDomainEvent, currentData: CartData): CartData = {
    domainEvent match {
      case AddingItem(item) => currentData.updateShoppingCart(currentData.shoppingCart.addItem(item))
      case RemovingItem(item) => currentData.updateShoppingCart(currentData.shoppingCart.removeItem(item))
      case AddingCheckout(checkout) => currentData.addCheckout(checkout)
      case RemovingCheckout => currentData.removeCheckout()
      case FreeingCart => currentData.updateShoppingCart(ShoppingCart())
    }
  }

  startWith(Empty, CartManagerContent())
}


object CartMessages {

  case class ItemAdded(item: Item)

  case class ItemRemoved(id: URI)

  case object GetStateData

  case object GetStateName

  case object CartTimerKey

  case object CartTimerExpired

  case object CheckoutStarted

  case object CheckoutCanceled

  case object CheckoutClosed

  case object GetShoppingCart

}






