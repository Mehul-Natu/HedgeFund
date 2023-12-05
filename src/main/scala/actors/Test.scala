package actors

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import models.{Portfolio, PortfolioJsonProtocol, StockModel, StockQuantity}
import spray.json.enrichAny

object Test extends PortfolioJsonProtocol with SprayJsonSupport {

  def main(args: Array[String]): Unit = {
    val stockQuantityAndPrice: Map[StockModel, (Integer, Option[Double])] = {
      Map(StockModel("lol", "yes") -> (2, None))

    }
    val newMap = stockQuantityAndPrice - StockModel("lol", "yes")
    println(newMap)

    val port = List(StockQuantity("LOL", "NASDAQ", 10), StockQuantity("Yes", "NASDAQ", 20))

    println(port.toJson)
  }

}
