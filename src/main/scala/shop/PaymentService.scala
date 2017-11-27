package shop

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import checkout.CheckoutMessages
import shop.PaymentService.DoPayment

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class PaymentService(checkout: ActorRef) extends Actor with ActorLogging {

  override val supervisorStrategy: OneForOneStrategy = OneForOneStrategy(loggingEnabled = false) {
    case _: RepeatPaymentException =>
      log.warning("Repeating payment")
      Restart
    case e: PaymentServerException =>
      log.error("Unexpected failure: {}", e.getMessage)
      Stop
  }
  var worker: ActorRef = _

  override def receive: Receive = {
    case DoPayment(payment: String) =>
      worker = context.actorOf(Props(new PaymentWorker(payment)))
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
    if(Math.random() < 0.05) throw new PaymentServerException
    val uri = payment match {
      case "paypal" => s"http://127.0.0.1:5000/$payment"
      case "visa" =>
        val status = if(Math.random() < 0.3) 200 else 408
        s"http://httpbin.org/status/$status"
    }
    println(uri)
    http.singleRequest(HttpRequest(uri = uri))
      .pipeTo(self)
  }

  def receive = {
    case resp@HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        resp.discardEntityBytes()
        shutdown()
      }
    case resp@HttpResponse(StatusCodes.RequestTimeout, _, _, _) =>
      resp.discardEntityBytes()
      throw new RepeatPaymentException

  }

  def shutdown(): Future[Terminated] = {
    Await.result(http.shutdownAllConnectionPools(), Duration.Inf)
    context.system.terminate()
  }
}

class RepeatPaymentException extends Exception("Repeat")

class PaymentServerException extends Exception("Stop")


