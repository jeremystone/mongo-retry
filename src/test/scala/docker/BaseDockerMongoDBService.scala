package docker

import com.google.common.base.Charsets.UTF_8
import com.spotify.docker.client.DockerClient.{ExecStartParameter, ListNetworksParam}
import com.spotify.docker.client.messages.{Container, NetworkConfig}
import com.spotify.docker.client.{DefaultDockerClient, DockerClient, LogStream}
import com.whisk.docker.{DockerContainer, DockerKit, DockerPortMapping, DockerReadyChecker}

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

trait BaseDockerMongoDBService extends DockerKit {
  private lazy val docker = DefaultDockerClient.fromEnv.build

  val networkName = "mongo-retry_mongo-net"

  def createContainer(name: String, ps: (Int, Int), entryPoint: String*): DockerContainer =
    DockerContainer("mongo:3.6", name = Some(name))
      .withReadyChecker(DockerReadyChecker.LogLineContains("waiting for connections on port"))
      .withEntrypoint(entryPoint: _*)
      .withPortMapping(ps._1 -> DockerPortMapping(Some(ps._2)))
      .withNetworkMode("bridge")

  private def containerFor(dockerContainer: DockerContainer): Future[Container] =
    for (name <- dockerContainer.getName) yield
      docker.listContainers().asScala
        .find(container => container.names().contains(name))
        .getOrElse(throw new RuntimeException(s"No container with name ${dockerContainer.name}"))

  def execMongoCommand(dockerContainer: DockerContainer, port: Int, command: String): Future[String] =
    containerFor(dockerContainer)
      .map { container =>
        val cmd = Array("sh", "-c", s"""mongo --port $port --eval '$command' """)
        val execCreation = docker.execCreate(
          container.id(), cmd,
          DockerClient.ExecCreateParam.attachStdout,
          DockerClient.ExecCreateParam.attachStderr)

        val output = docker.execStart(execCreation.id, ExecStartParameter.TTY)

        readFully(output)
      }

  def connectToNetwork(dockerContainer: DockerContainer): Future[Unit] =
    containerFor(dockerContainer)
      .map { container =>
        docker.connectToNetwork(container.id(), networkName)
      }

  def createNetwork(): Unit = {
    docker.listNetworks(ListNetworksParam.byNetworkName(networkName)).asScala
      .map(_.id)
      .foreach(docker.removeNetwork)

    docker.createNetwork(
      NetworkConfig.builder()
        .name(networkName)
        .build())
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