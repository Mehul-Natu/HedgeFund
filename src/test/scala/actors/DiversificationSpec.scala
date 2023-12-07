package actors

import actors.Diversification._
import akka.actor.testkit.typed.CapturedLogEvent
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, ScalaTestWithActorTestKit}
import models.{Portfolio, StockModel}
import org.scalatest.wordspec.AnyWordSpecLike
import org.slf4j.event.Level


class DiversificationSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  val filePath = "src/test/resources/finalClusteredData.csv"

  "Check if stocks are diversified" should {
    val diversificationTestKit = BehaviorTestKit(Diversification(Some(filePath)), "diversifyActorTestKit")
    val diversificationResponseTestProbe = createTestProbe[DiversifyResponse]("diversifyResponseTestProbe")

    val portfolioForDiversifyTrue = Portfolio(Map(StockModel("A", "NASDAQ") -> 10, StockModel("ACN", "NASDAQ") -> 20,
      StockModel("ABBV", "NASDAQ") -> 20, StockModel("ABMD", "NASDAQ") -> 20))
    val portfolioForDiversifyFalse = Portfolio(Map(StockModel("A", "NASDAQ") -> 10, StockModel("ACN", "NASDAQ") -> 20,
      StockModel("ABBV", "NASDAQ") -> 20, StockModel("AAP", "NASDAQ") -> 20))


    "File read check by no effect and log lines" in {
      diversificationTestKit.run(Initialize)
      diversificationTestKit.logEntries()

      assert(!diversificationTestKit.hasEffects())
      diversificationTestKit.logEntries() shouldBe Seq(CapturedLogEvent(Level.INFO, "stock to Cluster: HashMap(AAPL -> 1, ABMD -> 5, ABT -> 0, ABBV -> 5, A -> 0, ACN -> 3, AAP -> 6, ADBE -> 2, AAL -> 4, ADI -> 0) \n cluster to stock: HashMap(0 -> List(A, ABT, ADI), 5 -> List(ABBV, ABMD), 1 -> List(AAPL), 6 -> List(AAP), 2 -> List(ADBE), 3 -> List(ACN), 4 -> List(AAL))"))
    }

    "Checking Diversification case it need diversification" in {
      diversificationTestKit.run(CheckDiversification(portfolioForDiversifyTrue, diversificationResponseTestProbe.ref))
      diversificationResponseTestProbe
        .expectMessage(DiversificationCheckResponse(Some(Map(5 -> List("ABBV", "ABMD"))), ""))

    }

    "Checking Diversification case it does not need diversification" in {
      diversificationTestKit.run(CheckDiversification(portfolioForDiversifyFalse, diversificationResponseTestProbe.ref))
      diversificationResponseTestProbe
        .expectMessage(DiversificationCheckResponse(None, "Your Stocks are diversified"))

    }
  }

  "Diversify Recommendation " should {
    val diversificationTestKit = BehaviorTestKit(Diversification(Some(filePath)), "diversifyActorTestKit")
    val diversificationResponseTestProbe = createTestProbe[DiversifyResponse]("diversifyResponseTestProbe")

    val portfolioForDiversifyRecommendationTrue = Portfolio(Map(StockModel("AAP", "NASDAQ") -> 20, StockModel("ACN", "NASDAQ") -> 20,
      StockModel("ABBV", "NASDAQ") -> 20, StockModel("ABMD", "NASDAQ") -> 20))
    val portfolioForDiversifyRecommendationFalse = Portfolio(Map(StockModel("A", "NASDAQ") -> 10, StockModel("AAPL", "NASDAQ") -> 20,
      StockModel("ADBE", "NASDAQ") -> 20, StockModel("ACN", "NASDAQ") -> 20, StockModel("AAL", "NASDAQ") -> 20,
      StockModel("ABBV", "NASDAQ") -> 20, StockModel("AAP", "NASDAQ") -> 20))

    "File read check by no effect and log lines" in {
      diversificationTestKit.run(Initialize)
      diversificationTestKit.logEntries()

      assert(!diversificationTestKit.hasEffects())
      diversificationTestKit.logEntries() shouldBe Seq(CapturedLogEvent(Level.INFO, "stock to Cluster: HashMap(AAPL -> 1, ABMD -> 5, ABT -> 0, ABBV -> 5, A -> 0, ACN -> 3, AAP -> 6, ADBE -> 2, AAL -> 4, ADI -> 0) \n cluster to stock: HashMap(0 -> List(A, ABT, ADI), 5 -> List(ABBV, ABMD), 1 -> List(AAPL), 6 -> List(AAP), 2 -> List(ADBE), 3 -> List(ACN), 4 -> List(AAL))"))
    }


    "When Recommendation can be given" in {
      diversificationTestKit.run(RecommendStockToDiversify(portfolioForDiversifyRecommendationTrue, diversificationResponseTestProbe.ref))
      diversificationResponseTestProbe
        .expectMessage(DiversificationRecommendationResponse(Some(Map(0 -> List("A", "ABT", "ADI"), 4 -> List("AAL"),
          1 -> List("AAPL"), 2 -> List("ADBE"))), ""))

    }

    "No need for recommendation stocks are diversified" in {
      diversificationTestKit.run(RecommendStockToDiversify(portfolioForDiversifyRecommendationFalse, diversificationResponseTestProbe.ref))
      diversificationResponseTestProbe
        .expectMessage(DiversificationRecommendationResponse(None, "No Stocks Recommendation your stocks are good"))

    }
  }


}
