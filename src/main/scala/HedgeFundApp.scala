import Prediction.Constants.PATH_TO_STOCK_TO_CLUSTER
import actors.Diversification
import actors.Diversification.{DiversifyRequest, Initialize}
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.util.Timeout
import clients.AVClientHandler
import clients.AVClientHandler.HttpRequestAndResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}



object HedgeFundApp {

  def main(args: Array[String]): Unit = {


    def startHttpServer(stockManagerAndDiversifyingActor: StockManagerAndDiversifyingActor)(implicit system: ActorSystem[_]): Unit = {
      val router = new HedgeFundRoutes(stockManagerAndDiversifyingActor.clientRef,
        stockManagerAndDiversifyingActor.diversifyActor)
      val routes = router.routes
      val httpBindingFuture = Http().newServerAt("localhost", 8080).bind(routes)
      httpBindingFuture.onComplete {
        case Success(binding) =>
          val address = binding.localAddress
          system.log.info(s"server online at ${address.getAddress}:${address.getPort}")
        case Failure(exception) =>
          system.log.error(s"Error while starting server because $exception")
          system.terminate()
      }
    }

    case class StockManagerAndDiversifyingActor(clientRef: ActorRef[HttpRequestAndResponse],
                                                diversifyActor: ActorRef[DiversifyRequest])

    trait RootCommand
    case class RetrieveStockManagerAndDiversifying(replyTo: ActorRef[StockManagerAndDiversifyingActor]) extends RootCommand

    val rootBehaviour: Behavior[RootCommand] = Behaviors.setup {
      context =>
        implicit val executionContext: ExecutionContext = context.system.executionContext

        val diversifyingActor = context.spawn(Diversification(None), "diversifyingActor")
        diversifyingActor ! Initialize
        val clientRef = context.spawn(AVClientHandler(), "clientHandler")
        //val stockManager = context.spawn(StockManager(clientRef), "stockManager")

        Behaviors.receiveMessage {
          case RetrieveStockManagerAndDiversifying(replyTo) =>
            replyTo ! StockManagerAndDiversifyingActor(clientRef, diversifyingActor)
            Behaviors.same
        }
    }

    implicit val rootActorSystem: ActorRef[RootCommand] = ActorSystem(rootBehaviour, "rootSystem")

    implicit val timeout: Timeout = Timeout(10.seconds)
    implicit val actorSystemScheduler = ActorSystem(Behaviors.empty, "schedulerSystem")
    //val scheduler: Scheduler = actorSystemScheduler.scheduler

    val futureActors: Future[StockManagerAndDiversifyingActor] =
      rootActorSystem.ask(replyTo => RetrieveStockManagerAndDiversifying(replyTo))

    //implicit val actorSystemNoBehaviour = ActorSystem(Behaviors.empty, "schedulerSystem")
    futureActors.foreach(startHttpServer)
  }
}
