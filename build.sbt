ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.13"

val zioVersion = "2.0.21"
val sparkVersion = "3.5.0"
val playVersion = "3.0.1"
val zioConfigVersion = "4.0.1"
val kafkaVersion = "3.6.1"
val pekkoVersion = "1.0.2"
val sttpVersion = "3.9.3"

// Dependency conflict resolution
ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "always"
)

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "real-estate-demo",
    
    // Play routes configuration
    routesGenerator := InjectedRoutesGenerator,
    
    libraryDependencies ++= Seq(
      // ZIO
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-streams" % zioVersion,
      "dev.zio" %% "zio-config" % zioConfigVersion,
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
      "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
      "dev.zio" %% "zio-interop-reactivestreams" % "2.0.2",
      
      // Kafka
      "dev.zio" %% "zio-kafka" % "2.7.4",
      "org.apache.kafka" % "kafka-clients" % kafkaVersion,
      "org.apache.kafka" % "kafka-streams" % kafkaVersion,
      "org.apache.kafka" %% "kafka-streams-scala" % kafkaVersion,
      
      // Spark
      "org.apache.spark" %% "spark-core" % sparkVersion,
      "org.apache.spark" %% "spark-sql" % sparkVersion,
      "org.apache.spark" %% "spark-mllib" % sparkVersion,
      "org.apache.spark" %% "spark-streaming" % sparkVersion,
      "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion,
      
      // Play Framework
      guice,
      "org.playframework" %% "play" % playVersion,
      
      // Pekko
      "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
      "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
      
      // HTTP Client for Zillow API
      "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
      "com.softwaremill.sttp.client3" %% "zio" % sttpVersion,
      "com.softwaremill.sttp.client3" %% "circe" % sttpVersion,
      
      // JSON handling
      "io.circe" %% "circe-core" % "0.14.6",
      "io.circe" %% "circe-generic" % "0.14.6",
      "io.circe" %% "circe-parser" % "0.14.6",
      
      // Testing
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.18" % Test
    ),
    
    // Force specific versions for conflicting dependencies
    dependencyOverrides ++= Seq(
      "org.scala-lang.modules" %% "scala-parser-combinators" % "2.3.0",
      "org.scala-lang" % "scala-library" % "2.13.13"
    ),
    
    // Ignore binary incompatibility warnings for scala-parser-combinators
    evictionErrorLevel := Level.Info,
    
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
