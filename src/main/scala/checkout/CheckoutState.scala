package checkout

import akka.persistence.fsm.PersistentFSM

sealed trait CheckoutState extends PersistentFSM.FSMState {
  override def identifier: String = getClass.getName
}

case object Cancelled extends CheckoutState

case object SelectingDelivery extends CheckoutState

case object SelectingPaymentMethod extends CheckoutState

case object ProcessingPayment extends CheckoutState

case object Closed extends CheckoutState
