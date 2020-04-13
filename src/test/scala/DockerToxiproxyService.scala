import com.whisk.docker.{DockerContainer, DockerKit, DockerReadyChecker}

trait DockerToxiproxyService extends DockerKit with Ports {

  val toxiproxyContainer: DockerContainer = DockerContainer("shopify/toxiproxy")
    .withPorts(APIPort -> Some(APIPort), ProxyPort -> Some(ProxyPort))
    .withReadyChecker(DockerReadyChecker.LogLineContains("API HTTP server starting"))

  abstract override def dockerContainers: List[DockerContainer] =
    toxiproxyContainer :: super.dockerContainers
}