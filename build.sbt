version := "0.1-SNAPSHOT"
organization := "org"
description := "stream-exercise"
scalaVersion := "2.13.12"
isSnapshot := false
scalacOptions := Seq("-unchecked", "-deprecation", "-Xfatal-warnings")
Test / fork := true
Test / parallelExecution := false

val akkaVersion      = "2.6.20"
val fs2Version       = "3.7.0"
val scalaTestVersion = "3.2.17"
val mockitoVersion   = "1.17.27"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream"         % akkaVersion,
  "co.fs2"            %% "fs2-core"            % fs2Version,
  "org.scalatest"     %% "scalatest"           % "3.2.13" % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "org.scalatest"     %% "scalatest"           % scalaTestVersion % Test,
  "org.mockito"       %% "mockito-scala"       % mockitoVersion % Test,
  "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test
)
