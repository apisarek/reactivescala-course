//import akka.actor.{ActorRef, FSM, Props}
//import akka.persistence.fsm._
//
//import scala.concurrent.duration._
//import scala.reflect.{ClassTag, classTag}
//
//class CartManagerFSM(customer: ActorRef, cartExpirationTime: FiniteDuration = 10.seconds)
//  extends PersistentFSM [CartState, CartData, CartEvent] {
//  import Cart._
//  startWith(Empty, CartManagerContent())
//  when(Empty) {
//    case Event(ItemAdded(itemName), CartManagerContent(cart)) =>
//      //      goto(NonEmpty) using CartManagerContent(cart.addItem(itemName))
//      goto(NonEmpty) applying AddItemToCart(itemName)
//  }
//
//  when(NonEmpty, stateTimeout = cartExpirationTime) {
//    case Event(ItemAdded(itemName), CartManagerContent(cart)) =>
//      stay //using CartManagerContent(cart.addItem(itemName))
//    case Event(StateTimeout, _) =>
//      goto(Empty) //using CartManagerContent()
//    case Event(ItemRemoved(itemName), CartManagerContent(cart)) if cart.removeItem(itemName).count == 0 =>
//      goto(Empty)// using CartManagerContent(cart.removeItem(itemName))
//    case Event(ItemRemoved(itemName), CartManagerContent(cart)) =>
//      stay// using CartManagerContent(cart.removeItem(itemName))
//    case Event(CheckoutStarted, content: CartManagerContent) =>
//      val checkout = context.system.actorOf(Props(new CheckoutFSM(context.self, customer)))
//      goto(InCheckout)// using content.addCheckout(checkout)
//  }
//
//  onTransition {
//    case _ -> InCheckout =>
//      val checkout = nextStateData.asInstanceOf[CartManagerContentWithCheckout].checkout
//      sender() ! Customer.CheckoutStarted(checkout)
//    case InCheckout -> Empty =>
//      customer ! Customer.CartEmpty
//  }
//
//  when(InCheckout) {
//    case Event(CheckoutCanceled, content: CartManagerContentWithCheckout) =>
//      goto(NonEmpty)// using content.removeCheckout()
//    case Event(CheckoutClosed, _) =>
//      goto(Empty)// using CartManagerContent()
//  }
//
//  override def persistenceId = "persistent-cartmanager-fsm-id-1"
//  override def domainEventClassTag: ClassTag[CartEvent] = classTag[CartEvent]
//
//  override def applyEvent(event: CartEvent, dataBeforeEvent: CartData): CartData = {
//    event match {
//      case AddItemToCart(itemName) =>
//        val dataBefore = dataBeforeEvent.asInstanceOf[CartManagerContent]
//        CartManagerContent(dataBefore.shoppingCart.addItem(itemName))
//    }
//  }
//}
//
//sealed trait CartState
//
//case object Empty extends CartState
//
//case object NonEmpty extends CartState
//
//case object InCheckout extends CartState
//
//sealed trait CartData
//
//final case class CartManagerContent(shoppingCart: ShoppingCart = ShoppingCart()) extends CartData {
//  def addCheckout(checkout: ActorRef): CartManagerContentWithCheckout = {
//    CartManagerContentWithCheckout(shoppingCart, checkout)
//  }
//}
//final case class CartManagerContentWithCheckout(shoppingCart: ShoppingCart, checkout: ActorRef) extends CartData {
//  def removeCheckout(): CartManagerContent = {
//    CartManagerContent(shoppingCart)
//  }
//}
//
//sealed trait CartEvent
//
//case class AddItemToCart(itemName: String) extends CartEvent