package docker

import com.whisk.docker.DockerContainer

trait Mongo3DBService extends BaseDockerMongoDBService {

  val mongodb3Container = createContainer("mongod", "--port", "27019",
    "--bind_ip_all", "--replSet", "rs0", "--smallfiles", "--syncdelay", "0")

  abstract override def dockerContainers: List[DockerContainer] =
    mongodb3Container :: super.dockerContainers
}