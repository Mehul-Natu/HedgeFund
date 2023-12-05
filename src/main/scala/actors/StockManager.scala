package actors

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, PostStop, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import clients.{AVClientHandler, StockTimeSeriesDataModel}
import clients.AVClientHandler.{HttpGetPriceRequest, HttpRequestAndResponse}
import models.{Portfolio, StockModel}
import actors.Stock.{FifteenMin, GetStockPriceForPortfolio, GetStocksTimeSeriesData, IntraDayTimeWindow, IntraDayWindowSplit, StockRequestResponse, TimeWindow}
import actors.StockManager.{CreatePortfolioStocks, CreateStockIfNotPresentResponse, GetTimeSeriesDataOfPortfolio, GetTotalPriceOfPortfolio, UpdateStockPriceForPortfolioResponse}


object StockManager {



  //state to accept request create stocks and then send request to self for the same (request to calculate portfolio price total) (request to get time series data for all stock)
  //state to wait for calculation stage then transfer to waiting for response stage
  //wating for response from all the stock once received reture the response to sender


  trait StockManagerRequestResponse

  //case class StartExchange(exchangeName: String)
  final case class GetTotalPriceOfPortfolio(portfolio: Portfolio) extends StockManagerRequestResponse
  final case class GetTimeSeriesDataOfPortfolio(portfolio: Portfolio, timeWindow: TimeWindow,
                                                intraDayWindowSplit: IntraDayWindowSplit) extends StockManagerRequestResponse


  final case class CreateStockIfNotPresentResponse(exchange: String, symbol: String) extends StockManagerRequestResponse

  final case class CreatePortfolioStocks(portfolio: Portfolio) extends StockManagerRequestResponse

  final case class StockPriceResponse(symbol: String) extends StockManagerRequestResponse

  final case class UpdateTimeSeriesDataForPortfolio(stock: StockModel, stockData: StockTimeSeriesDataModel, timeWindow: TimeWindow,
                                                    intraDayWindowSplit: IntraDayWindowSplit, id: Int) extends StockManagerRequestResponse

  final case class UpdateStockPriceForPortfolioResponse(stockModel: StockModel, price: Double, portfolioId: Int) extends StockManagerRequestResponse

  final case class ResponseForPortFolioValue(totalValue: Double, stockToData: Map[StockModel, Double], portfolio: Portfolio)

  final case class ResponseForPortFolioTimeSeriesData(totalValue: Double, stockToData: Map[StockModel, StockTimeSeriesDataModel],
                                                      timeWindow: TimeWindow,
                                                      intraDayWindowSplit: IntraDayWindowSplit) extends StockManagerRequestResponse


  def apply(clientActorRef: ActorRef[HttpRequestAndResponse]):
  Behavior[StockManagerRequestResponse] = waitingForUserRequest(clientActorRef)


  def waitingForUserRequest(clientActorRef: ActorRef[HttpRequestAndResponse]): Behavior[StockManagerRequestResponse] =
    Behaviors.receive {(context, message) =>
      message match {
        case GetTotalPriceOfPortfolio(portfolio) =>
          context.log.info(s"Got the request for portfolio Stocks: $portfolio")

          //TODO add check if the stock is not present already
          val stockRefMap: Map[String, ActorRef[StockRequestResponse]] = portfolio.stockQuantity.map { keyValue =>
            (keyValue._1.symbol -> context.spawn(
              Stock(keyValue._1.symbol, keyValue._1.exchange, clientActorRef), keyValue._1.symbol))
          }
          stockRefMap.values.foreach(stock => stock ! GetStockPriceForPortfolio(0, context.self))
          val stockNames: Set[String] = stockRefMap.keys.toSet
          waitingForStocksPriceResponse(portfolio, stockRefMap, clientActorRef, stockNames, Map())

        case GetTimeSeriesDataOfPortfolio(portfolio, timeWindow, intraDayWindowSplit) =>
          context.log.info(s"Got the request for portfolio time series data Stocks: $portfolio")

          //TODO add check if the stock is not present already
          val stockRefMap = portfolio.stockQuantity.map { keyValue =>
            (keyValue._1.symbol -> context.spawn(
              Stock(keyValue._1.symbol, keyValue._1.exchange, clientActorRef), keyValue._1.symbol))
          }

          stockRefMap.values.foreach(stock => stock ! GetStocksTimeSeriesData(0, context.self, timeWindow, intraDayWindowSplit))
          val stockNames: Set[String] = stockRefMap.keys.toSet

          waitingForStocksTimeSeriesDataResponse(portfolio, stockRefMap, timeWindow, intraDayWindowSplit, clientActorRef, stockNames, Map())
      }
    }

  def waitingForStocksPriceResponse(portfolio: Portfolio, stockMap: Map[String, ActorRef[StockRequestResponse]],
                                              clientActorRef: ActorRef[HttpRequestAndResponse],
                                    stocksLeftForData: Set[String],
                                    stockToData: Map[StockModel, Double]): Behavior[StockManagerRequestResponse] =
    Behaviors.receive { (context, message) =>
      message match {
        case UpdateStockPriceForPortfolioResponse(stockModel, price, portfolioId) =>
          val updatedStocksLeft = stocksLeftForData - stockModel.symbol
          val updatedStockData = stockToData + (stockModel -> price)
          if (updatedStocksLeft.isEmpty) {
            //to send response
            context.log.info("total Price" + portfolio.calculatePortfolioPrice(updatedStockData))
            waitingForUserRequest(clientActorRef)
          } else {
            waitingForStocksPriceResponse(portfolio, stockMap, clientActorRef, updatedStocksLeft, updatedStockData)
          }
      }
    }

  def waitingForStocksTimeSeriesDataResponse(portfolio: Portfolio, stockMap: Map[String, ActorRef[StockRequestResponse]], timeWindow: TimeWindow,
                                              intraDayWindowSplit: IntraDayWindowSplit,
                                              clientActorRef: ActorRef[HttpRequestAndResponse],
                                             stocksLeftForData: Set[String],
                                             stockToData: Map[StockModel, StockTimeSeriesDataModel]): Behavior[StockManagerRequestResponse] =
    Behaviors.receive { (context, message) =>
      message match {
        case UpdateTimeSeriesDataForPortfolio(stockModel, stockData, timeWindow, intraDayWindowSplit, portfolioId) =>
          val updatedStocksLeft = stocksLeftForData - stockModel.symbol
          val updatedStockData = stockToData + (stockModel -> stockData)
          if (updatedStocksLeft.isEmpty) {
            //to send response with time window and everything
            context.log.info("total Stock Data" + updatedStockData.keys)
            waitingForUserRequest(clientActorRef)
          } else {
            waitingForStocksTimeSeriesDataResponse(portfolio, stockMap, timeWindow, intraDayWindowSplit,
              clientActorRef, updatedStocksLeft, updatedStockData)
          }
      }
    }



  def requestHandler(stockMap: Map[String, ActorRef[StockRequestResponse]],
                     stockToPriceAndLastUpdateTime: Map[StockModel, (Double, Long)],
                     idToPortfolio: Map[Int, Portfolio], newPortfolioId: Int,
                     portfolioRemainingStock: Map[Int, Set[StockModel]],
                     clientActorRef: ActorRef[HttpRequestAndResponse],
                    ):
  Behavior[StockManagerRequestResponse] =
    Behaviors.receive[StockManagerRequestResponse] { (context, message) =>

      //val clientActorRef = context.spawn(AVClientHandler(), "fmpClient")
      //println("here again")

      message match {
        case CreateStockIfNotPresentResponse(exchange, symbol) =>
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

        case UpdateStockPriceForPortfolioResponse(stockModel, price, portfolioId) =>

          println(s"Updating $stockModel price $price for portfolio Stocks: $portfolioId")
          val updateStockPriceAndTime = stockToPriceAndLastUpdateTime + (stockModel -> (price, System.currentTimeMillis()))
          if (portfolioRemainingStock.contains(portfolioId)) {
            val thisPortfolioRemainingStock = portfolioRemainingStock.getOrElse(portfolioId, Set()) - stockModel
            if (thisPortfolioRemainingStock.isEmpty) {
              //todo delete all the references of this portfolio and ID
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


}


object TestStockManager {

   def main(args: Array[String]): Unit = {
     //val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig")
     val avClinet = ActorSystem(AVClientHandler(), "AVClient")
     val actorClient = ActorSystem(StockManager(avClinet), "actorClinet")
     val stockQuantityPrice: Map[StockModel, Int] = Map(StockModel("PRAA", "NASDAQ") -> 10, StockModel("IBM", "NASDAQ") -> 10)
     val portfolio = models.Portfolio(stockQuantityPrice)

     //actorClient ! CreateStockIfNotPresent("lol", "shdcvb")

     // ! CreatePortfolioStocks(portfolio)
     //actorClient ! GetTotalPriceOfPortfolio(portfolio)
     actorClient ! GetTimeSeriesDataOfPortfolio(portfolio, IntraDayTimeWindow, FifteenMin)
    //Thread.sleep(5000)

    //actorClient ! UpdateStockPriceForPortfolio(StockModel("PRAA", "NASDAQ"), 10D, 1)
  }

  //actorClient ! GetTotalPriceOfPortfolio(portfolio)

}

