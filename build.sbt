PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

name := "ledger-manager-hsm-proxy"

version := "1.0.0"

scalaVersion := "2.12.6"


lazy val versions = new {
  val websocket = "1.3.8"
  val finatra = "18.1.0"
  val typesafe_config = "1.3.2"
  val scala_uri = "1.1.1"
  val scala_test = "3.0.5"
}

libraryDependencies ++= Seq(
  "org.java-websocket" % "Java-WebSocket" % versions.websocket,
  "com.twitter" %% "finatra-jackson"  % versions.finatra,
  "com.typesafe" % "config" % versions.typesafe_config,
  "io.lemonlabs" %% "scala-uri" % versions.scala_uri,
  "org.scalactic" %% "scalactic" % versions.scala_test,
  "org.scalatest" %% "scalatest" % versions.scala_test % "test",
)