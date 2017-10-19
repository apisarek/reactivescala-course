import akka.actor.{ActorSystem, Props}

object ShopApp extends App {
  val system = ActorSystem("ShopSystem")

  val cart = system.actorOf(Props[Cart])

}
