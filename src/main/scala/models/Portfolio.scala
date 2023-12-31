package models

case class Portfolio(stockQuantity: Map[StockModel, Int]) {

  def calculatePortfolioPrice(stockQuantityPrice: Map[StockModel, Double]): Double = {
    val combination = stockQuantity.map(keyValue => keyValue._2.toDouble * stockQuantityPrice.getOrElse(keyValue._1, 0D))
    combination.foldRight(0D)(_ + _)
  }
}

case class StockModel(symbol: String, exchange: String)
case class StockQuantity(symbol: String, exchange: String, quantity: Int)

import spray.json._
object PortfolioJsonProtocol extends DefaultJsonProtocol {

  implicit val stockFormat = jsonFormat2(StockModel)
  implicit val stockQuantityFormat = jsonFormat3(StockQuantity)
  implicit val portfolioFormat = jsonFormat1(Portfolio)
}
