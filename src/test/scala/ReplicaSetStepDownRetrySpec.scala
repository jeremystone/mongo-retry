import com.whisk.docker.impl.dockerjava.DockerKitDockerJava
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{Matchers, WordSpec}
import org.slf4j.LoggerFactory

class ReplicaSetStepDownRetrySpec
  extends WordSpec
    with Matchers
    with DockerTestKit
    with DockerKitDockerJava
    with Mongo1DBService
    with Mongo2DBService
    with Mongo3DBService
    with ReactiveMongoTestRepositoryComponent
    with Ports {

  private val logger = LoggerFactory.getLogger(getClass)

  implicit val pc = PatienceConfig(Span(60, Seconds), Span(1, Second))

  "containers" must {
    "be ready" in {
      isContainerReady(mongodb1Container).futureValue shouldBe true
      isContainerReady(mongodb2Container).futureValue shouldBe true
      isContainerReady(mongodb3Container).futureValue shouldBe true

      val result = execMongoCommand(mongodb1Container, 27017,
        """rs.initiate({ _id: "rs0", members: [ { _id: 0, host: "localhost:27017" }, { _id: 1, host: "localhost:27018" }, { _id: 2, host: "localhost:27019", arbiterOnly:true }]})""")
        .futureValue

      logger.info(result)
    }
  }


  "mongo driver" must {
    "not lose writes" in {
      fail
    }
  }

}
