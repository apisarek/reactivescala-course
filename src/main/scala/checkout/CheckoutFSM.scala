package checkout

import akka.actor.{ActorRef, Props}
import akka.persistence.fsm.PersistentFSM
import cart.{CartMessages, PrintStateName}
import shop.{CustomerMessages, PaymentService}

import scala.concurrent.duration._
import scala.reflect.ClassTag


class CheckoutFSM(
  cart: ActorRef,
  customer: ActorRef,
  id: String,
  checkoutExpirationTime: FiniteDuration = 10.seconds,
  paymentExpirationTime: FiniteDuration = 10.seconds
)(
  implicit val domainEventClassTag: ClassTag[CheckoutDomainEvent]
) extends PersistentFSM[CheckoutState, CheckoutData, CheckoutDomainEvent] {
  startWith(SelectingDelivery, CheckoutParameters())

  when(SelectingDelivery, stateTimeout = checkoutExpirationTime) {
    case Event(StateTimeout | CheckoutMessages.Cancelled, _) =>
      goto(Cancelled) applying CleaningData
    case Event(CheckoutMessages.DeliveryMethodSelected(method), _: CheckoutParameters) =>
      goto(SelectingPaymentMethod) applying AddingDeliveryMethod(method)
  }


  when(SelectingPaymentMethod, stateTimeout = checkoutExpirationTime) {
    case Event(StateTimeout | CheckoutMessages.Cancelled, _) =>
      goto(Cancelled) applying CleaningData
    case Event(CheckoutMessages.PaymentSelected(method), CheckoutParameters(_, _)) =>
      val paymentService = context.system.actorOf(Props(new PaymentService(context.self)))
      goto(ProcessingPayment) applying AddingPayment(method, paymentService)
  }

  when(ProcessingPayment, stateTimeout = paymentExpirationTime) {
    case Event(CheckoutMessages.PaymentReceived, _) =>
      print("DOSTALEM")
      goto(Closed)
    case Event(StateTimeout | CheckoutMessages.Cancelled, _) =>
      goto(Cancelled) applying CleaningData

  }

  when(Cancelled) {
    case Event(PrintStateName, _) =>
    println(stateName)
    println(stateData)
    stay
  case Event(CheckoutMessages.GetStateData, _) =>
    sender() ! stateData
    stay
  case Event(CheckoutMessages.GetStateName, _) =>
    sender() ! stateName
    stay
  }

  when(Closed) {
    case Event(PrintStateName, _) =>
      println(stateName)
      println(stateData)
      stay
    case Event(CheckoutMessages.GetStateData, _) =>
      sender() ! stateData
      stay
    case Event(CheckoutMessages.GetStateName, _) =>
      sender() ! stateName
      stay
  }

  whenUnhandled {
    case Event(PrintStateName, _) =>
      println(stateName)
      println(stateData)
      stay
    case Event(CheckoutMessages.GetStateData, _) =>
      sender() ! stateData
      stay
    case Event(CheckoutMessages.GetStateName, _) =>
      sender() ! stateName
      stay
  }

  onTransition {
    case _ -> ProcessingPayment =>
      println(stateData)
      println(nextStateData)
      val paymentService = nextStateData.asInstanceOf[CheckoutParametersWithPaymentService].paymentService
      customer ! CustomerMessages.PaymentServiceStarted(paymentService)
    case _ -> Closed =>
      customer ! CustomerMessages.CheckoutClosed
      cart ! CartMessages.CheckoutClosed
  }

  override def applyEvent(domainEvent: CheckoutDomainEvent, currentData: CheckoutData): CheckoutData = {
    domainEvent match {
      case CleaningData => CheckoutParameters()
      case AddingPayment(method, service) => currentData.addPayment(method).addPaymentService(service)
      case AddingDeliveryMethod(method) => currentData.addDelivery(method)
    }

  }

  override def persistenceId: String = id
}


object CheckoutMessages {

  case class DeliveryMethodSelected(method: String)

  case class PaymentSelected(payment: String)

  case object CheckoutTimerExpired

  case object PaymentTimerKey

  case object Cancelled

  case object CheckoutTimerKey

  case object PaymentReceived

  case object PaymentTimerExpired
  case object PrintStateName
  case object GetStateData
  case object GetStateName

}