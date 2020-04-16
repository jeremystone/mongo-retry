import org.slf4j.LoggerFactory
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{FailoverStrategy, ReadConcern, WriteConcern}
import reactivemongo.bson.BSONDocument

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal

trait ReactiveMongoTestRepositoryComponent extends TestRepositoryComponent {

  private val logger = LoggerFactory.getLogger(getClass)

  override lazy val testRepository: TestRepository = new TestRepository with Ports {

    private val driver = new reactivemongo.api.AsyncDriver

    private val failoverStrategy = FailoverStrategy(
      initialDelay = 500.millis,
      retries = 10,
      delayFactor = i => 2 * i
    )

    private val testConnection = driver.connect(List(s"127.0.0.1:$ProxyPort"))

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

    override def insert(num: Int): Future[Unit] =
      (1 to num).foldLeft(Future.unit) { (prev, i) =>
        for {
          _ <- prev
          collection <- testCollection
          _ <- collection.insert(ordered = false, WriteConcern.Acknowledged).one(BSONDocument("i" -> i))
            .recover {
              case NonFatal(e) => logger.error(s"Write $i failed", e)
            }
        } yield logger.debug(s"Done $i")
      }
  }
}