ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

libraryDependencies += "org.scala-lang" % "scala-library" % "2.13.10"

val akkaVersion = "2.8.0"
val akkaHttpVersion = "10.5.0"

libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.8.0"


libraryDependencies ++= Seq(
  // akka streams
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  // akka http
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
)

lazy val root = (project in file("."))
  .settings(
    name := "HedgeFund"
  )
