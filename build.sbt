ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

libraryDependencies += "org.scala-lang" % "scala-library" % "2.13.10"

val akkaVersion = "2.8.0"
val akkaHttpVersion = "10.2.0"
lazy val circeVersion = "0.14.1"

resolvers += "Akka library repository".at("https://repo.akka.io/maven")

//libraryDependencies +=

//libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion

libraryDependencies ++= Seq(

  "org.scalatest" %% "scalatest" % "3.2.15" % Test,
  //"io.spray" %% "spray-json" % "1.3.6",
  //"org.slf4j" % "slf4j-api" % "2.0.5",
  //"org.slf4j" % "slf4j-nop" % "2.0.5",
  "ch.qos.logback" % "logback-classic" % "1.2.11",
// akka streams
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  // akka http
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,

  //circe
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  //"io.circe" %% "circe-parser" % circeVersion,
  //"de.heikoseeberger" %% "akka-http-circe" % "1.39.2",

  //scala ajl
  "org.scalaj" %% "scalaj-http" % "2.4.2",
  //spark
  "org.apache.spark" %% "spark-core" % "3.2.1",
  "org.apache.spark" %% "spark-sql" % "3.2.1",
// https://mvnrepository.com/artifact/org.apache.spark/spark-mllib
  "org.apache.spark" %% "spark-mllib" % "3.2.1"
)

lazy val root = (project in file("."))
  .settings(
    name := "HedgeFund"
  )
