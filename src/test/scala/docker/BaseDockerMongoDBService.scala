package docker

import com.google.common.base.Charsets.UTF_8
import com.spotify.docker.client.DockerClient.ExecStartParameter
import com.spotify.docker.client.{DefaultDockerClient, DockerClient, LogStream}
import com.whisk.docker.{DockerContainer, DockerKit, DockerPortMapping, DockerReadyChecker}

import scala.jdk.CollectionConverters._

trait BaseDockerMongoDBService extends DockerKit {

  def createContainer(name: String, ps: (Int, Int), entryPoint: String*): DockerContainer =
    DockerContainer("mongo:3.6", name = Some(name))
      .withReadyChecker(DockerReadyChecker.LogLineContains("waiting for connections on port"))
      .withEntrypoint(entryPoint: _*)
      .withPortMapping(ps._1 -> DockerPortMapping(Some(ps._2)))
      .withNetworkMode("bridge")

  def execMongoCommand(dockerContainer: DockerContainer, port: Int, command: String) = {
    for (name <- dockerContainer.getName) yield {

      val docker = DefaultDockerClient.fromEnv.build

      docker.listContainers().asScala
        .find(container => container.names().contains(name))
        .map { container =>
          val cmd = Array("sh", "-c", s"""mongo --port $port --eval '$command' """)
          val execCreation = docker.execCreate(
            container.id(), cmd,
            DockerClient.ExecCreateParam.attachStdout,
            DockerClient.ExecCreateParam.attachStderr)

          val output = docker.execStart(execCreation.id, ExecStartParameter.TTY)

          readFully(output)
        }
        .getOrElse("Container not found")
    }
  }

  def connectToNetwork(dockerContainer: DockerContainer, networkId: String) = {
    for (name <- dockerContainer.getName) yield {

      val docker = DefaultDockerClient.fromEnv.build

      docker.listContainers().asScala
        .find(container => container.names().contains(name))
        .map { container =>
          docker.connectToNetwork(container.id(), networkId)
        }
        .getOrElse("Container not found")
    }
  }

  private def readFully(logStream: LogStream) = {
    val stringBuilder = new StringBuilder

    try {
      while (logStream.hasNext) stringBuilder.append(UTF_8.decode(logStream.next.content))
    }
    catch {
      case e: Exception => // Ignore ? get 'connection reset by peer' at end of output
    }
    stringBuilder.toString
  }
}