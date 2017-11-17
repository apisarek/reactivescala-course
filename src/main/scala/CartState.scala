import akka.persistence.fsm.PersistentFSM

case object Empty extends CartState

case object NonEmpty extends CartState

case object InCheckout extends CartState

sealed trait CartState extends PersistentFSM.FSMState {
  override def identifier: String = this.getClass.getName
}