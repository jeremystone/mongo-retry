package docker

import com.whisk.docker.DockerContainer

trait Mongo2DBService extends BaseDockerMongoDBService {

  val mongodb2Container = createContainer("mongod", "--port", "27018",
    "--bind_ip_all", "--replSet", "rs0", "--smallfiles", "--syncdelay", "0")

  abstract override def dockerContainers: List[DockerContainer] =
    mongodb2Container :: super.dockerContainers
}