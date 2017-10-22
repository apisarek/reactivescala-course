import akka.actor.{ActorSystem, Props}

object ShopApp extends App {
  import Cart._
  val system = ActorSystem("ShopSystem")

  val cart = system.actorOf(Props(new Cart()))

  cart ! ItemAdded
  println(cart)
}
