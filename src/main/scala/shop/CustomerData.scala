package shop

import akka.actor.ActorRef

sealed trait CustomerData

case class CustomerCartAndCheckout(cart: ActorRef, checkout: ActorRef) extends CustomerData

case class CustomerCartCheckoutPayment(
  cart: ActorRef,
  checkout: ActorRef,
  paymentService: ActorRef
) extends CustomerData


final case class CustomerCart(cart: ActorRef) extends CustomerData
