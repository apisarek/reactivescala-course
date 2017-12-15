name := "e-shop"

version := "0.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.4",
  "com.typesafe.akka" %% "akka-remote" % "2.5.4",
  "com.typesafe.akka" %% "akka-stream" % "2.5.4",
  "com.typesafe.akka" %% "akka-http" % "10.0.10",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.4" % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "com.typesafe.akka" %% "akka-persistence" % "2.5.4",
  "org.iq80.leveldb" % "leveldb" % "0.9",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.1.1",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.10",
  "net.liftweb" %% "lift-json" % "3.1.1",
  "com.typesafe.akka" %% "akka-cluster" % "2.5.7",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.5.7"
)

