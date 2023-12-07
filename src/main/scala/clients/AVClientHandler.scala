package clients
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethod, HttpMethods, HttpProtocols, HttpRequest, HttpResponse, StatusCodes, Uri, headers}
import actors.Stock.{DailyTimeWindow, EmptyWindowSplit, FetchedStockPriceWithId, FetchedStockTimeSeriesData, FifteenMin, HttpFailureRequestFromHttpClient, IntraDayTimeWindow, IntraDayWindowSplit, OneMin, SixtyMin, StockRequest, TimeWindow, UpdateStockPrice}
import models.{StockDataDaily, StockDataIntraDay15Min, StockDataIntraDay1Min, StockDataIntraDay60Min}

import scala.concurrent.duration.DurationInt
import scala.io.Source.fromURL
import scala.util.{Failure, Success}


object AVClientHandler {


  trait HttpRequestAndResponse
  case class HttpGetPriceRequest(stockName: String, exchangeName: String, replyTo: ActorRef[StockRequest], id: Integer) extends HttpRequestAndResponse
  case class HttpGetPriceSuccessResponse(httpEntity: HttpEntity, replyTo: ActorRef[StockRequest], id: Integer) extends HttpRequestAndResponse
  case class HttpFailureResponse(message: String, replyTo: ActorRef[StockRequest], id: Integer) extends HttpRequestAndResponse
  case class EntityGetPriceSuccessResponse(entityString: String, replyTo: ActorRef[StockRequest], id: Integer) extends HttpRequestAndResponse
  case class EntityFailureResponse(ExceptionMessage: String, replyTo: ActorRef[StockRequest], id: Integer) extends HttpRequestAndResponse

  case class HttpGetTimeSeriesForIntraDay(stockName: String, exchangeName: String, replyTo: ActorRef[StockRequest],
                                          intraDayWindowSplit: IntraDayWindowSplit,
                                          id: Integer) extends HttpRequestAndResponse

  case class HttpGetTimeSeries(stockName: String, exchangeName: String, replyTo: ActorRef[StockRequest],
                               timeWindow: TimeWindow,
                               id: Integer) extends HttpRequestAndResponse


  case class HttpGetTimeSeriesSuccessResponse(httpEntity: HttpEntity, replyTo: ActorRef[StockRequest], id: Integer,
                                              timeWindow: TimeWindow, intraDayWindowSplit: IntraDayWindowSplit) extends HttpRequestAndResponse

  case class EntityGetTimeSeriesSuccessResponse(entityString: String, replyTo: ActorRef[StockRequest],
                                                id: Integer, timeWindow: TimeWindow,
                                                intraDayWindowSplit: IntraDayWindowSplit) extends HttpRequestAndResponse


  //case class HttpFailureResponse(message: String, replyTo: ActorRef[StockRequest], id: Integer) extends HttpRequestAndResponse


  import HttpClient._

  def apply(): Behavior[HttpRequestAndResponse] = Behaviors.setup(context => {

    val apiKeyFMP = "GQYI8P0BU34P6SJH"

    Behaviors.receiveMessage {


      case HttpGetPriceRequest(stockName, exchangeName, replyTo, id) =>
        context.log.info(s"Recived Reuqet to get price for $stockName ann id: $id")

        val request = getPriceRequest(stockName, exchangeName, apiKeyFMP)
        val responseFuture = Http().singleRequest(request)

        context.pipeToSelf(responseFuture) {
          case Success(httpResponse) => httpResponse match {
            case HttpResponse(StatusCodes.OK, headers, entity, _) =>
              println("headers" + headers)
              HttpGetPriceSuccessResponse(entity, replyTo, id)
            case resp@HttpResponse(code, _, _, _) =>
              resp.discardEntityBytes()
              HttpFailureResponse(code.defaultMessage(), replyTo, id)
          }
          case Failure(exception) =>
            HttpFailureResponse(exception.getMessage, replyTo, id)

        }
        Behaviors.same

      case HttpGetTimeSeriesForIntraDay(stockName, exchangeName, replyTo, intraDayWindowSplit, id) =>
        context.log.info(s"Recived Reuqet to get time series for intra day $intraDayWindowSplit " +
          s" for $stockName ann id: $id")

        val request = getIntraDayTimeSeriesData(stockName, exchangeName, apiKeyFMP, intraDayWindowSplit)
        val responseFuture = Http().singleRequest(request)

        context.pipeToSelf(responseFuture) {
          case Success(httpResponse) => httpResponse match {
            case HttpResponse(StatusCodes.OK, headers, entity, _) =>
              println("headers" + headers)
              HttpGetTimeSeriesSuccessResponse(entity, replyTo, id, IntraDayTimeWindow, intraDayWindowSplit)
            case resp@HttpResponse(code, _, _, _) =>
              resp.discardEntityBytes()
              HttpFailureResponse(code.defaultMessage(), replyTo, id)
          }
          case Failure(exception) =>
            HttpFailureResponse(exception.getMessage, replyTo, id)

        }
        Behaviors.same

      case HttpGetTimeSeries(stockName, exchangeName, replyTo, timeWindow, id) =>
        context.log.info(s"Received Request to get time series for non Intra $timeWindow " +
          s" for $stockName ann id: $id")

        val request = getNonIntraDayTimeSeriesData(stockName, exchangeName, apiKeyFMP, timeWindow)
        val responseFuture = Http().singleRequest(request)

        context.pipeToSelf(responseFuture) {
          case Success(httpResponse) => httpResponse match {
            case HttpResponse(StatusCodes.OK, headers, entity, _) =>
              //println("headers" + headers)
              HttpGetTimeSeriesSuccessResponse(entity, replyTo, id, timeWindow, EmptyWindowSplit)
            case resp@HttpResponse(code, _, _, _) =>
              resp.discardEntityBytes()
              HttpFailureResponse(code.defaultMessage(), replyTo, id)
          }
          case Failure(exception) =>
            HttpFailureResponse(exception.getMessage, replyTo, id)

        }
        Behaviors.same

      case HttpGetPriceSuccessResponse(entity, replyTo, id) =>
        val futureEntity = entity.toStrict(10.seconds)
        //import system.dispatcher
        //val map = futureEntity//.map(entity => entity.data.utf8String)
        context.pipeToSelf(futureEntity) {
          case Success(httpEntity) =>
            EntityGetPriceSuccessResponse(httpEntity.data.utf8String, replyTo, id)
          case Failure(exception) =>
            EntityFailureResponse(exception.getMessage, replyTo, id)
        }
        //println(s"Response Value $success:" + map)
        Behaviors.same


      case HttpGetTimeSeriesSuccessResponse(entity, replyTo, id, timeWindow, intraDayWindowSplit) =>
        val futureEntity = entity.toStrict(10.seconds)
        //import system.dispatcher
        //val map = futureEntity//.map(entity => entity.data.utf8String)
        context.pipeToSelf(futureEntity) {
          case Success(httpEntity) =>
            EntityGetTimeSeriesSuccessResponse(httpEntity.data.utf8String, replyTo, id, timeWindow, intraDayWindowSplit)
          case Failure(exception) =>
            EntityFailureResponse(exception.getMessage, replyTo, id)
        }
        //println(s"Response Value $success:" + map)
        Behaviors.same

      case EntityGetPriceSuccessResponse(entityString, replyTo, id) =>
        context.log.info(s"Received Entity Success for id: $id")
        import models.StockResponseJsonProtocol._

        import spray.json._
        val stockData = entityString.parseJson.convertTo[StockDataIntraDay1Min]
        val price = stockData.`Time Series (1min)`(stockData.`Meta Data`.`3. Last Refreshed`).`4. close`
        println("In Here: " + stockData.`Time Series (1min)`.get(stockData.`Meta Data`.`3. Last Refreshed`))
        replyTo ! FetchedStockPriceWithId(price.toDouble, id)
        Behaviors.same

      case EntityGetTimeSeriesSuccessResponse(entityString, replyTo, id, timeWindow, intraDayWindowSplit) =>
        context.log.info(s"Time Series Data time window [$timeWindow] = [$entityString]")
        //context.log.info(s"response = $entityString")
        import models.StockResponseJsonProtocol._
        import spray.json._
        timeWindow match {
          case IntraDayTimeWindow =>
            intraDayWindowSplit match {
              case OneMin =>
                val stockData = entityString.parseJson.convertTo[StockDataIntraDay1Min]
                replyTo ! FetchedStockTimeSeriesData(stockData, timeWindow, intraDayWindowSplit, id)
              case FifteenMin =>
                val stockData = entityString.parseJson.convertTo[StockDataIntraDay15Min]
                replyTo ! FetchedStockTimeSeriesData(stockData, timeWindow, intraDayWindowSplit, id)
              case SixtyMin => val stockData = entityString.parseJson.convertTo[StockDataIntraDay60Min]
                replyTo ! FetchedStockTimeSeriesData(stockData, timeWindow, intraDayWindowSplit, id)
            }
            Behaviors.same
          case DailyTimeWindow =>
            val stockData = entityString.parseJson.convertTo[StockDataDaily]
            replyTo ! FetchedStockTimeSeriesData(stockData, timeWindow, intraDayWindowSplit, id)
            Behaviors.same
        }


      case EntityFailureResponse(exceptionMessage, replyTo, id) =>
        context.log.error("Error while fetching data from downstream client Alfa Vantage" + exceptionMessage)
        //TODO Mehul complete this
        replyTo ! HttpFailureRequestFromHttpClient("Sorry DownStream Error", exceptionMessage, id)
        Behaviors.same
    }

    }
  )

  def getPriceRequest(stock: String, exchangeName: String, apiKey: String): HttpRequest = {
    val request = Get(f"https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&outputsize=compact&symbol=$stock&interval=1min&apikey=$apiKey")
    //println(request)
    request
  }

  def getIntraDayTimeSeriesData(stock: String, exchangeName: String, apiKey: String, intraDayWindowSplit: IntraDayWindowSplit): HttpRequest = {
    val request = Get(f"https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&outputsize=compact&symbol=$stock&interval=${intraDayWindowSplit.getString}&apikey=$apiKey")
    //println(request)
    request
  }

  def getNonIntraDayTimeSeriesData(stock: String, exchangeName: String, apiKey: String, timeWindow: TimeWindow): HttpRequest = {
    val request = Get(f"https://www.alphavantage.co/query?function=${timeWindow.getString}&symbol=$stock&apikey=$apiKey")
    //println(request)
    request
  }


  def main(args: Array[String]): Unit = {
    val actor = ActorSystem(AVClientHandler(), "actor")
    //actor ! HttpGetPriceRequest("PRAA", "NASDAQ", null, 1)
    //actor ! HttpGetTimeSeriesForIntraDay("PRAA", "NASDAQ", null, SixtyMin, 1)
    actor ! HttpGetTimeSeries("PRAA", "NASDAQ", null, DailyTimeWindow, 1)
    //val FMPClientHandler = new FMPClientHandler("AlGvLMZ6dIlU7SzF1BnvIGV27et3CYjG")
    //FMPClientHandler.getFullQuote("AAPL")
  }


}
