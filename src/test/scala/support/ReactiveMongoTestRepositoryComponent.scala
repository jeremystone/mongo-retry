package support

import org.slf4j.LoggerFactory
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{FailoverStrategy, ReadConcern, WriteConcern}
import reactivemongo.bson.BSONDocument

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal

trait ReactiveMongoTestRepositoryComponent extends TestRepositoryComponent {
  self: ConnectionConfigComponent =>

  private val logger = LoggerFactory.getLogger(getClass)

  override lazy val testRepository: TestRepository = new TestRepository {

    private val driver = new reactivemongo.api.AsyncDriver

    private val failoverStrategy = FailoverStrategy(
      initialDelay = 500.millis,
      retries = 10,
      delayFactor = i => 2 * i
    )

    private val writeConcern = if (connectionConfig.hosts.size == 1)
      WriteConcern.Acknowledged
    else
      WriteConcern.ReplicaAcknowledged(2, 10000, journaled = true)

    private lazy val testConnection = driver.connect(connectionConfig.hosts)

    private def testCollection =
      for {
        connection <- testConnection
        database <- connection.database("dbtest")
      } yield database.collection[BSONCollection]("test", failoverStrategy = failoverStrategy)

    override def clear: Future[Unit] = for {
      collection <- testCollection
      _ <- collection.delete().one(BSONDocument.empty)
    } yield ()

    override def count: Future[Long] = for {
      collection <- testCollection
      count <- collection.count(None, None, 0, None, readConcern = ReadConcern.Majority)
    } yield count


    override def insert(num: Int)(callback: Int => Unit): Future[Unit] =
      (1 to num).foldLeft(Future.unit) { (prev, i) =>
        for {
          _ <- prev
          collection <- testCollection
          _ = callback(i)
          _ <- collection.insert(ordered = false, writeConcern).one(BSONDocument("i" -> i))
            .recover {
              case NonFatal(e) => logger.error(s"Write $i failed", e)
            }
        } yield {
          logger.info(s"Done $i")
        }
      }
  }
}