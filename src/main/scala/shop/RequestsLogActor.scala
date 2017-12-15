package shop


import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Subscribe


class RequestsLogActor extends Actor with ActorLogging{
  import akka.cluster.pubsub.DistributedPubSubMediator.SubscribeAck
  val mediator: ActorRef = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("requests", self)

  override def receive: Receive = {
    case SubscribeAck(Subscribe("requests", None, `self`)) ⇒
      log.info("Subscribing to all requests")
    case PublishRequest(req) =>
      log.info(prepareLogMessage(req))
  }

  private def prepareLogMessage(req: String) = s"${identifier()} got request: $req"
  private def identifier(): String = sender.path.root.toString
}