package actors

import actors.Stock.{DailyTimeWindow, EmptyWindowSplit, FetchedStockPriceWithId, FifteenMin, IntraDayTimeWindow, OneMin, SixtyMin}
import actors.StockManager.{ResponseForPortFolioTimeSeriesData, StockManagerRequest, UpdateStockPriceForPortfolio, UpdateTimeSeriesDataForPortfolio}
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, ScalaTestWithActorTestKit, TestProbe}
import clients.AVClientHandler.{HttpGetPriceRequest, HttpGetTimeSeries, HttpGetTimeSeriesForIntraDay, HttpRequestAndResponse}
import models.{MetaData6, MetaDataDaily, StockDataDaily, StockDataIntraDay15Min, StockDataIntraDay1Min, StockDataIntraDay60Min, StockModel, TimeSeriesData}
import org.scalatest.wordspec.AnyWordSpecLike

class StockSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  //all the tests which are together should be run together that also helps checking The flow

  "For just price" should {
    val httpClientTestProbe = createTestProbe[HttpRequestAndResponse]("httpClientTestProbe")
    val stockManagerTestProbe = createTestProbe[StockManagerRequest]("stockManagerTestProbe")
    val testKit = BehaviorTestKit(Stock("IBM", "NASDQ", httpClientTestProbe.ref), "stock")
    val stockIBM = StockModel("IBM", "NASDQ")

    "stock price fetching request and message being sent to httpClientRef" in {
      testKit.run(Stock.GetStockPriceForPortfolio(0, stockManagerTestProbe.ref))
      httpClientTestProbe.expectMessage(HttpGetPriceRequest("IBM", "NASDQ", testKit.ref, 0))
    }

    "Response from clientRef request and message being sent to stock manager" in {
      testKit.run(Stock.FetchedStockPriceWithId(100, 0))
      stockManagerTestProbe.expectMessage(UpdateStockPriceForPortfolio(stockIBM, 100, 0))
    }


  }

  "time series daily" should {
    val httpClientTestProbe = createTestProbe[HttpRequestAndResponse]("httpClientTestProbe")
    val stockManagerTestProbe = createTestProbe[StockManagerRequest]("stockManagerTestProbe")
    val testKit = BehaviorTestKit(Stock("IBM", "NASDQ", httpClientTestProbe.ref), "stock")
    val stockIBM = StockModel("IBM", "NASDQ")

    "stock series data Daily fetching request and message being sent to httpClientRef" in {
      testKit.run(Stock.GetStocksTimeSeriesData(0, stockManagerTestProbe.ref, DailyTimeWindow, EmptyWindowSplit))
      httpClientTestProbe.expectMessage(HttpGetTimeSeries("IBM", "NASDQ", testKit.ref, DailyTimeWindow, 0))
    }

    "Response from clientRef and message being sent to stock manager" in {
      testKit.run(Stock.FetchedStockTimeSeriesData(dailyTimeSeriesData, DailyTimeWindow, EmptyWindowSplit, 0))
      stockManagerTestProbe.expectMessage(UpdateTimeSeriesDataForPortfolio(stockIBM, dailyTimeSeriesData, DailyTimeWindow,
        EmptyWindowSplit, 0))
    }
  }

  "time series Intra day" should {
    val httpClientTestProbe = createTestProbe[HttpRequestAndResponse]("httpClientTestProbe")
    val stockManagerTestProbe = createTestProbe[StockManagerRequest]("stockManagerTestProbe")
    val testKit = BehaviorTestKit(Stock("IBM", "NASDQ", httpClientTestProbe.ref), "stock")
    val stockIBM = StockModel("IBM", "NASDQ")

    "(1min) stock series data Inta-Day time split fetching" in {
      testKit.run(Stock.GetStocksTimeSeriesData(0, stockManagerTestProbe.ref, IntraDayTimeWindow, OneMin))
      httpClientTestProbe.expectMessage(HttpGetTimeSeriesForIntraDay("IBM", "NASDQ", testKit.ref, OneMin, 0))
    }

    "(1min) Response from clientRef and message being sent to stock manager for 1min split" in {
      testKit.run(Stock.FetchedStockTimeSeriesData(intraDat1MinData, IntraDayTimeWindow, OneMin, 0))
      stockManagerTestProbe.expectMessage(UpdateTimeSeriesDataForPortfolio(stockIBM, intraDat1MinData, IntraDayTimeWindow,
        OneMin, 0))
    }

    "(15min) stock series data Inta-Day time split fetching" in {
      testKit.run(Stock.GetStocksTimeSeriesData(0, stockManagerTestProbe.ref, IntraDayTimeWindow, FifteenMin))
      httpClientTestProbe.expectMessage(HttpGetTimeSeriesForIntraDay("IBM", "NASDQ", testKit.ref, FifteenMin, 0))
    }

    "(15min) Response from clientRef and message being sent to stock manager for 1min split" in {
      testKit.run(Stock.FetchedStockTimeSeriesData(intraDat15MinData, IntraDayTimeWindow, FifteenMin, 0))
      stockManagerTestProbe.expectMessage(UpdateTimeSeriesDataForPortfolio(stockIBM, intraDat15MinData, IntraDayTimeWindow,
        FifteenMin, 0))
    }

    "(60min) stock series data Inta-Day time split fetching" in {
      testKit.run(Stock.GetStocksTimeSeriesData(0, stockManagerTestProbe.ref, IntraDayTimeWindow, SixtyMin))
      httpClientTestProbe.expectMessage(HttpGetTimeSeriesForIntraDay("IBM", "NASDQ", testKit.ref, SixtyMin, 0))
    }

    "(60min) Response from clientRef and message being sent to stock manager for 1min split" in {
      testKit.run(Stock.FetchedStockTimeSeriesData(intraDat15MinData, IntraDayTimeWindow, SixtyMin, 0))
      stockManagerTestProbe.expectMessage(UpdateTimeSeriesDataForPortfolio(stockIBM, intraDat15MinData, IntraDayTimeWindow,
        SixtyMin, 0))
    }
  }


  val dailyTimeSeriesData = StockDataDaily(
    MetaDataDaily(
      "Daily Prices (open, high, low, close) and Volumes",
      "IBM",
      "2023-12-06",
      "Compact",
      "US/Eastern"
    ),
    Map(
      "2023-12-06" -> TimeSeriesData(
        "161.5900",
        "162.3550",
        "160.0100",
        "160.2800",
        "3356432"
      ),
      "2023-12-05" -> TimeSeriesData(
        "160.7600",
        "162.4700",
        "160.7200",
        "161.3900",
        "4556668"
      )
    )
  )

  val intraDat1MinData = StockDataIntraDay1Min(
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

  val intraDat15MinData = StockDataIntraDay15Min(
    MetaData6(
      "Intraday (15min) open, high, low, close prices and volume",
      "IBM",
      "2023-12-06 19:45:00",
      "15min",
      "Compact",
      "US/Eastern"
    ),
    Map(
      "2023-12-06 19:45:00" -> TimeSeriesData(
        "160.2000",
        "160.5000",
        "160.0000",
        "160.4900",
        "66"
      ),
      "2023-12-06 19:30:00" -> TimeSeriesData(
        "160.4700",
        "160.4800",
        "160.3500",
        "160.3500",
        "25"
      )
    )
  )

  val intraDat60MinData = StockDataIntraDay60Min(
    MetaData6(
      "Intraday (60min) open, high, low, close prices and volume",
      "IBM",
      "2023-12-06 19:00:00",
      "60min",
      "Compact",
      "US/Eastern"
    ),
    Map(
      "2023-12-06 19:00:00" -> TimeSeriesData(
        "160.2800",
        "160.5000",
        "160.0000",
        "160.4900",
        "501201"
      ),
      "2023-12-06 18:00:00" -> TimeSeriesData(
        "160.2800",
        "160.4900",
        "160.2000",
        "160.2000",
        "501336"
      )
    )
  )

}
