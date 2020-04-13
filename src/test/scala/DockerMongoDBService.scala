import com.whisk.docker.DockerContainer
import com.whisk.docker.config.DockerKitConfig

trait DockerMongoDBService extends DockerKitConfig {

  val mongodbContainer: DockerContainer = configureDockerContainer("docker.mongodb")

  abstract override def dockerContainers: List[DockerContainer] =
    mongodbContainer :: super.dockerContainers
}