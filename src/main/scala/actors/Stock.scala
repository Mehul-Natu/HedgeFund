package actors

import actors.StockManager.{StockFailureResponse, StockManagerRequest, UpdateStockPriceForPortfolio, UpdateTimeSeriesDataForPortfolio}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import clients.AVClientHandler
import models.{StockModel, StockTimeSeriesDataModel}

object Stock {

  import AVClientHandler._

  val FIFTEEN_MINUTE_IN_MILLISECONDS = 900000


  //state 1 - request - (to get latest price), (to get multiple prices of a time period)  then ask Http Clinet to get price
  //state 2 - wating for price update response - return the response to the requester and go to state 1

  trait TimeWindow {
    def getString: String
  }
  case object IntraDayTimeWindow extends TimeWindow {
    def getString: String = "TIME_SERIES_INTRADAY"
  }
  case object DailyTimeWindow extends TimeWindow {
    def getString: String = "TIME_SERIES_DAILY"
  }
  case object WeeklyTimeWindow extends TimeWindow {
    def getString: String = "TIME_SERIES_WEEKLY"
  }
  case object MonthlyTimeWindow extends TimeWindow {
    def getString: String = "TIME_SERIES_MONTHLY"
  }

  trait IntraDayWindowSplit {
    def getString: String
  }
  case object EmptyWindowSplit extends IntraDayWindowSplit {
    def getString: String = ""
  }
  case object OneMin extends IntraDayWindowSplit {
    def getString: String = "1min"
  }
  case object FifteenMin extends IntraDayWindowSplit {
    def getString: String = "15min"
  }
  case object SixtyMin extends IntraDayWindowSplit {
    def getString: String = "60min"
  }


  trait StockRequest

  case object UpdateStockPrice extends StockRequest

  case class GetStockPriceForPortfolio(portfolioId: Int, replyTo: ActorRef[StockManagerRequest]) extends StockRequest

  case class GetStocksTimeSeriesData(portfolioId: Int, replyTo: ActorRef[StockManagerRequest], timeWindow: TimeWindow,
                                     intraDayWindowSplit: IntraDayWindowSplit) extends StockRequest

  case class FetchedStockPriceWithId(price: Double, requestId: Int) extends StockRequest

  case class FetchedStockTimeSeriesData(stockData: StockTimeSeriesDataModel, timeWindow: TimeWindow,
                                        intraDayWindowSplit: IntraDayWindowSplit, id: Int) extends StockRequest

  case class HttpFailureRequestFromHttpClient(message: String, exception: String, id: Int) extends StockRequest

  def apply(symbol: String, exchange: String, clientRef: ActorRef[HttpRequestAndResponse]): Behavior[StockRequest] =
    waitingForRequest(symbol, exchange, clientRef)


  def waitingForRequest(symbol: String, exchange: String, clientRef: ActorRef[HttpRequestAndResponse]): Behavior[StockRequest] =
    Behaviors.receive { (context, message) =>
      message match {
        case GetStockPriceForPortfolio(portfolioId, replyTo) =>
          clientRef ! HttpGetPriceRequest(symbol, exchange, context.self, 0)
          waitingForClientResponse(symbol, exchange, clientRef, replyTo, portfolioId)

        case GetStocksTimeSeriesData(portfolioId, replyTo, IntraDayTimeWindow, intraDayWindowSplit) =>
          clientRef ! HttpGetTimeSeriesForIntraDay(symbol, exchange, context.self, intraDayWindowSplit, portfolioId)
          waitingForClientResponse(symbol, exchange, clientRef, replyTo, portfolioId)

        case GetStocksTimeSeriesData(portfolioId, replyTo, timeWindow, _) =>
          clientRef ! HttpGetTimeSeries(symbol, exchange, context.self, timeWindow, portfolioId)
          waitingForClientResponse(symbol, exchange, clientRef, replyTo, portfolioId)
      }
    }

  def waitingForClientResponse(symbol: String, exchange: String, clientRef: ActorRef[HttpRequestAndResponse],
                               requesterRef: ActorRef[StockManagerRequest], portfolioId: Int): Behavior[StockRequest] =
    Behaviors.receive { (context, message) =>
      message match {
        case FetchedStockPriceWithId(price, requestId) =>
          println(s"Recieved New Price $price for ID: $requestId")
          context.log.info(s"Recieved New Price for ID: $requestId")
          requesterRef ! UpdateStockPriceForPortfolio(StockModel(symbol, exchange), price, portfolioId)
          waitingForRequest(symbol, exchange, clientRef)

        case FetchedStockTimeSeriesData(stockData, timeWindow, intraDayWindowSplit, id) =>
          context.log.info(s"Stock: Received TimeSeries Data[$timeWindow]")
          requesterRef ! UpdateTimeSeriesDataForPortfolio(StockModel(symbol, exchange), stockData, timeWindow, intraDayWindowSplit, id)
          waitingForRequest(symbol, exchange, clientRef)

        case HttpFailureRequestFromHttpClient(message, exception, id) =>
          requesterRef ! StockFailureResponse(message, exception, id)
          waitingForRequest(symbol, exchange, clientRef)
      }
    }

}









/*
object TestStock extends App {
  val actorClient = ActorSystem(AVClientHandler(), "actorClinet")
  val actor = ActorSystem(Stock("PRAA", "NASDAQ", actorClient), "actor")
  //val dummyActorRef =
  val actorManager = ActorSystem(StockManager(actorClient), "actormanag")
  //actor ! GetStockPriceForPortfolio(1, actorManager)
  actor ! GetStocksTimeSeriesData(0, actorManager, DailyTimeWindow, EmptyWindowSplit)
  //Thread.sleep(5000)
  //actor ! GetStockPriceForPortfolio(1, null)
}

case class StockPrice(price: Double) extends StockRequestResponse
  def requestHandler(symbol: String, exchange: String, clientRef: ActorRef[HttpRequestAndResponse],
                     price: Double = -1D, lastUpdate: Long,
                     requestIdToPortfolioIdAndRef: Map[Int, (Int, ActorRef[StockManagerRequest])],
                     requestId: Int):
  Behavior[StockRequestResponse] = Behaviors.setup {
    context =>

      Behaviors.receiveMessage {

        case StockPrice(price) =>
          requestHandler(symbol, exchange, clientRef, price, System.currentTimeMillis(),
            requestIdToPortfolioIdAndRef, requestId)

        case StockPriceWithId(price, requestId) =>
          println(s"Recieved New Price $price for ID: $requestId")
          context.log.info(s"Recieved New Price for ID: $requestId")
          val portfolioId = requestIdToPortfolioIdAndRef(requestId)._1
          val senderRef = requestIdToPortfolioIdAndRef(requestId)._2
          senderRef ! UpdateStockPriceForPortfolio(StockModel(symbol, exchange), price, portfolioId)
          requestHandler(symbol, exchange, clientRef, price, System.currentTimeMillis(),
            requestIdToPortfolioIdAndRef - requestId, requestId)

        case GetStockPriceForPortfolio(portfolioId, replyTo) =>
          println(s"Recieved stock price for portfolio request: $portfolioId")
          context.log.info(s"Recieved stock price for portfolio request: $portfolioId")
          if (System.currentTimeMillis() - lastUpdate <= FIFTEEN_MINUTE_IN_MILLISECONDS) {
            replyTo ! UpdateStockPriceForPortfolio(StockModel(symbol, exchange), price, portfolioId)
            println("In Here")
            Behaviors.same
          } else {
            clientRef ! HttpGetPriceRequest(symbol, exchange, context.self, requestId)
            requestHandler(symbol, exchange, clientRef, price, lastUpdate,
              requestIdToPortfolioIdAndRef + (requestId -> (portfolioId, replyTo)), requestId + 1)
          }
      }
  }

 */