package actors

import actors.Stock._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import clients.AVClientHandler.HttpRequestAndResponse
import models.{Portfolio, StockModel, StockTimeSeriesDataModel}


object StockManager {

  //state to accept request create stocks and then send request to self for the same (request to calculate portfolio price total) (request to get time series data for all stock)
  //state to wait for calculation stage then transfer to waiting for response stage
  //wating for response from all the stock once received reture the response to sender


  trait StockManagerRequest

  case class GetTotalPriceOfPortfolio(portfolio: Portfolio, replyTo: ActorRef[StockManagerResponse]) extends StockManagerRequest
  case class GetTimeSeriesDataOfPortfolio(portfolio: Portfolio, timeWindow: TimeWindow,
                                                intraDayWindowSplit: IntraDayWindowSplit,
                                                replyTo: ActorRef[StockManagerResponse]) extends StockManagerRequest
  case class UpdateTimeSeriesDataForPortfolio(stock: StockModel, stockData: StockTimeSeriesDataModel, timeWindow: TimeWindow,
                                                    intraDayWindowSplit: IntraDayWindowSplit, id: Int) extends StockManagerRequest
  case class UpdateStockPriceForPortfolio(stockModel: StockModel, price: Double, portfolioId: Int) extends StockManagerRequest

  case class StockFailureResponse(message: String, exception: String, id: Int) extends StockManagerRequest


  trait StockManagerResponse
  case class ResponseForPortFolioValue(totalValue: Double, stockToData: Map[StockModel, Double], portfolio: Portfolio)
    extends StockManagerResponse
  case class ResponseForPortFolioTimeSeriesData(stockToData: Map[String, StockTimeSeriesDataModel],
                                                      timeWindow: TimeWindow,
                                                      intraDayWindowSplit: IntraDayWindowSplit) extends StockManagerResponse
  case class ResponseForFailedCase(message: String, exception: String) extends StockManagerResponse

  def apply(clientActorRef: ActorRef[HttpRequestAndResponse]):
  Behavior[StockManagerRequest] = waitingForUserRequest(clientActorRef)


  def waitingForUserRequest(clientActorRef: ActorRef[HttpRequestAndResponse]): Behavior[StockManagerRequest] =
    Behaviors.receive {(context, message) =>
      message match {
        case GetTotalPriceOfPortfolio(portfolio, replyTo) =>
          context.log.info(s"Got the request for portfolio Stocks: $portfolio")

          val stockRefMap: Map[String, ActorRef[StockRequest]] = portfolio.stockQuantity.map { keyValue =>
            (keyValue._1.symbol -> context.spawn(
              Stock(keyValue._1.symbol, keyValue._1.exchange, clientActorRef), keyValue._1.symbol))
          }
          stockRefMap.values.foreach(stock => stock ! GetStockPriceForPortfolio(0, context.self))
          val stockNames: Set[String] = stockRefMap.keys.toSet
          waitingForStocksPriceResponse(portfolio, stockRefMap, clientActorRef, stockNames, Map(), replyTo)

        case GetTimeSeriesDataOfPortfolio(portfolio, timeWindow, intraDayWindowSplit, replyTo) =>
          context.log.info(s"Got the request for portfolio time series data Stocks: $portfolio")

          val stockRefMap = portfolio.stockQuantity.map { keyValue =>
            (keyValue._1.symbol -> context.spawn(
              Stock(keyValue._1.symbol, keyValue._1.exchange, clientActorRef), keyValue._1.symbol))
          }

          stockRefMap.values.foreach(stock => stock ! GetStocksTimeSeriesData(0, context.self, timeWindow, intraDayWindowSplit))
          val stockNames: Set[String] = stockRefMap.keys.toSet

          waitingForStocksTimeSeriesDataResponse(portfolio, stockRefMap, timeWindow, intraDayWindowSplit,
            clientActorRef, stockNames, Map(), replyTo)
      }
    }

  def waitingForStocksPriceResponse(portfolio: Portfolio, stockMap: Map[String, ActorRef[StockRequest]],
                                    clientActorRef: ActorRef[HttpRequestAndResponse],
                                    stocksLeftForData: Set[String], stockToData: Map[StockModel, Double],
                                    replyTo: ActorRef[StockManagerResponse]): Behavior[StockManagerRequest] =
    Behaviors.receive { (context, message) =>
      message match {
        case UpdateStockPriceForPortfolio(stockModel, price, portfolioId) =>
          val updatedStocksLeft = stocksLeftForData - stockModel.symbol
          val updatedStockData = stockToData + (stockModel -> price)
          if (updatedStocksLeft.isEmpty) {
            val totalValue = portfolio.calculatePortfolioPrice(updatedStockData)
            replyTo ! ResponseForPortFolioValue(totalValue, updatedStockData, portfolio)
            context.log.info("total Price" + portfolio.calculatePortfolioPrice(updatedStockData))
            //waitingForUserRequest(clientActorRef)
            Behaviors.stopped
          } else {
            waitingForStocksPriceResponse(portfolio, stockMap, clientActorRef, updatedStocksLeft, updatedStockData, replyTo)
          }
        case StockFailureResponse(message, exception, id) =>
          replyTo ! ResponseForFailedCase(message, exception)
          Behaviors.stopped
      }
    }

  def waitingForStocksTimeSeriesDataResponse(portfolio: Portfolio, stockMap: Map[String, ActorRef[StockRequest]], timeWindow: TimeWindow,
                                             intraDayWindowSplit: IntraDayWindowSplit,
                                             clientActorRef: ActorRef[HttpRequestAndResponse],
                                             stocksLeftForData: Set[String], stockToData: Map[String, StockTimeSeriesDataModel],
                                             replyTo: ActorRef[StockManagerResponse]): Behavior[StockManagerRequest] =
    Behaviors.receive { (context, message) =>
      message match {
        case UpdateTimeSeriesDataForPortfolio(stockModel, stockData, timeWindow, intraDayWindowSplit, portfolioId) =>
          val updatedStocksLeft = stocksLeftForData - stockModel.symbol
          val updatedStockData = stockToData + (stockModel.symbol -> stockData)
          if (updatedStocksLeft.isEmpty) {
            context.log.info("total Stock Data" + updatedStockData.keys)
            replyTo ! ResponseForPortFolioTimeSeriesData(updatedStockData, timeWindow, intraDayWindowSplit)
            context.log.info("total Stock Data Sent" + updatedStockData.keys)
            //waitingForUserRequest(clientActorRef)
            Behaviors.stopped
          } else {
            waitingForStocksTimeSeriesDataResponse(portfolio, stockMap, timeWindow, intraDayWindowSplit,
              clientActorRef, updatedStocksLeft, updatedStockData, replyTo)
          }
        case StockFailureResponse(message, exception, id) =>
          replyTo ! ResponseForFailedCase(message, exception)
          Behaviors.stopped
      }
    }
}



/*

 final case class CreateStockIfNotPresent(exchange: String, symbol: String) extends StockManagerRequest

  final case class CreatePortfolioStocks(portfolio: Portfolio) extends StockManagerRequest

  final case class StockPrice(symbol: String) extends StockManagerRequest
  def requestHandler(stockMap: Map[String, ActorRef[StockRequestResponse]],
                     stockToPriceAndLastUpdateTime: Map[StockModel, (Double, Long)],
                     idToPortfolio: Map[Int, Portfolio], newPortfolioId: Int,
                     portfolioRemainingStock: Map[Int, Set[StockModel]],
                     clientActorRef: ActorRef[HttpRequestAndResponse],
                    ):
  Behavior[StockManagerRequest] =
    Behaviors.receive[StockManagerRequest] { (context, message) =>

      //val clientActorRef = context.spawn(AVClientHandler(), "fmpClient")
      //println("here again")

      message match {
        case CreateStockIfNotPresent(exchange, symbol) =>
          /*if (!stockMap.contains(symbol)) {
            val stockRef = context.spawn(Stock(symbol, exchange, clientActorRef), symbol)
            requestHandler(stockMap + (symbol -> stockRef), stockToPriceAndLastUpdateTime, idToPortfolio, newPortfolioId,
              portfolioRemainingStock)
          } else Behaviors.same
           */
          Behaviors.same

        case CreatePortfolioStocks(portfolio) =>
          context.log.info(s"Got the request for portfolio Stocks: $portfolio")

          val stockNotPresentInSystem = portfolio.stockQuantity.filter(value => !stockMap.contains(value._1.symbol))
          val newStockRefMap = stockNotPresentInSystem.map { keyValue =>
            (keyValue._1.symbol -> context.spawn(
              Stock(keyValue._1.symbol, keyValue._1.exchange, clientActorRef), keyValue._1.symbol))
          }
          val finalStockMap = stockMap ++ newStockRefMap
          requestHandler(finalStockMap, stockToPriceAndLastUpdateTime,
            idToPortfolio, newPortfolioId + 1, portfolioRemainingStock, clientActorRef)

        case GetTotalPriceOfPortfolio(portfolio) =>

          println(s"fetching stock prices for portfolio: $portfolio")
          val stockRefForPortFolio = (for (stockModel <- portfolio.stockQuantity.keys) yield stockMap.get(stockModel.symbol))
            .filter(optional => optional.isDefined)
          stockRefForPortFolio.foreach(value => value.get ! GetStockPriceForPortfolio(newPortfolioId, context.self))
          val updatedPortfolioRemainingStock: Map[Int, Set[StockModel]] = portfolioRemainingStock + (newPortfolioId -> portfolio.getStocks())
          println(s"Ending of fetching fetching stock prices for portfolio: $portfolio")
          requestHandler(stockMap, stockToPriceAndLastUpdateTime, idToPortfolio + (newPortfolioId -> portfolio),
            1 + newPortfolioId, updatedPortfolioRemainingStock, clientActorRef)

        case UpdateStockPriceForPortfolio(stockModel, price, portfolioId) =>

          println(s"Updating $stockModel price $price for portfolio Stocks: $portfolioId")
          val updateStockPriceAndTime = stockToPriceAndLastUpdateTime + (stockModel -> (price, System.currentTimeMillis()))
          if (portfolioRemainingStock.contains(portfolioId)) {
            val thisPortfolioRemainingStock = portfolioRemainingStock.getOrElse(portfolioId, Set()) - stockModel
            if (thisPortfolioRemainingStock.isEmpty) {

              val stockPrices: Map[StockModel, Double] = idToPortfolio(portfolioId).getStocksIterable().map { stock =>
                stock -> updateStockPriceAndTime(stock)._1
              }.toMap
              val portfolio = idToPortfolio(portfolioId)
              println("PortFolio Total Price:" + portfolio.calculatePortfolioPrice(stockPrices))
            }
            val updatePortfolioRemainingStock: Map[Int, Set[StockModel]] =
              portfolioRemainingStock + (portfolioId -> thisPortfolioRemainingStock)
            requestHandler(stockMap, updateStockPriceAndTime, idToPortfolio, newPortfolioId, updatePortfolioRemainingStock,
              clientActorRef)
          } else {
            requestHandler(stockMap, updateStockPriceAndTime, idToPortfolio, newPortfolioId, portfolioRemainingStock,
              clientActorRef)
          }
      }
    }.receiveSignal {
      case (context, Terminated(ref)) =>
        println("Job stopped: {}", ref.path.name)
        Behaviors.same
    }
  object TestStockManager {
  implicit val timeout: Timeout = 3.seconds
  //implicit system: =
  //import akka.actor.typed.scaladsl.AskableActorContext

  def main(args: Array[String]): Unit = {
     //val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig")
     val avClinet = ActorSystem(AVClientHandler(), "AVClient")
     val actorClient = ActorSystem(StockManager(avClinet), "actorClinet")
     val stockQuantityPrice: Map[StockModel, Int] = Map(StockModel("PRAA", "NASDAQ") -> 10, StockModel("IBM", "NASDAQ") -> 10)
     val portfolio = models.Portfolio(stockQuantityPrice)

     //actorClient ! CreateStockIfNotPresent("lol", "shdcvb")

     // ! CreatePortfolioStocks(portfolio)
     actorClient ! GetTotalPriceOfPortfolio(portfolio, null)
     //actorClient ! GetTimeSeriesDataOfPortfolio(portfolio, IntraDayTimeWindow, FifteenMin)
    //Thread.sleep(5000)

    //actorClient ! UpdateStockPriceForPortfolio(StockModel("PRAA", "NASDAQ"), 10D, 1)
  }
*/

