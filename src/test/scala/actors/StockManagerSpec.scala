package actors

import actors.Stock._
import actors.StockManager._
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, ScalaTestWithActorTestKit}
import clients.AVClientHandler.HttpRequestAndResponse
import models.{MetaData6, StockDataIntraDay1Min, StockModel, TimeSeriesData}
import org.scalatest.wordspec.AnyWordSpecLike

class StockManagerSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  //all the tests which are together should be run together that also helps checking The flow

  "For Portfolio total value calculation Flow" should {
    val httpClientTestProbe = createTestProbe[HttpRequestAndResponse]("httpClientTestProbe")
    val stockManagerResponseReceiverTestProbe = createTestProbe[StockManagerResponse]("SMResponseReceiverRef")
    val testKitSM = BehaviorTestKit(StockManager(httpClientTestProbe.ref), "stockManager")
    val ibmStock = StockModel("IBM", "NASDAQ")
    val praaStock = StockModel("PRAA", "NASDAQ")
    val stockQuantityPrice: Map[StockModel, Int] = Map(praaStock -> 10, ibmStock -> 10)
    val stockDataPrice = Map(praaStock -> 20.5D, ibmStock -> 32.5)
    val portfolio = models.Portfolio(stockQuantityPrice)

    "Checking if child actors are created and then do they receive price fetching request and actor is alive" in {
      testKitSM.run(GetTotalPriceOfPortfolio(portfolio, stockManagerResponseReceiverTestProbe.ref))
      val testInboxOfPRAA = testKitSM.childInbox[StockRequest]("PRAA")
      val testInboxOfIBM = testKitSM.childInbox[StockRequest]("IBM")

      testInboxOfPRAA.hasMessages shouldBe true
      testInboxOfIBM.hasMessages shouldBe true

      val messageToPRAA = testInboxOfPRAA.receiveMessage
      val messageToIBM = testInboxOfIBM.receiveMessage

      assert(messageToPRAA == GetStockPriceForPortfolio(0, testKitSM.ref))
      assert(messageToIBM == GetStockPriceForPortfolio(0, testKitSM.ref))
      assert(testKitSM.isAlive)
    }

    //always run this test after running above test or whole spec file together
    "Checking Total Price calculation after receiving all the stock price and actor is killed" in {
      testKitSM.run(UpdateStockPriceForPortfolio(praaStock, 20.5D, 0))
      testKitSM.run(UpdateStockPriceForPortfolio(ibmStock, 32.5, 0))

      stockManagerResponseReceiverTestProbe.expectMessage(ResponseForPortFolioValue(20.50 * 10 + 32.5 * 10,
        stockDataPrice, portfolio))
      assert(!testKitSM.isAlive)
    }
  }

  "Fetching time series data Flow" should {
    val httpClientTestProbe = createTestProbe[HttpRequestAndResponse]("httpClientTestProbe")
    val stockManagerResponseReceiverTestProbe = createTestProbe[StockManagerResponse]("SMResponseReceiverRef")
    val testKitSM = BehaviorTestKit(StockManager(httpClientTestProbe.ref), "stockManager")
    val ibmStock = StockModel("IBM", "NASDAQ")
    val praaStock = StockModel("PRAA", "NASDAQ")
    val stockQuantityPrice: Map[StockModel, Int] = Map(praaStock -> 10, ibmStock -> 10)
    val stockDataPrice = Map(praaStock -> 20.5D, ibmStock -> 32.5)
    val portfolio = models.Portfolio(stockQuantityPrice)

    "Checking if child actors are created and then do they receive time series data fetching request and actor is alive" in {
      testKitSM.run(GetTimeSeriesDataOfPortfolio(portfolio, IntraDayTimeWindow, OneMin, stockManagerResponseReceiverTestProbe.ref))
      val testInboxOfPRAA = testKitSM.childInbox[StockRequest]("PRAA")
      val testInboxOfIBM = testKitSM.childInbox[StockRequest]("IBM")

      testInboxOfPRAA.hasMessages shouldBe true
      testInboxOfIBM.hasMessages shouldBe true

      val messageToPRAA = testInboxOfPRAA.receiveMessage
      val messageToIBM = testInboxOfIBM.receiveMessage

      assert(messageToPRAA == GetStocksTimeSeriesData(0, testKitSM.ref, IntraDayTimeWindow, OneMin))
      assert(messageToIBM == GetStocksTimeSeriesData(0, testKitSM.ref, IntraDayTimeWindow, OneMin))
      assert(testKitSM.isAlive)
    }


    //always run this test after running above test or whole spec file together
    "Checking Response after receiving all the stock time series data" in {

      testKitSM.run(UpdateTimeSeriesDataForPortfolio(praaStock, intraDat1MinDataPRAA, IntraDayTimeWindow, OneMin, 0))
      testKitSM.run(UpdateTimeSeriesDataForPortfolio(ibmStock, intraDat1MinDataIBM, IntraDayTimeWindow, OneMin, 0))

      stockManagerResponseReceiverTestProbe
        .expectMessage(ResponseForPortFolioTimeSeriesData(Map("PRAA" -> intraDat1MinDataPRAA, "IBM" -> intraDat1MinDataIBM),
        IntraDayTimeWindow, OneMin))
      assert(!testKitSM.isAlive)
    }
  }


  val intraDat1MinDataIBM = StockDataIntraDay1Min(
    MetaData6(
      "Intraday (1min) open, high, low, close prices and volume",
      "IBM",
      "2023-12-06 19:59:00",
      "1min",
      "Compact",
      "US/Eastern"
    ),
    Map(
      "2023-12-06 19:59:00" -> TimeSeriesData(
        "160.4800",
        "160.4900",
        "160.4800",
        "160.4900",
        "29"
      ),
      "2023-12-06 19:52:00" -> TimeSeriesData(
        "160.0000",
        "160.0000",
        "160.0000",
        "160.0000",
        "2"
      )
    )
  )

  val intraDat1MinDataPRAA = StockDataIntraDay1Min(
    MetaData6(
      "Intraday (1min) open, high, low, close prices and volume",
      "PRAA",
      "2023-12-06 16:20:00",
      "1min",
      "Compact",
      "US/Eastern"
    ),
    Map(
      "2023-12-06 16:20:00" -> TimeSeriesData(
        "20.5800",
        "20.5800",
        "20.5800",
        "20.5800",
        "28"
      ),
      "2023-12-06 16:05:00" -> TimeSeriesData(
        "20.2600",
        "20.2600",
        "20.2600",
        "20.2600",
        "1"
      )
    )
  )

}
