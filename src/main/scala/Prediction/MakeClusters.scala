package Prediction

import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.sql.SparkSession
import Prediction.Constants._

import java.io.{BufferedWriter, FileWriter}

object MakeClusters extends App {
  val spark: SparkSession = SparkSession.builder()
    .appName("StockClustering")
    .master("local[*]")
    .getOrCreate()

  // Load the data
  val filePath = "src/predictionFiles/finalResults/eigenResult/finalFile.csv"
  val data = spark.read.option("header", "true").option("inferSchema", "true").csv(filePath)

  // Assemble the features into a feature vector
  val assembler = new VectorAssembler()
    .setInputCols(Array("avg_SMA30", "avg_RSI", "avg_BollingerUpper", "avg_BollingerLower", "avg_VWAP", "avg_ROC"))
    .setOutputCol("features")

  val featureData = assembler.transform(data)


  // Apply K-means clustering
  val kmeans = new KMeans().setK(7).setSeed(12345L).setFeaturesCol("features").setPredictionCol("Cluster")
  val model = kmeans.fit(featureData)

  // Make predictions
  val predictions = model.transform(featureData)

  // Select relevant columns to display
  // just for showing the result
  //    val result = predictions.select("Stock_Name", "Cluster")

  // Select the required columns
  val finalResult = predictions.select("Stock_Name", "avg_SMA30", "avg_RSI", "avg_BollingerUpper", "avg_BollingerLower", "avg_VWAP", "avg_ROC", "Cluster")

  // Specify the path where you want to save the CSV file
  val outputPath = PATH_TO_STOCK_TO_CLUSTER


  // Save the DataFrame to a CSV file

  val rows = finalResult.collect()
  val listOfStockAndCluster = rows.map(row => {
    row.getString(0) + "," + row.getAs[String](7)
  })

  val writer = new FileWriter(outputPath)
  val buffer = new BufferedWriter(writer)

  try {
    buffer.write("StockName,ClusterNumber")
    buffer.newLine()
    listOfStockAndCluster.foreach(line => {
      buffer.write(line)
      buffer.newLine()
    })
  } finally {
    buffer.close()
    writer.close()
  }
  // Stop the SparkSession when done
  spark.stop()


  // Function to find the optimal K based on the WCSS values
  def findOptimalK(wcssValues: Array[(Int, Double)]): Int = {
    val wcssDifferences = wcssValues.sliding(2).map { case Array(prev, current) =>
      prev._2 - current._2
    }.toArray

    // Find the index where the reduction in WCSS starts to slow down
    val optimalIndex = wcssDifferences.indexOf(wcssDifferences.min) + 1
    wcssValues(optimalIndex)._1
  }
}
