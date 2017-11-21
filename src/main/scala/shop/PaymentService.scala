package shop

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import checkout.CheckoutMessages
import shop.PaymentService.DoPayment

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PaymentService(checkout: ActorRef) extends Actor with ActorLogging {

  override val supervisorStrategy: OneForOneStrategy = OneForOneStrategy(loggingEnabled = false) {
    case _: RepeatPaymentException =>
      println("HERE")
      log.warning("REPEAT PAYMENT")
      Restart
    //    case e: ArithmeticException =>
    //      log.error("Evaluation failed because of: {}", e.getMessage)
    //      notifyConsumerFailure(worker = sender(), failure = e)
    //      Stop
    case e =>
      log.error("Unexpected failure: {}", e.getMessage)
      Stop
  }
  var worker: ActorRef = _

  override def receive: Receive = {
    case DoPayment(payment: String) =>
//      worker = context.actorOf(Props(new PaymentWorker(payment)))
      sender() ! CustomerMessages.PaymentConfirmed
      checkout ! CheckoutMessages.PaymentReceived
  }
}

object PaymentService {

  case class DoPayment(payment: String)

}

class PaymentWorker(payment: String) extends Actor {

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  import akka.pattern.pipe
  import context.dispatcher

  val http = Http(context.system)

  override def preStart() = {
    val uri = s"http://127.0.0.1:5000/$payment"
    println(uri)
    http.singleRequest(HttpRequest(uri = uri))
      .pipeTo(self)
  }

  def receive = {
    case resp@HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        println("Got response, body: " + body.utf8String)
        resp.discardEntityBytes()
        shutdown()
      }
    case resp@HttpResponse(code, _, _, _) =>
      println("Request failed, response code: " + code)
      resp.discardEntityBytes()
      throw new RepeatPaymentException
      shutdown()

  }

  def shutdown() = {
    Await.result(http.shutdownAllConnectionPools(), Duration.Inf)
    context.system.terminate()
  }
}

class RepeatPaymentException extends Exception("Repeat")


