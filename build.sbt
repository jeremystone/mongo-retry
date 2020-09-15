name := "mongo-retry"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies += "org.reactivemongo" %% "reactivemongo" % "0.18.8"
libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "2.9.0"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.30"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

libraryDependencies ++= Seq(
  "com.whisk" %% "docker-testkit-scalatest" % "0.9.9" % "test",
  "com.whisk" %% "docker-testkit-config" % "0.9.9" % "test",
  "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.9" % "test")