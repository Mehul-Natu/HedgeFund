package stocks

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import clients.FMPClientHandler
import clients.FMPClientHandler.HttpRequestAndResponse
import stocks.Stock.{GetStockPrice, StockRequestResponse}

object StockManager {

  trait StockManagerRequest

  //case class StartExchange(exchangeName: String)
  case class GetTotalPriceOfPortfolio(portfolio: Portfolio) extends StockManagerRequest

  case class CreateStockIfNotPresent(exchange: String, symbol: String) extends StockManagerRequest

  case class CreatePortfolioStocks(portfolio: Portfolio) extends StockManagerRequest

  case class StockPrice(symbol: String) extends StockManagerRequest

  case class UpdateStockPriceForPortfolio(stockModel: StockModel, price: Double, portfolioId: Integer) extends StockManagerRequest


  def apply(): Behavior[StockManagerRequest] = requestHandler(Map(), Map(), Map(), 0, Map())


  def requestHandler(stockMap: Map[String, ActorRef[StockRequestResponse]],
                     stockToPriceAndLastUpdateTime: Map[StockModel, (Double, Long)],
                     idToPortfolio: Map[Integer, Portfolio], newPortfolioId: Integer,
                     portfolioRemainingStock: Map[Integer, Set[StockModel]]):
  Behavior[StockManagerRequest] =
    Behaviors.setup { context =>

      val clientActorRef = context.spawn(FMPClientHandler(), "fmpClient")

      Behaviors.receiveMessage {

        case CreateStockIfNotPresent(exchange, symbol) =>
          if (!stockMap.contains(symbol)) {
            val stockRef = context.spawn(Stock(symbol, exchange, clientActorRef), symbol)
            requestHandler(stockMap + (symbol -> stockRef), stockToPriceAndLastUpdateTime, idToPortfolio, newPortfolioId,
              portfolioRemainingStock)
          } else Behaviors.same

        case CreatePortfolioStocks(portfolio) =>
          val stockNotPresentInSystem = portfolio.stockQuantity.filter(value => !stockMap.contains(value._1.symbol))
          val newStockRefMap = stockNotPresentInSystem.map { keyValue =>
            keyValue._1.symbol -> context.spawn(
              Stock(keyValue._1.symbol, keyValue._1.exchange, clientActorRef), keyValue._1.symbol)
          }
          requestHandler(stockMap ++ newStockRefMap, stockToPriceAndLastUpdateTime,
            idToPortfolio, newPortfolioId + 1, portfolioRemainingStock)

        case GetTotalPriceOfPortfolio(portfolio) =>
          val stockRefForPortFolio = (for (stockModel <- portfolio.stockQuantity.keys) yield stockMap.get(stockModel.symbol))
            .filter(optional => optional.isDefined)
          stockRefForPortFolio.foreach(value => value.get ! GetStockPrice(newPortfolioId))
          val updatedPortfolioRemainingStock: Map[Integer, Set[StockModel]] = portfolioRemainingStock + (newPortfolioId -> portfolio.getStocks())
          requestHandler(stockMap, stockToPriceAndLastUpdateTime, idToPortfolio + (newPortfolioId -> portfolio),
            1 + newPortfolioId, updatedPortfolioRemainingStock)

        case UpdateStockPriceForPortfolio(stockModel, price, portfolioId) =>
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
            val updatePortfolioRemainingStock: Map[Integer, Set[StockModel]] =
              portfolioRemainingStock + (portfolioId -> thisPortfolioRemainingStock)
            requestHandler(stockMap, updateStockPriceAndTime, idToPortfolio, newPortfolioId, updatePortfolioRemainingStock)
          } else {
            requestHandler(stockMap, updateStockPriceAndTime, idToPortfolio, newPortfolioId, portfolioRemainingStock)
          }
      }
    }


}

object Stock {

  trait StockRequestResponse
  case object UpdateStockPrice extends StockRequestResponse
  case class GetStockPrice(portfolioId: Int) extends StockRequestResponse
  case class NewStockPrice(price: Double) extends StockRequestResponse

  def apply(symbol: String, exchange: String, clientRef: ActorRef[HttpRequestAndResponse]): Behavior[StockRequestResponse] =
    requestHandler(symbol, exchange, clientRef)

    def requestHandler(symbol: String, exchange: String, clientRef: ActorRef[HttpRequestAndResponse], price: Double = -1D): Behavior[StockRequestResponse] = Behaviors.setup {
      context =>

        Behaviors.receiveMessage {
          case NewStockPrice(price) =>
            requestHandler(symbol, exchange, clientRef, price)
          case GetStockPrice(portfolioId) =>
            //clientRef !
            Behaviors.same

        }
    }
}
