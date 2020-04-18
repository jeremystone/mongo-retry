package docker

import com.whisk.docker.{DockerContainer, DockerKit, DockerReadyChecker}

object DockerToxiproxyService {
  val ProxyPort = 30000
  val ProxyAPIPort = 8474
}

trait DockerToxiproxyService extends DockerKit {

  val toxiproxyContainer: DockerContainer = DockerContainer("shopify/toxiproxy")
    .withReadyChecker(DockerReadyChecker.LogLineContains("API HTTP server starting"))
    .withNetworkMode("host")

  abstract override def dockerContainers: List[DockerContainer] =
    toxiproxyContainer :: super.dockerContainers
}