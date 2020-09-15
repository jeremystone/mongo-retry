package docker

import com.whisk.docker.DockerContainer

trait Mongo2DBService extends BaseDockerMongoDBService {

  val mongodb2Container: DockerContainer =
    createContainer("mongo2", 27019 -> 27019,"/usr/bin/mongod", "--port", "27019",
      "--bind_ip_all", "--replSet", "rs0").withHostname("mongo2")

  abstract override def dockerContainers: List[DockerContainer] =
    mongodb2Container :: super.dockerContainers
}