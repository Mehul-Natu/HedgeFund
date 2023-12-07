import actors.Diversification._
import actors.Stock._
import actors.StockManager
import actors.StockManager._
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import clients.AVClientHandler.HttpRequestAndResponse
import models.{Portfolio, PortfolioJsonProtocol, StockModel, StockQuantity}
import spray.json.DefaultJsonProtocol

import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.duration._
//import io.circe.generic.auto._
//import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import spray.json._


case class PortfolioValueRequest(stockQuantity: List[StockQuantity]) {
  def toStockManagerRequest(replyTo: ActorRef[StockManagerResponse]): StockManagerRequest = {
    GetTotalPriceOfPortfolio(Portfolio(stockQuantity.map(stockQuantity =>
      (StockModel(stockQuantity.symbol, stockQuantity.exchange), stockQuantity.quantity)).toMap), replyTo)
  }
}
case class PortfolioTimeSeriesRequest(stockQuantity: List[StockQuantity], timeWindowString: String, timeSplitString: String) {
  def toStockManagerRequest(replyTo: ActorRef[StockManagerResponse]): StockManagerRequest = {
    val portfolio = Portfolio(stockQuantity.map(stockQuantity =>
      (StockModel(stockQuantity.symbol, stockQuantity.exchange), stockQuantity.quantity)).toMap)
    val timeWindow = timeWindowString match {
      case "intra" => IntraDayTimeWindow
      case "daily" => DailyTimeWindow
    }
    val timeSplit = timeSplitString match {
      case "1min" => OneMin
      case "15min" => FifteenMin
      case "60Min" => SixtyMin
      case _ => EmptyWindowSplit
    }
    GetTimeSeriesDataOfPortfolio(portfolio, timeWindow, timeSplit, replyTo)
  }
}


case class DiversificationCheckForPortfolioRequest(stockQuantity: List[StockQuantity]) {
  def toDiversificationCheckRequest(replyTo: ActorRef[DiversifyResponse]): DiversifyRequest = {
    CheckDiversification(Portfolio(stockQuantity.map(stockQuantity =>
      (StockModel(stockQuantity.symbol, stockQuantity.exchange), stockQuantity.quantity)).toMap), replyTo)
  }
}
case class RecommendationForDiversificationRequest(stockQuantity: List[StockQuantity]) {
  def toRecommendDiversificationRequest(replyTo: ActorRef[DiversifyResponse]): DiversifyRequest = {
    RecommendStockToDiversify(Portfolio(stockQuantity.map(stockQuantity =>
      (StockModel(stockQuantity.symbol, stockQuantity.exchange), stockQuantity.quantity)).toMap), replyTo)
  }
}

object RequestJsonProtocol extends DefaultJsonProtocol {
  import PortfolioJsonProtocol._

  implicit val portfolioValueRequestFormat = jsonFormat1(PortfolioValueRequest)
  implicit val portfolioTimeSeriesRequestFormat = jsonFormat3(PortfolioTimeSeriesRequest)
  implicit val diversificationCheckForPortfolioRequestFormat = jsonFormat1(DiversificationCheckForPortfolioRequest)
  implicit val recommendationForDiversificationRequestFormat = jsonFormat1(RecommendationForDiversificationRequest)

  //data.parseJson.convertTo[Map[String, JsValue]]

  implicit val intFormat: JsonFormat[Int] = new JsonFormat[Int] {
    override def write(x: Int): JsValue = JsNumber(x)

    override def read(value: JsValue): Int = value match {
      case JsNumber(n) => n.intValue
      case _ => throw DeserializationException("Expected a number")
    }
  }

  implicit val stringListFormat: JsonFormat[List[String]] = new JsonFormat[List[String]] {

    override def write(x: List[String]): JsValue = JsArray(
      x.map {
        case s: String => JsString(s)
        case other => serializationError(s"Unexpected element type: ${other.getClass.getName}")
      }
    )

    override def read(value: JsValue): List[String] = value match {
      case JsArray(elements) => elements.collect {
        case JsString(s) => s
      }.toVector.toList
      case _ => throw DeserializationException("Expected an array of strings")
    }
  }

  implicit val mapIntStringListFormat: JsonFormat[Map[Int, List[String]]] = new JsonFormat[Map[Int, List[String]]] {
    override def write(obj: Map[Int, List[String]]): JsValue = {
      val value = obj.map {
        case (key, value) =>
          // Wrap key in JsString
          key.toString -> stringListFormat.write(value)
      }
      JsObject(value)
    }

    override def read(value: JsValue): Map[Int, List[String]] = value match {
      case JsObject(fields) => fields.map { case (key, value) =>
        // Validate key type
        key match {
          case n => n.toInt.intValue -> stringListFormat.read(value)
          case _ => throw DeserializationException(s"Invalid key type: $key (expected number)")
        }
      }.toMap
      case _ => throw DeserializationException("Expected a Map object")
    }
  }

}


class HedgeFundRoutes(clientHttpRef: ActorRef[HttpRequestAndResponse], diversification: ActorRef[DiversifyRequest])
                     (implicit val actorSystem: ActorSystem[_]) extends SprayJsonSupport {

  implicit val timeout: Timeout = Timeout(10.seconds)
  import RequestJsonProtocol._
  import models.StockResponseJsonProtocol._


  def getPortfolioValue(request: PortfolioValueRequest): Future[StockManagerResponse] = {
    val stockManager = createStockManager(clientHttpRef)
    stockManager.ask(replyTo => request.toStockManagerRequest(replyTo))
  }

  def getPortfolioTimeSeries(request: PortfolioTimeSeriesRequest): Future[StockManagerResponse] = {
    val stockManager = createStockManager(clientHttpRef)
    stockManager.ask(replyTo => request.toStockManagerRequest(replyTo))
  }

  def checkDiversificationForPortfolio(request: DiversificationCheckForPortfolioRequest): Future[DiversifyResponse] = {
    diversification.ask(replyTo => request.toDiversificationCheckRequest(replyTo))
  }

  def recommendStocksForDiversification(request: RecommendationForDiversificationRequest): Future[DiversifyResponse] = {
    diversification.ask(replyTo => request.toRecommendDiversificationRequest(replyTo))
  }

  def createStockManager(clientActorRef: ActorRef[HttpRequestAndResponse]): ActorRef[StockManagerRequest] = {
    ActorSystem(StockManager(clientActorRef), "stockManager" + UUID.randomUUID())
  }

  val routes: Route = {
    pathPrefix("edgeFund") {
      concat(
        path("portfolioValue") {
          post {
            entity(as[PortfolioValueRequest]) { request =>
              onSuccess(getPortfolioValue(request)) {
                case ResponseForPortFolioValue(totalValue, stockToPrice, portfolio) =>
                  complete("TotalValue" + totalValue)
              }
            }
          }
        },
        path("portfolioDataSeries") {
          post {
            entity(as[PortfolioTimeSeriesRequest]) { request =>
              onSuccess(getPortfolioTimeSeries(request)) {
                case ResponseForPortFolioTimeSeriesData(stockToData, timeWindow, intraDayWindowSplit) =>
                  complete(stockToData.toJson.toString())
              }
            }
          }
        },
        path("portfolioDiversificationCheck") {
          post {
            entity(as[DiversificationCheckForPortfolioRequest]) { request =>
              onSuccess(checkDiversificationForPortfolio(request)) {
                case DiversificationCheckResponse(Some(value), _) =>
                  complete(value.toJson.toString())
                case DiversificationCheckResponse(None, message) =>
                  complete(message)
              }
            }
          }
        },
        path("portfolioRecommendation") {
          post {
            entity(as[RecommendationForDiversificationRequest]) { request =>
              onSuccess(recommendStocksForDiversification(request)) {
                case DiversificationRecommendationResponse(Some(value), _) =>
                  complete(value.toJson.toString())
                case DiversificationRecommendationResponse(None, message) =>
                  complete(message)
              }
            }
          }
        }
      )
    }
  }
}
