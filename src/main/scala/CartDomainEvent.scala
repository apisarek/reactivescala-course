import java.net.URI

import akka.actor.ActorRef

sealed trait CartDomainEvent

case class AddingItem(item: Item) extends CartDomainEvent

case object FreeingCart extends CartDomainEvent

case class RemovingItem(id: URI) extends CartDomainEvent

case class AddingCheckout(checkout: ActorRef) extends CartDomainEvent

case object RemovingCheckout extends CartDomainEvent

case object PrintStateName extends CartDomainEvent