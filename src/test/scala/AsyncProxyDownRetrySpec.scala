import java.util.concurrent.CountDownLatch

import com.whisk.docker.impl.dockerjava.DockerKitDockerJava
import com.whisk.docker.scalatest.DockerTestKit
import eu.rekawek.toxiproxy.ToxiproxyClient
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{Matchers, WordSpec}

class AsyncProxyDownRetrySpec
  extends WordSpec
    with Matchers
    with DockerTestKit
    with DockerKitDockerJava
    with DockerMongoDBService
    with DockerToxiproxyService
    with ReactiveMongoTestRepositoryComponent
    with SingleMongoConnectionConfigComponent
    with Ports {

  implicit val pc = PatienceConfig(Span(60, Seconds), Span(1, Second))

  "containers" must {
    "be ready" in {
      isContainerReady(mongodbContainer).futureValue shouldBe true
      isContainerReady(toxiproxyContainer).futureValue shouldBe true
    }
  }

  "mongo driver" must {
    "not lose writes" in {
      val client = new ToxiproxyClient("localhost", APIPort)

      val mongodbAddress = mongodbContainer.getIpAddresses().map(_.head).futureValue
      val proxy = client.createProxy("mongo", s"0.0.0.0:$ProxyPort", s"$mongodbAddress:$MongodbPort")

      val latch = new CountDownLatch(1)

      new Thread(() => {
        latch.await()
        proxy.disable()
        Thread.sleep(100)
        proxy.enable()
      }).start()

      val numInserts = 100

      val result = for {
        _ <- testRepository.clear
        _ <- testRepository.insert(numInserts) { i =>
          if (i == numInserts / 2) latch.countDown()
        }
        count <- testRepository.count
      } yield count

      result.futureValue shouldBe numInserts
    }
  }
}
