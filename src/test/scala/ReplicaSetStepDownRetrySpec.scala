import com.whisk.docker.impl.dockerjava.DockerKitDockerJava
import com.whisk.docker.scalatest.DockerTestKit
import docker.DockerToxiproxyService._
import docker.{DockerToxiproxyService, Mongo1DBService, Mongo2DBService, Mongo3DBService}
import eu.rekawek.toxiproxy.ToxiproxyClient
import eu.rekawek.toxiproxy.model.ToxicDirection
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{Matchers, WordSpec}
import org.slf4j.LoggerFactory
import support.{ReactiveMongoTestRepositoryComponent, ReplicaSetMongoConnectionConfigComponent}

class ReplicaSetStepDownRetrySpec
  extends WordSpec
    with Matchers
    with DockerTestKit
    with DockerKitDockerJava
    with Mongo1DBService
    with Mongo2DBService
    with Mongo3DBService
    with DockerToxiproxyService
    with ReactiveMongoTestRepositoryComponent
    with ReplicaSetMongoConnectionConfigComponent {

  private val logger = LoggerFactory.getLogger(getClass)

  implicit val pc = PatienceConfig(Span(60, Seconds), Span(1, Second))


  "containers" must {
    "be ready" in {
      isContainerReady(mongodb1Container).futureValue shouldBe true
      isContainerReady(mongodb2Container).futureValue shouldBe true
      isContainerReady(mongodb3Container).futureValue shouldBe true
      isContainerReady(toxiproxyContainer).futureValue shouldBe true

      val initiateResult = execMongoCommand(mongodb1Container, 27017,
        """rs.initiate({ _id: "rs0", members: [ { _id: 0, host: "localhost:27017" }, { _id: 1, host: "localhost:27018" }, { _id: 2, host: "localhost:27019", arbiterOnly:true }]})""")
        .futureValue

      logger.debug(initiateResult)

      waitForReplSet
    }
  }

  private def waitForReplSet = {
    (1 to 20).find { i =>
      logger.info(s"Waiting for replset - attempt $i...")
      val statusResult = execMongoCommand(mongodb1Container, 27017,
        """rs.status()""")
        .futureValue

      logger.debug(statusResult)

      // TODO do properly ...
      val ok = statusResult.contains(""""stateStr" : "PRIMARY"""") &&
        statusResult.contains(""""stateStr" : "SECONDARY"""") &&
        statusResult.contains(""""stateStr" : "ARBITER"""")

      if (!ok) Thread.sleep(1000)

      ok
    } should not be empty
  }

  "mongo driver" must {
    "not lose writes" in {
      val client = new ToxiproxyClient("localhost", ProxyAPIPort)

      val proxy1 = client.createProxy("mongo1", s"0.0.0.0:$ProxyPort1", "localhost:27017")
      val proxy2 = client.createProxy("mongo2", s"0.0.0.0:$ProxyPort2", "localhost:27018")
      val proxy3 = client.createProxy("mongo3", s"0.0.0.0:$ProxyPort3", "localhost:27019")

      proxy1.toxics().bandwidth("data limit", ToxicDirection.DOWNSTREAM, 128)
      proxy2.toxics().bandwidth("data limit", ToxicDirection.DOWNSTREAM, 128)
      proxy3.toxics().bandwidth("data limit", ToxicDirection.DOWNSTREAM, 128)

      val numInserts = 100

      val result = for {
        _ <- testRepository.clear
        _ <- testRepository.insert(numInserts) { i =>
          if (i == numInserts / 2) execMongoCommand(mongodb1Container, 27017, "rs.stepDown(10)")
        }.recover{
          case e => logger.error("write failed",  e)
        }
        _ = waitForReplSet
        _ = logger.info("Counting")
        count <- testRepository.count
      } yield count

      result.futureValue shouldBe numInserts
    }
  }

}
