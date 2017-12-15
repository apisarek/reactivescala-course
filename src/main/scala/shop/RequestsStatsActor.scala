package shop


import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe
import shop.PublishRequest

case object Stats

class RequestsStatsActor extends Actor with ActorLogging {

  import akka.cluster.pubsub.DistributedPubSubMediator.SubscribeAck

  val mediator: ActorRef = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("requests", self)

  override def receive: Receive = {
    case SubscribeAck(Subscribe("requests", None, `self`)) ⇒
      log.info("Subscribing to all requests")
      context become listening(Map.empty[String, Int])
  }

  def listening(map: Map[String, Int]): Receive = {
    case PublishRequest(_) =>
      val value = map.getOrElse(identifier, 0) + 1
      context become listening(map + (identifier -> value))
    case Stats =>
      sender ! map
  }

  private def identifier: String = sender.path.root.toString

}

