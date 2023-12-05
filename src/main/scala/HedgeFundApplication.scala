import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpProtocols, HttpResponse, ResponseEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.Complete
import models.{Portfolio, PortfolioJsonProtocol, StockModel, StockQuantity}
import spray.json.DefaultJsonProtocol.mapFormat
import spray.json._
import actors.StockManager.{ResponseForPortFolioValue, StockManagerRequestResponse}

import scala.concurrent.Future



class HedgeFundApplication(stockManager: ActorRef[StockManagerRequestResponse]) extends PortfolioJsonProtocol with SprayJsonSupport {

  implicit val actorSystem = ActorSystem(Behaviors.empty, "HedgeFundApp")

  val route: Route = (path("api" / "portfolio") & post){
    entity(as[List[StockQuantity]]) { listOfStock: List[StockQuantity] =>
      complete(getPortfolioValue(listOfStock))
    }
  }


  def getPortfolioValue(stockQuantity: List[StockQuantity]): Future[ResponseForPortFolioValue] = {

  }





  def main(args: Array[String]): Unit = {

    Http().newServerAt("localhost", 8081).bind(route)
  }

}
