package actors

import Prediction.Constants.PATH_TO_STOCK_TO_CLUSTER
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import models.Portfolio

import scala.collection.MapView
import scala.io.Source

object Diversification {

  val filePath = PATH_TO_STOCK_TO_CLUSTER

  //todo to add stock name to the cluster not just symbol
  trait DiversifyRequest
  case object Initialize extends DiversifyRequest
  case class CheckDiversification(portfolio: Portfolio, replyTo: ActorRef[DiversifyResponse]) extends DiversifyRequest
  case class RecommendStockToDiversify(portfolio: Portfolio, replyTo: ActorRef[DiversifyResponse]) extends DiversifyRequest

  trait DiversifyResponse
  case class DiversificationCheckResponse(clusteredStockMap: Option[Map[Int, List[String]]], message: String) extends DiversifyResponse
  case class DiversificationRecommendationResponse(clusteredStockMap: Option[Map[Int, List[String]]], message: String) extends DiversifyResponse

  def apply(clusterPath: Option[String]): Behavior[DiversifyRequest] = initialize(clusterPath)

  def initialize(clusterPath: Option[String]): Behavior[DiversifyRequest] = Behaviors.receive{
    (context, message) =>
      message match {
        case Initialize =>
          val source = Source.fromFile(clusterPath.getOrElse(filePath))
          val lines: List[String] = source.getLines().toList
          val removedHeaderLines = lines.slice(1, lines.size)

          val stockToCluster = removedHeaderLines.map(string => {
            val splitString = string.split(",")
            (splitString(0) -> splitString(1).toInt)
          }).toMap

          val clusterToStock = removedHeaderLines.map(string => {
            val splitString = string.split(",")
            (splitString(0) -> splitString(1).toInt)
          }).groupBy(_._2).view.mapValues(_.map(_._1)).toMap

          source.close()

          context.log.info(s"stock to Cluster: $stockToCluster \n cluster to stock: $clusterToStock")
          canReceiveRequest(stockToCluster, clusterToStock)
      }
  }
  //todo to add stock name
  def canReceiveRequest(stockToCluster: Map[String, Int], clusterToStock: Map[Int, List[String]]): Behavior[DiversifyRequest] =
    Behaviors.receive {
      (context, message) =>
        message match {
          case CheckDiversification(portfolio, replyTo) =>
            val stockAndTheirCluster = portfolio.stockQuantity.filter(stock => stockToCluster.contains(stock._1.symbol))
              .map(stock => (stock._1.symbol, stockToCluster(stock._1.symbol)))
            if (stockAndTheirCluster.isEmpty) {
              replyTo ! DiversificationCheckResponse(None, "Your Stocks are diversified")
              canReceiveRequest(stockToCluster, clusterToStock)
            } else {
              val clusterToStockListOfUser: MapView[Int, List[String]] =
                stockAndTheirCluster.groupBy(_._2).view.mapValues(_.keys.toList)
              val stocksInSameCluster = clusterToStockListOfUser.filter(values => values._2.size > 1)
              if (stocksInSameCluster.isEmpty) {
                replyTo ! DiversificationCheckResponse(None, "Your Stocks are diversified")
                canReceiveRequest(stockToCluster, clusterToStock)
              } else {
                replyTo ! DiversificationCheckResponse(Some(stocksInSameCluster.toMap), "")
                canReceiveRequest(stockToCluster, clusterToStock)
              }
            }

          case RecommendStockToDiversify(portfolio, replyTo) =>
            val stockClustersUsedByUser = portfolio.stockQuantity.filter(stock => stockToCluster.contains(stock._1.symbol))
              .map(stock => (stock._1, stockToCluster(stock._1.symbol))).groupBy(_._2).keys.toSet
            val clusterInWhichUserHasNotInvested: Set[Int] = clusterToStock.keys.toSet -- stockClustersUsedByUser
            if (clusterInWhichUserHasNotInvested.isEmpty) {
              replyTo ! DiversificationRecommendationResponse(None, "No Stocks Recommendation your stocks are good")
              canReceiveRequest(stockToCluster, clusterToStock)
            } else {
              val stockToRecommend: Map[Int, List[String]] =
                clusterInWhichUserHasNotInvested.map(cluster => (cluster -> clusterToStock(cluster))).toMap
              replyTo ! DiversificationRecommendationResponse(Some(stockToRecommend), "")
              canReceiveRequest(stockToCluster, clusterToStock)
            }
        }
    }

  def main(args: Array[String]): Unit = {
    val filePath = PATH_TO_STOCK_TO_CLUSTER
    val source = Source.fromFile(filePath)
    val lines: List[String] = source.getLines().toList
    val removedHeaderLines = lines.slice(1, lines.size)

    val stockToCluster = removedHeaderLines.map(string => {
      val splitString = string.split(",")
      (splitString(0) -> splitString(1).toInt)
    }).toMap

    val clusterToStock = removedHeaderLines.map(string => {
      val splitString = string.split(",")
      (splitString(0) -> splitString(1).toInt)
    }).groupBy(_._2).view.mapValues(_.map(_._1)).toMap
    //todo test this
    println("hello")
  }
}
