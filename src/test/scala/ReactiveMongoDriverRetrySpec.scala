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
    with DockerToxiproxyService
    with Ports {

  implicit val pc = PatienceConfig(Span(60, Seconds), Span(1, Second))

  val logger = LoggerFactory.getLogger(getClass)
  val driver = new reactivemongo.api.AsyncDriver

  import scala.concurrent.Future

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
          _ <- collection.insert(ordered = false, WriteConcern.Acknowledged).one(BSONDocument("i" -> i))
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
      val client = new ToxiproxyClient("localhost", APIPort)
      mongodbContainer.getIpAddresses().map {
        ips =>
          val ip = ips.head

          client.createProxy("mongo", s"0.0.0.0:$ProxyPort", s"$ip:$MongodbPort")
      }.futureValue
    }
  }

  "mongo driver" must {
    "not lose writes" in {
      val numInserts = 1000

      val result: Future[Long] = for {
        connection <- driver.connect(List(s"127.0.0.1:$ProxyPort"))
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
