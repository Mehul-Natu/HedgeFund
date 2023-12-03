package stocks

import scala.math.Fractional.Implicits.infixFractionalOps

class Portfolio(val stockQuantity: Map[StockModel, Integer]) {

  def calculatePortfolioPrice(stockQuantityPrice: Map[StockModel, Double]): Double = {
    val combination = stockQuantity.map(keyValue => keyValue._2.toDouble * stockQuantityPrice.getOrElse(keyValue._1, 0D))
    combination.foldRight(0D)(_ + _)
    //for (value <- stockQuantity.keys) yield stockQuantity.get(value) * stockQuantityPrice.get(value)
  }

  def getStocks() = stockQuantity.keys.toSet
  def getStocksIterable() = stockQuantity.keys

}

case class StockModel(symbol: String, exchange: String)
