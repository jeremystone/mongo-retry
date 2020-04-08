/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

import org.slf4j.LoggerFactory
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{FailoverStrategy, ReadConcern, WriteConcern}
import reactivemongo.bson.BSONDocument

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.control.NonFatal

object ReactiveMongoRetry extends App {
  val logger = LoggerFactory.getLogger(getClass)
  val driver = new reactivemongo.api.AsyncDriver

  import scala.concurrent.Future

  val writeConcern = WriteConcern.ReplicaAcknowledged(2, 10000, journaled = true)

  private val failoverStrategy = FailoverStrategy(
    initialDelay = 500.millis,
    retries = 10,
    delayFactor = _ => 1
  )

  val numInserts = 3000

  val result = for {
    connection <- driver.connect(List("localhost:30001", "localhost:30002", "localhost:30003"))
    database <- connection.database("dbtest")
    collection = database.collection[BSONCollection]("test", failoverStrategy = failoverStrategy)
    _ <- collection.delete().one(BSONDocument.empty)
    _ <- doInserts(collection)
    count <- collection.count(None, None, 0, None, readConcern = ReadConcern.Majority)
  } yield count

  def doInserts(collection: BSONCollection) =
    (1 to numInserts).foldLeft(Future.unit) {
      (prev, i) =>
        for {
          _ <- prev
          _ <- collection.insert(ordered = false, writeConcern).one(BSONDocument("i" -> i))
            .recover {
              case NonFatal(e) => logger.error(s"Write $i failed", e)
            }
        } yield logger.info(s"Done $i")
    }

  result.foreach { numWritten =>
    logger.info(s"Docss written $numWritten out of $numInserts")
  }

  Await.ready(result, Duration.Inf)
  System.exit(0)
}
