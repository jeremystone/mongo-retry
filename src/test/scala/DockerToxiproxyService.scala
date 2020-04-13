import com.whisk.docker.DockerContainer
import com.whisk.docker.config.DockerKitConfig

trait DockerToxiproxyService extends DockerKitConfig {

  val toxiproxyContainer: DockerContainer = configureDockerContainer("docker.toxiproxy")

  abstract override def dockerContainers: List[DockerContainer] =
    toxiproxyContainer :: super.dockerContainers
}