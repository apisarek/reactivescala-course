import akka.actor.{Actor, ActorRef, FSM, Props}
import akka.event.LoggingReceive
import Utils.generateID

sealed trait CustomerData

sealed trait CustomerState

class Customer extends FSM[CustomerState, CustomerData] {
  startWith(Customer.FirstState, Customer.CustomerCart(
    context.system.actorOf(Props(new CartManagerFSM(context.self, generateID())))
  ))

  when(Customer.FirstState) {
    case Event(Customer.AddItem(_), Customer.CustomerCart(cart)) =>
      cart ! CartMessages.ItemAdded
      goto(Customer.SecondState)
  }

  when(Customer.SecondState) {
    case Event(Customer.AddItem(_), Customer.CustomerCart(cart)) =>
      cart ! CartMessages.ItemAdded
      goto(Customer.SecondState)

    case Event(Customer.StartCheckout, Customer.CustomerCart(cart)) =>
      cart ! CartMessages.CheckoutStarted
      goto(Customer.ThirdState)
  }

  when(Customer.ThirdState) {
    case Event(Customer.CheckoutStarted(checkout), Customer.CustomerCart(cart)) =>
      goto(Customer.FourthState) using Customer.CustomerCartAndCheckout(cart, checkout)

  }

  when(Customer.FourthState) {
    case Event(Customer.DeliveryMethodSelected(delivery), Customer.CustomerCartAndCheckout(_, checkout)) =>
      checkout ! Checkout.DeliveryMethodSelected(delivery)
      goto(Customer.FifthState)
  }

  when(Customer.FifthState) {
    case Event(Customer.PaymentSelected(paymentMethod), Customer.CustomerCartAndCheckout(_, checkout)) =>
      checkout ! Checkout.PaymentSelected(paymentMethod)
      goto(Customer.SixthState)
  }

  when(Customer.SixthState) {
    case Event(Customer.PaymentServiceStarted(paymentService), Customer.CustomerCartAndCheckout(cart, checkout)) =>
      goto(Customer.SeventhState) using Customer.CustomerCartCheckoutPayment(cart, checkout, paymentService)
  }


  when(Customer.SeventhState) {
    case Event(Customer.DoPayment, Customer.CustomerCartCheckoutPayment(_, _, paymentService)) =>
      paymentService ! PaymentService.DoPayment
      goto(Customer.EightState)
  }

  when(Customer.EightState) {
    case Event(Customer.PaymentConfirmed, _) =>
      goto(Customer.NinthState)
  }


  when(Customer.NinthState) {
    case Event(Customer.CheckoutClosed, _) =>
      goto(Customer.TenthState)
  }

  when(Customer.TenthState) {
    case Event(Customer.CartEmpty, Customer.CustomerCartCheckoutPayment(cart, _, _)) =>
      goto(Customer.FirstState) using Customer.CustomerCart(cart)
  }


  initialize()
}


object Customer {

  final case class CustomerCart(cart: ActorRef) extends CustomerData

  case class AddItem(item: String)

  case class CheckoutStarted(checkout: ActorRef)

  case class DeliveryMethodSelected(deliveryMethod: String)

  case class PaymentSelected(paymentMethod: String)

  case class PaymentServiceStarted(paymentService: ActorRef)

  case class CustomerCartAndCheckout(cart: ActorRef, checkout: ActorRef) extends CustomerData

  case class CustomerCartCheckoutPayment(
    cart: ActorRef,
    checkout: ActorRef,
    paymentService: ActorRef
  ) extends CustomerData

  case object StartCheckout

  case object DoPayment

  case object PaymentConfirmed

  case object FirstState extends CustomerState

  case object SecondState extends CustomerState

  case object ThirdState extends CustomerState

  case object FourthState extends CustomerState

  case object FifthState extends CustomerState

  case object SixthState extends CustomerState

  case object SeventhState extends CustomerState

  case object EightState extends CustomerState

  object CartEmpty

  case object NinthState extends CustomerState

  case object CheckoutClosed

  case object TenthState extends CustomerState

}


