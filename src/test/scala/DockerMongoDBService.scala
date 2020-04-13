import com.whisk.docker.{DockerContainer, DockerKit, DockerReadyChecker}

trait DockerMongoDBService extends DockerKit with Ports{


  val mongodbContainer: DockerContainer = DockerContainer("mongo:3.6.14")
    .withHostname("mongo")
    .withPorts(MongodbPort -> Some(MongodbPort))
    .withReadyChecker(DockerReadyChecker.LogLineContains("waiting for connections on port"))
    .withCommand("mongod", "--nojournal", "--smallfiles", "--syncdelay", "0")

  abstract override def dockerContainers: List[DockerContainer] =
    mongodbContainer :: super.dockerContainers
}