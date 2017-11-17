import java.util.UUID

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.persistence.fsm.PersistentFSM
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._
import scala.reflect.{ClassTag, _}

class DummyTest extends TestKit(ActorSystem("DummyTest"))
  with FlatSpecLike
  with Matchers
  with BeforeAndAfterAll
  with Eventually
  with ImplicitSender
{
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }


  "A" should "B" in {
    val dummy = system.actorOf(Props(new DummyFSM("1")))
    dummy ! "DODAJ"
    Thread.sleep(200)
    dummy ! PoisonPill
    Thread.sleep(200)
    val dummy1 = system.actorOf(Props(new DummyFSM("1")))
    Thread.sleep(2000)

  }
}


class DummyFSM(id: String)(implicit val domainEventClassTag: ClassTag[DummyEvent])
extends PersistentFSM[DummyState, DummyData, DummyEvent] {
  startWith(FirstState, DummyData(0))

  override def applyEvent(domainEvent: DummyEvent, currentData: DummyData): DummyData = {
    println("DODAJE")
    currentData.copy(count = currentData.count + 1)
  }

  override def persistenceId: String = id

  when(FirstState) {
    case Event("DODAJ", DummyData(count)) => goto(SecondState) applying DummyEvent()
  }


  when(SecondState, stateTimeout = 1.seconds) {
    case Event(StateTimeout, _) =>
      println("WRACAM")
      goto(FirstState)
  }
}

case class DummyEvent()

case class DummyData(count: Int = 0)

sealed trait DummyState extends PersistentFSM.FSMState

case object FirstState extends DummyState {
  override def identifier: String = "FirstState"
}

case object SecondState  extends DummyState {
  override def identifier: String = "SecondState"
}
