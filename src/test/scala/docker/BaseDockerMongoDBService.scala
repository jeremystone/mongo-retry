package docker

import com.google.common.base.Charsets.UTF_8
import com.spotify.docker.client.{DefaultDockerClient, DockerClient, LogStream}
import com.whisk.docker.{DockerContainer, DockerKit, DockerReadyChecker}

import scala.jdk.CollectionConverters._

trait BaseDockerMongoDBService extends DockerKit {

  private lazy val docker = DefaultDockerClient.fromEnv.build

  def createContainer(mongodCommand: String*): DockerContainer = DockerContainer("mongo:3.6.14")
    .withReadyChecker(DockerReadyChecker.LogLineContains("waiting for connections on port"))
    .withCommand(mongodCommand: _*)
    .withNetworkMode("host")

  def execMongoCommand(dockerContainer: DockerContainer, port : Int, command: String) = {
    for (name <- dockerContainer.getName) yield {

      docker.listContainers().asScala
        .find ( container => container.names().contains(name))
        .map { container =>
          val cmd = Array("sh", "-c", s"""mongo --port $port --eval '$command' """)
          val execCreation = docker.execCreate(
            container.id(), cmd,
            DockerClient.ExecCreateParam.attachStdout,
            DockerClient.ExecCreateParam.attachStderr)

          val output = docker.execStart(execCreation.id)

          readFully(output)
        }
        .getOrElse("Container not found")
    }
  }

  private def readFully(logStream: LogStream) = {
    val stringBuilder = new StringBuilder

    try {
      while (logStream.hasNext) stringBuilder.append(UTF_8.decode(logStream.next.content))
    }
    catch{
      case e: Exception => // Ignore ? get 'connection reset by peer' at end of output
    }
    stringBuilder.toString
  }
}