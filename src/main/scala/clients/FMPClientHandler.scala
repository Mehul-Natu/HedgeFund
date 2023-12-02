package clients
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethod, HttpMethods, HttpProtocols, HttpRequest, HttpResponse, StatusCodes, Uri, headers}
import akka.util.ByteString

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
  case class HttpGetPriceRequest(stockName: String, exchangeName: String, replyTo: ActorRef[HttpRequestAndResponse]) extends HttpRequestAndResponse
  case class HttpGetPriceResponse(success: Boolean, httpEntity: HttpEntity, replyTo: ActorRef[HttpRequestAndResponse]) extends HttpRequestAndResponse
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
              HttpGetPriceResponse(true, entity, replyTo)
            case resp@HttpResponse(code, _, _, _) =>
              resp.discardEntityBytes()
              HttpGetPriceResponse(false, null, replyTo)
          }
          case Failure(exception) => {
            HttpGetPriceResponse(false, null, replyTo)
          }
        }
        Behaviors.same

      case HttpGetPriceResponse(success, entity, replyTo) => {

        val futureEntity = entity.toStrict(10.seconds)
        import system.dispatcher
        val map = futureEntity.map(entity => entity.data.utf8String).onComplete(println)
        println(s"Response Value $success:" + map)
        Behaviors.same
      }

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
