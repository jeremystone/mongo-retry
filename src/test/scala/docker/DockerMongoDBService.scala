package docker

import com.whisk.docker.DockerContainer

trait DockerMongoDBService extends BaseDockerMongoDBService {

  val mongodbContainer = createContainer("mongod", "--nojournal", "--smallfiles", "--syncdelay", "0")

  abstract override def dockerContainers: List[DockerContainer] =
    mongodbContainer :: super.dockerContainers
}