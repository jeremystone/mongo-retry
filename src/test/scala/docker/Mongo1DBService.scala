package docker

import com.whisk.docker.DockerContainer

trait Mongo1DBService extends BaseDockerMongoDBService {

  val mongodb1Container: DockerContainer =
    createContainer("mongo1", 27018 -> 27018,"/usr/bin/mongod", "--port", "27018",
      "--bind_ip_all", "--replSet", "rs0").withHostname("mongo1")

  abstract override def dockerContainers: List[DockerContainer] =
    mongodb1Container :: super.dockerContainers
}