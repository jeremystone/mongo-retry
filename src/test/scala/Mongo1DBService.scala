import com.whisk.docker.DockerContainer

trait Mongo1DBService extends BaseDockerMongoDBService {
  self: Ports =>

  val mongodb1Container = createContainer("mongod", "--port", "27017",
    "--bind_ip_all" ,"--replSet", "rs0", "--smallfiles", "--syncdelay", "0")

  abstract override def dockerContainers: List[DockerContainer] =
    mongodb1Container :: super.dockerContainers
}