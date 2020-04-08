/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

import com.mongodb.connection.ClusterConnectionMode
import org.mongodb.scala._
import org.mongodb.scala.bson.Document
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal

object ScalaDriverMongoRetry extends App {
  val logger = LoggerFactory.getLogger(getClass)

  import scala.concurrent.Future

  val settings: MongoClientSettings =
    MongoClientSettings.builder()
      .applyToClusterSettings(b => b.hosts(List(
        new ServerAddress("localhost:30001"),
        new ServerAddress("localhost:30002"),
        new ServerAddress("localhost:30003")).asJava).mode(ClusterConnectionMode.MULTIPLE))
      .build()

  val mongoClient: MongoClient = MongoClient(settings)

  val database: MongoDatabase = mongoClient.getDatabase("dbtest")

  val collection: MongoCollection[Document] = database.getCollection("test")

  val numInserts = 3000

  val result = for {
    _ <- collection.deleteMany(Document.empty).toFuture()
    _ <- doInserts()
    count <- collection.countDocuments(Document.empty).toFuture()
  } yield count

  def doInserts() =
    (1 to numInserts).foldLeft(Future.unit) {
      (prev, i) =>
        for {
          _ <- prev
          _ <- collection.insertOne(Document("i" -> i)).toFuture()
            .recover {
              case NonFatal(e) => logger.error(s"Write $i failed", e)
            }
        } yield logger.info(s"Done $i")
    }

  result.foreach { numWritten =>
    logger.info(s"Docss written $numWritten out of $numInserts")
  }

  Await.ready(result, Duration.Inf)
}
