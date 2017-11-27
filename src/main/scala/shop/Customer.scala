package shop

import java.net.URI

import akka.actor.{ActorRef, FSM, Props}
import akka.pattern.ask
import akka.util
import akka.util.Timeout
import cart.{CartManagerFSM, CartMessages, Item}
import checkout.CheckoutMessages
import shop.Utils.generateID

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scalaz.Success

class Customer extends FSM[CustomerState, CustomerData] {

  startWith(FirstState, CustomerCart(
    context.system.actorOf(Props(new CartManagerFSM(context.self, generateID())))
  ))
  var paymentMeth: String = _
  when(FirstState) {
    case Event(CustomerMessages.AddItem(item), CustomerCart(cart)) =>
      // TODO
      cart ! CartMessages.ItemAdded(item)
      goto(SecondState)
  }

  when(SecondState) {
    case Event(CustomerMessages.AddItem(item), CustomerCart(cart)) =>
      // TODO
      cart ! CartMessages.ItemAdded(item)
      goto(SecondState)

    case Event(CustomerMessages.StartCheckout, CustomerCart(cart)) =>
      cart ! CartMessages.CheckoutStarted
      goto(ThirdState)
  }

  when(ThirdState) {
    case Event(CustomerMessages.CheckoutStarted(checkout), CustomerCart(cart)) =>
      goto(FourthState) using CustomerCartAndCheckout(cart, checkout)

  }

  when(FourthState) {
    case Event(CustomerMessages.DeliveryMethodSelected(delivery), CustomerCartAndCheckout(_, checkout)) =>
      checkout ! CheckoutMessages.DeliveryMethodSelected(delivery)
      goto(FifthState)
  }

  when(FifthState) {
    case Event(CustomerMessages.PaymentSelected(paymentMethod), CustomerCartAndCheckout(_, checkout)) =>
      checkout ! CheckoutMessages.PaymentSelected(paymentMethod)
      paymentMeth = paymentMethod
      goto(SixthState)
  }

  when(SixthState) {
    case Event(CustomerMessages.PaymentServiceStarted(paymentService), CustomerCartAndCheckout(cart, checkout)) =>
      goto(SeventhState) using CustomerCartCheckoutPayment(cart, checkout, paymentService)
  }


  when(SeventhState) {
    case Event(CustomerMessages.DoPayment, CustomerCartCheckoutPayment(_, _, paymentService)) =>
      import scala.util.{Success, Failure}
      import context.dispatcher
      implicit val timeout: util.Timeout = Timeout(100.millis)

      val future = paymentService ? PaymentService.DoPayment(paymentMeth)
      future onComplete {

        case Success(_) => ()
        case Failure(_) => throw new Exception

      }
//      paymentService ! PaymentService.DoPayment("paypal")
//      goto(EightState)
      stay()
    case Event(CustomerMessages.PaymentConfirmed, _) =>
      goto(NinthState)
  }

  when(EightState) {
    case Event(CustomerMessages.PaymentConfirmed, _) =>
      goto(NinthState)
  }


  when(NinthState) {
    case Event(CustomerMessages.CheckoutClosed, _) =>
      goto(TenthState)
  }

  when(TenthState) {
    case Event(CustomerMessages.CartEmpty, CustomerCartCheckoutPayment(cart, _, _)) =>
      goto(FirstState) using CustomerCart(cart)
  }


  initialize()
}

object CustomerMessages {


  case class AddItem(item: Item)

  case class CheckoutStarted(checkout: ActorRef)

  case class DeliveryMethodSelected(deliveryMethod: String)

  case class PaymentSelected(paymentMethod: String)

  case class PaymentServiceStarted(paymentService: ActorRef)


  case object StartCheckout

  case object DoPayment

  case object PaymentConfirmed


  case object CartEmpty

  case object CheckoutClosed

}