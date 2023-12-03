package clients
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethod, HttpMethods, HttpProtocols, HttpRequest, HttpResponse, StatusCodes, Uri, headers}
import akka.util.ByteString
import stocks.Stock.StockRequestResponse

import scala.concurrent.duration.DurationInt
import scala.io.Source.fromURL
import scala.util.{Failure, Success}


class FMPClientHandler(val apiKey: String) {

  def getFullQuote(name: String): String = {
    val json = fromURL(s"https://financialmodelingprep.com/api/v3/quote/${name}?apikey=${apiKey}").mkString
    println(json)
    json
  }


}

object FMPClientHandler {

  trait HttpRequestAndResponse
  case class HttpGetPriceRequest(stockName: String, exchangeName: String, replyTo: ActorRef[StockRequestResponse]) extends HttpRequestAndResponse
  case class HttpGetPriceSuccessResponse(httpEntity: HttpEntity, replyTo: ActorRef[StockRequestResponse]) extends HttpRequestAndResponse
  case class HttpGetPriceFailureResponse(message: String, replyTo: ActorRef[StockRequestResponse]) extends HttpRequestAndResponse
  case class EntityGetPriceSuccessResponse(entityString: String, replyTo: ActorRef[StockRequestResponse]) extends HttpRequestAndResponse
  case class EntityGetPriceFailureResponse(ExceptionMessage: String, replyTo: ActorRef[StockRequestResponse]) extends HttpRequestAndResponse


  import HttpClient._

  def apply(): Behavior[HttpRequestAndResponse] = Behaviors.setup(context => {

    val apiKeyFMP = ""

    Behaviors.receiveMessage {
      case HttpGetPriceRequest(stockName, exchangeName, replyTo) =>
        val request = getPriceRequest(stockName, exchangeName, apiKeyFMP)
        val responseFuture = Http().singleRequest(request)

        context.pipeToSelf(responseFuture) {
          case Success(httpResponse) => httpResponse match {
            case HttpResponse(StatusCodes.OK, headers, entity, _) =>
              println("headers" + headers)
              HttpGetPriceSuccessResponse(entity, replyTo)
            case resp@HttpResponse(code, _, _, _) =>
              resp.discardEntityBytes()
              HttpGetPriceFailureResponse(code.defaultMessage(), replyTo)
          }
          case Failure(exception) =>
            HttpGetPriceFailureResponse(exception.getMessage, replyTo)

        }
        Behaviors.same

      case HttpGetPriceSuccessResponse(entity, replyTo) => {
        val futureEntity = entity.toStrict(10.seconds)
        import system.dispatcher
        val map = futureEntity//.map(entity => entity.data.utf8String)
        context.pipeToSelf(futureEntity) {
          case Success(httpEntity) =>
            EntityGetPriceSuccessResponse(httpEntity.data.utf8String, replyTo)
          case Failure(exception) =>
            EntityGetPriceSuccessResponse(exception.getMessage, replyTo)
        }
        //println(s"Response Value $success:" + map)
        Behaviors.same
      }
      case EntityGetPriceSuccessResponse(entityString, replyTo) =>
        println("In Here: " + entityString)
        Behaviors.same
      case EntityGetPriceFailureResponse(exceptionMessage, replyTo) =>
        println("Error" + exceptionMessage)
        Behaviors.same
    }

    }
  )


  def sendRequest(request: HttpRequest) = {
    //Http().
  }


  def getPriceRequest(stock: String, exchangeName: String, apiKey: String): HttpRequest = {
    val uri = s"https://financialmodelingprep.com/api/v3/search-ticker?query=${stock}&limit=10&exchange=${exchangeName}?apikey=$apiKey"
    val uri2 = s"https://financialmodelingprep.com/api/v3/quote/${stock}?apikey=${apiKey}"
    val uriWithoutAPiKey = s"https://financialmodelingprep.com/api/v3/search-ticker?query=${stock}&limit=10&exchange=${exchangeName}"
     HttpRequest(
      method = HttpMethods.GET,
      uri = uri,
       protocol = HttpProtocols.`HTTP/2.0`,
      //headers = List(headers.RawHeader("Authorization", apiKey)),
    )
    //
    //println(uri2)


    val request = Get("https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=IBM&apikey=demo")
    println(request)
    request
  }


  //AAPL
  //val FMPClientHandler = new FMPClientHandler("AlGvLMZ6dIlU7SzF1BnvIGV27et3CYjG")
  //FMPClientHandler.getFullQuote("AAPL")

  def main(args: Array[String]): Unit = {
    val actor = ActorSystem(FMPClientHandler(), "actor")
    actor ! HttpGetPriceRequest("PRAA", "NASDAQ", null)

    //val FMPClientHandler = new FMPClientHandler("AlGvLMZ6dIlU7SzF1BnvIGV27et3CYjG")
    //FMPClientHandler.getFullQuote("AAPL")
  }


}
