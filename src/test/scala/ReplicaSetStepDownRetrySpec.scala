import com.whisk.docker.impl.dockerjava.DockerKitDockerJava
import com.whisk.docker.scalatest.DockerTestKit
import docker.{Mongo0DBService, Mongo1DBService, Mongo2DBService}
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{Matchers, WordSpec}
import org.slf4j.LoggerFactory
import support.{ReplicaSetMongoConnectionConfigComponent, TestRepositoryComponent}

trait ReplicaSetStepDownRetrySpec
  extends WordSpec
    with Matchers
    with DockerTestKit
    with DockerKitDockerJava
    with Mongo0DBService
    with Mongo1DBService
    with Mongo2DBService
    with ReplicaSetMongoConnectionConfigComponent {
  self: TestRepositoryComponent =>

  private val logger = LoggerFactory.getLogger(getClass)

  implicit val pc: PatienceConfig = PatienceConfig(Span(60, Seconds), Span(1, Second))

  "containers" must {
    "be ready" in {
      createNetwork()

      isContainerReady(mongodb0Container).futureValue shouldBe true
      isContainerReady(mongodb1Container).futureValue shouldBe true
      isContainerReady(mongodb2Container).futureValue shouldBe true

      connectToNetwork(mongodb0Container)
      connectToNetwork(mongodb1Container)
      connectToNetwork(mongodb2Container)

      val initiateResult = execMongoCommand(mongodb0Container, 27017,
        """rs.initiate({ _id: "rs0", members: [ { _id: 0, host: "mongo0:27017" }, { _id: 1, host: "mongo1:27018" }, { _id: 2, host: "mongo2:27019", arbiterOnly:true }]})""")
        .futureValue

      logger.debug(initiateResult)

      waitForReplSet
    }
  }

  private def waitForReplSet = {
    (1 to 20).find { i =>
      logger.info(s"Waiting for replset - attempt $i...")
      val statusResult = execMongoCommand(mongodb0Container, 27017,
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
      val numInserts = 100

      val result = for {
        _ <- testRepository.clear
        _ <- testRepository.insert(numInserts) { i =>
          if (i == numInserts / 2) {
            logger.info("Stepping down")
            execMongoCommand(mongodb0Container, 27017, "rs.stepDown(10)")
          }
          logger.info(s"Writing $i")
        }
          .recover {
            case e => logger.error(s"insert failed", e)
          }
        _ = waitForReplSet
        _ = logger.info("Counting")
        count <- testRepository.count
      } yield count

      result.futureValue shouldBe numInserts
    }

    "recover" in {
      val numInserts = 10

      val result = for {
        _ <- testRepository.clear
        _ <- testRepository.insert(numInserts) { i =>

          logger.info(s"Writing $i")
        }
          .recover {
            case e => logger.error(s"insert failed", e)
          }
        _ = waitForReplSet
        _ = logger.info("Counting")
        count <- testRepository.count
      } yield count

      result.futureValue shouldBe numInserts
    }
  }

}
