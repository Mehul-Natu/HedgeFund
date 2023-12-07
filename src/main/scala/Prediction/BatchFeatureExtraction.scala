package Prediction

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._

import java.io.{BufferedWriter, File, FileWriter}

// This is to extract the eigenvalues in batches
object BatchFeatureExtraction extends App {

  val spark: SparkSession = SparkSession.builder()
    .appName("StockFeatureExtraction")
    .master("local[*]")
    .getOrCreate()

  import spark.implicits._

  val columnOrder = Seq("Stock_Name", "avg_SMA30", "avg_RSI", "avg_BollingerUpper", "avg_BollingerLower", "avg_VWAP", "avg_ROC")

  def createSparkSession() = SparkSession.builder()
    .appName("StockFeatureExtraction")
    .master("local[*]")
    .getOrCreate()

  // Function to process a single CSV file and return eigenvalues
  // Eigenvalues include: DailyReturn, SMA, RSI, bollinger bands, VWAP, ROC
  // I just analyzed the stock price data ot the nearest 30 days in our dataset
  def processFile(filePath: String): String = {


    println(s"processing for file $filePath")
    val df = spark.read.option("header", "true").option("inferSchema", "true").csv(filePath) //.limit(30)
    val windowSpec = Window.orderBy("timestamp")

    // calculate: DailyReturn
    val dailyReturn = df.withColumn("dailyReturn", (col("Close") - lag("Close", 1).over(windowSpec)) / lag("Close", 1).over(windowSpec))

    // calculate: SMA
    val movingAverage30 = df
      .withColumn("SMA30", avg("Close").over(windowSpec.rowsBetween(-29, 0)))

    // calculate: RSI
    val gain = dailyReturn.withColumn("Gain", when(col("dailyReturn") > 0, col("dailyReturn")).otherwise(0))
    val loss = dailyReturn.withColumn("Loss", when(col("dailyReturn") < 0, -col("dailyReturn")).otherwise(0))
    val avgGain = gain.withColumn("AvgGain", avg("Gain").over(windowSpec.rowsBetween(-29, -1)))
    val avgLoss = loss.withColumn("AvgLoss", avg("Loss").over(windowSpec.rowsBetween(-29, -1)))
    val rs = avgGain.join(avgLoss, "timestamp").withColumn("RS", col("AvgGain") / col("AvgLoss"))
    val rsi = rs.withColumn("RSI", lit(100) - (lit(100) / (col("RS") + 1)))

    // calculate: bollinger bands
    val stdDev = stddev("Close").over(windowSpec.rowsBetween(-29, 0))
    val bollingerUpper = movingAverage30.withColumn("BollingerUpper", col("SMA30") + (stdDev * lit(2)))
    val bollingerLower = movingAverage30.withColumn("BollingerLower", col("SMA30") - (stdDev * lit(2)))

    // calculate VWAP
    val vwap = df.withColumn("VWAP", sum($"Close" * $"Volume").over(windowSpec) / sum("Volume").over(windowSpec))

    // calculate: ROC
    val closeLag = lag("Close", 1).over(windowSpec)
    val roc = df.withColumn("ROC", ($"Close" - closeLag) / closeLag)

    // join all eigenvalues into one DF
    val resultDf = df
      .join(movingAverage30.select("timestamp", "SMA30"), Seq("timestamp"))
      .join(rsi.select("timestamp", "RSI"), Seq("timestamp"))
      .join(bollingerUpper.select("timestamp", "BollingerUpper"), Seq("timestamp"))
      .join(bollingerLower.select("timestamp", "BollingerLower"), Seq("timestamp"))
      .join(vwap.select("timestamp", "VWAP"), Seq("timestamp"))
      .join(roc.select("timestamp", "ROC"), Seq("timestamp"))
      .orderBy("timestamp")

    movingAverage30.unpersist()
    rsi.unpersist()
    bollingerLower.unpersist()
    bollingerUpper.unpersist()
    vwap.unpersist()
    roc.unpersist()

    // get eigenvalues: calculate the avg of every column, then join into a new DF
    //val first30RowsDf = resultDf//.limit(30)

    val result = resultDf.select(
      lit(new File(filePath).getName.replace(".csv", "")).as("Stock_Name"),
      avg("SMA30").as("avg_SMA30"),
      avg("RSI").as("avg_RSI"),
      avg("BollingerUpper").as("avg_BollingerUpper"),
      avg("BollingerLower").as("avg_BollingerLower"),
      avg("VWAP").as("avg_VWAP"),
      avg("ROC").as("avg_ROC")
    )
    resultDf.unpersist()
    result.show()
    //spark.close()
    println(s"processing completed for file $filePath")
    ///result.c
    columnOrder.map(colName => {
      println(colName)
      result.select(colName).first().mkString
    }).mkString(",")
  }


  val parentDirectoryPath = "src/predictionFiles/distributed_data/"
  val parentDirectory = new File(parentDirectoryPath)

  val files = parentDirectory.listFiles().size

  //println(files)
  parentDirectory.listFiles().foreach(file => println(file.getName))
  val allFiles = parentDirectory.listFiles().flatMap(file => file.listFiles(file => file.isFile && file.getName.endsWith(".csv")))


  val onlyTenFiles = allFiles take 10

  allFiles.foreach(file => println(file.getName))
  println("Total files: " + allFiles.size)

  val allEigenvalues: Array[String] = onlyTenFiles.map(file => {
    processFile(file.getPath)
  })

  /*
    val csvStrings = allEigenvalues.map { dataset =>
        columnOrder.map(colName => {
            println(colName)
            dataset.select(colName).first().mkString
        }).mkString(",")
    }

     */
  //println(csvStrings)

  val outputPath = "src/predictionFiles/finalResults/eigenResult/finalFile.csv"

  val writer = new FileWriter(outputPath)
  val buffer = new BufferedWriter(writer)

  try {
    buffer.write(columnOrder.mkString(","))
    buffer.newLine()
    allEigenvalues.foreach(line => {
      buffer.write(line)
      buffer.newLine()
    })
  } finally {
    buffer.close()
    writer.close()
  }


  println("Batch processing complete.")

  spark.stop()
}
