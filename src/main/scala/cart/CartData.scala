package cart

import akka.actor.ActorRef

sealed trait CartData {
  def shoppingCart: ShoppingCart

  def updateShoppingCart(newShoppingCart: ShoppingCart): CartData

  def addCheckout(checkout: ActorRef): CartManagerContentWithCheckout

  def removeCheckout(): CartManagerContent
}

final case class CartManagerContent(shoppingCart: ShoppingCart = ShoppingCart()) extends CartData {
  def addCheckout(checkout: ActorRef): CartManagerContentWithCheckout = {
    cart.CartManagerContentWithCheckout(shoppingCart, checkout)
  }

  override def updateShoppingCart(newShoppingCart: ShoppingCart): CartData = {
    copy(shoppingCart = newShoppingCart)
  }

  override def removeCheckout(): CartManagerContent = this
}

final case class CartManagerContentWithCheckout(shoppingCart: ShoppingCart, checkout: ActorRef) extends CartData {
  def removeCheckout(): CartManagerContent = {
    cart.CartManagerContent(shoppingCart)
  }

  override def updateShoppingCart(newShoppingCart: ShoppingCart): CartData =
    copy(shoppingCart = newShoppingCart)

  override def addCheckout(checkout: ActorRef): CartManagerContentWithCheckout = this
}