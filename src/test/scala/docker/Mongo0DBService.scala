package docker

import com.whisk.docker.{DockerContainer, DockerFactory}

trait Mongo0DBService extends BaseDockerMongoDBService {

  val mongodb0Container: DockerContainer =
    createContainer("mongo0", 27017 -> 27017, "/usr/bin/mongod", "--port", "27017",
      "--bind_ip_all", "--replSet", "rs0").withHostname("mongo0")

  abstract override def dockerContainers: List[DockerContainer] =
    mongodb0Container :: super.dockerContainers
}