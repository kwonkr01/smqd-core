import sbt._

/**
  * 2018. 5. 29. - Created by Kwon, Yeong Eon
  */
object Dependencies {

  object Versions {
    val scala = "2.12.6"
    val akka = "2.5.13"
    val netty = "4.1.22.Final"
    val alpakka = "0.19"
  }

  val akka: Seq[ModuleID] = Seq(
    //////////////////////////////////
    // akka actor
    "com.typesafe.akka" %% "akka-actor" % Versions.akka,
    //////////////////////////////////
    // akka cluster and distributed data
    "com.typesafe.akka" %% "akka-cluster" % Versions.akka,
    "com.typesafe.akka" %% "akka-distributed-data" % Versions.akka,
    //////////////////////////////////
    // akka http
    "com.typesafe.akka" %% "akka-http"   % "10.1.3",
    //////////////////////////////////
    // akka Stream
    "com.typesafe.akka" %% "akka-stream" % Versions.akka,
    //////////////////////////////////
    // alpakka Stream
    "com.lightbend.akka" %% "akka-stream-alpakka-mqtt" % Versions.alpakka,
    //////////////////////////////////
    // akka Stream
    "io.spray" %% "spray-json" % "1.3.3",
    "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.3",
    //////////////////////////////////
    // Logging
    "com.typesafe.akka" %% "akka-slf4j" % Versions.akka force(),
    "ch.qos.logback" % "logback-classic" % "1.2.3", // 01-Apr-2017 updated
    "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
    //////////////////////////////////
    // Test
    "com.typesafe.akka" %% "akka-testkit" % Versions.akka % Test,
    "org.scalatest" %% "scalatest" % "3.0.5" % Test
  )

  val netty: Seq[ModuleID] = Seq(
    "io.netty" % "netty-buffer" % Versions.netty,
    "io.netty" % "netty-codec-mqtt" % Versions.netty,
    "io.netty" % "netty-codec-http" % Versions.netty,
    "io.netty" % "netty-handler" % Versions.netty,
    "io.netty" % "netty-transport-native-epoll" % Versions.netty, //classifier "linux-x86_64",  // for Linux
    "io.netty" % "netty-transport-native-kqueue" % Versions.netty // classifier "macos-x86_64"  // for macOS
  )

  val crypto: Seq[ModuleID] = Seq(
    "org.bouncycastle" % "bcprov-jdk15on" % "1.56"
  )

  val metricsVersion = "4.1.0-rc2" // updated 04-May-2018
  val metrics: Seq[ModuleID] = Seq(
    "io.dropwizard.metrics" % "metrics-core" % metricsVersion,
    "io.dropwizard.metrics" % "metrics-logback" % metricsVersion,
    "io.dropwizard.metrics" % "metrics-jvm" % metricsVersion
  )
}