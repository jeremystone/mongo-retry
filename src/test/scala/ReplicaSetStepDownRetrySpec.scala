import com.whisk.docker.impl.dockerjava.DockerKitDockerJava
import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.time.{Second, Seconds, Span}

import scala.collection.JavaConverters._
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.ExecCreation

class ReplicaSetStepDownRetrySpec
  extends WordSpec
    with Matchers
    with DockerTestKit
    with DockerKitDockerJava
    with DockerMongoDBService
    with ReactiveMongoTestRepositoryComponent
    with Ports {

  implicit val pc = PatienceConfig(Span(60, Seconds), Span(1, Second))

  "containers" must {
    "be ready" in {
      isContainerReady(mongodbContainer).futureValue shouldBe true

      // FIXME MOVE THIS CODE TO FIND CONTAINER INTO A BASE CONTAINER TRAIT

      import com.spotify.docker.client.DefaultDockerClient
      import com.spotify.docker.client.DockerClient

      val docker = DefaultDockerClient.fromEnv.build

      val optContainer = docker.listContainers().asScala.find {
        _.image().contains("mongo")
      }

      optContainer match {
        case Some(c) =>
          val id = c.id()

          val command = Array("sh", "-c", """mongo --eval "db.serverStatus()" """)
          val execCreation = docker.execCreate(id, command,
            DockerClient.ExecCreateParam.attachStdout,
            DockerClient.ExecCreateParam.attachStderr)
          val output = docker.execStart(execCreation.id)
          println(output.readFully)
        case None => println("NO MONGO")
      }

    }
  }


  "mongo driver" must {
    "not lose writes" in {
      fail
    }
  }

}
