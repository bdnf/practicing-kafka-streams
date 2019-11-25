name := "scala-examples"

version := "0.1"

scalaVersion := "2.12.8"

lazy val kafkaVer = "2.2.0"

libraryDependencies ++= Seq(
  "org.apache.kafka" %% "kafka-streams-scala" % kafkaVer,
  "com.sksamuel.avro4s" %% "avro4s-core" % "2.0.4"

)