package clients

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object HttpClient {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

}
