import com.whisk.docker.impl.dockerjava.DockerKitDockerJava
import com.whisk.docker.scalatest.DockerTestKit
import eu.rekawek.toxiproxy.ToxiproxyClient
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{Matchers, WordSpec}
import org.slf4j.LoggerFactory
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{FailoverStrategy, ReadConcern, WriteConcern}
import reactivemongo.bson.BSONDocument

import scala.concurrent.duration._
import scala.util.control.NonFatal


class ReactiveMongoDriverRetrySpec
  extends WordSpec
    with Matchers
    with DockerTestKit
    with DockerKitDockerJava
    with DockerMongoDBService
    with DockerToxiproxyService {

  implicit val pc = PatienceConfig(Span(60, Seconds), Span(1, Second))

  val logger = LoggerFactory.getLogger(getClass)
  val driver = new reactivemongo.api.AsyncDriver

  import scala.concurrent.Future

  val writeConcern = WriteConcern.ReplicaAcknowledged(2, 10000, journaled = true)

  private val failoverStrategy = FailoverStrategy(
    initialDelay = 500.millis,
    retries = 10,
    delayFactor = _ => 1
  )

  def doInserts(num: Int, collection: BSONCollection): Future[Unit] =
    (1 to num).foldLeft(Future.unit) {
      (prev, i) =>
        for {
          _ <- prev
          _ <- collection.insert(ordered = false, writeConcern).one(BSONDocument("i" -> i))
            .recover {
              case NonFatal(e) => logger.error(s"Write $i failed", e)
            }
        } yield logger.info(s"Done $i")
    }

  "containers" must {
    "be ready" in {
      isContainerReady(mongodbContainer).futureValue shouldBe true
      isContainerReady(toxiproxyContainer).futureValue shouldBe true
    }

    "be configured" in {
      val client = new ToxiproxyClient("localhost", 8474)

      client.createProxy("mongo", "127.0.0.1:27017", "127.0.0.1:34000")
    }
  }

  "mongo driver" must {
    "not lose writes" in {
      val numInserts = 3000

      val result: Future[Long] = for {
        connection <- driver.connect(List("localhost:27017"))
        database <- connection.database("dbtest")
        collection = database.collection[BSONCollection]("test", failoverStrategy = failoverStrategy)
        _ <- collection.delete().one(BSONDocument.empty)
        _ <- doInserts(numInserts, collection)
        count <- collection.count(None, None, 0, None, readConcern = ReadConcern.Majority)
      } yield count


      result.futureValue shouldBe numInserts
    }
  }


}
