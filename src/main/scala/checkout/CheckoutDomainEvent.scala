package checkout

import akka.actor.ActorRef

sealed trait CheckoutDomainEvent

case class AddingDeliveryMethod(method: String) extends CheckoutDomainEvent

case class AddingPayment(method: String, service: ActorRef) extends CheckoutDomainEvent

case object CleaningData extends CheckoutDomainEvent