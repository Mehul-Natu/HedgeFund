package Prediction

import java.io.File
import java.nio.file.{Files, Paths, StandardCopyOption}
import scala.io.Source

object FileDistributor extends App {
  val sourceDir = new File("src/predictionFiles/fetched_data")
  val destDirBasePath = "src/predictionFiles/distributed_data"
  val maxFilesPerFolder = 20
  val constituentFile = Source.fromFile("src/main/resources/constituents.csv")
  val totalFilesThereShouldBe = constituentFile.getLines().drop(1).size

  if (!sourceDir.exists() || !sourceDir.isDirectory) {
    println("Source directory does not exist or is not a directory.")
  } else {
    val files = sourceDir.listFiles().filter(file => file.isFile && file.getName.endsWith(".csv"))

    if (files.length != totalFilesThereShouldBe) {
      println(s"Expected $totalFilesThereShouldBe files, but found ${files.length} files.")
    } else {
      // Calculate the number of folders needed
      println(s"Expected files $totalFilesThereShouldBe files, found ${files.length} files.")
      val numFolders = (totalFilesThereShouldBe + maxFilesPerFolder - 1) / maxFilesPerFolder

      for (i <- 0 until numFolders) {
        val destDir = new File(s"$destDirBasePath/folder${i + 1}")
        if (!destDir.exists()) destDir.mkdirs()

        // Calculate start and end indices for file slicing
        val startIndex = i * maxFilesPerFolder
        val endIndex = Math.min(startIndex + maxFilesPerFolder, totalFilesThereShouldBe)

        files.slice(startIndex, endIndex).foreach { file =>
          try {
            Files.move(
              Paths.get(file.getAbsolutePath),
              Paths.get(destDir.getAbsolutePath, file.getName),
              StandardCopyOption.REPLACE_EXISTING
            )
          } catch {
            case e: Exception => println(s"Failed to move file: ${file.getName}. Error: ${e.getMessage}")
          }
        }
      }
      println("Files have been distributed.")
    }
  }
}
