package support

import docker.DockerToxiproxyService._

trait  ReplicaSetMongoConnectionConfigComponent extends ConnectionConfigComponent {
  lazy val connectionConfig: ConnectionConfig = new ConnectionConfig {
    def hosts = List(s"localhost:$ProxyPort1", s"localhost:$ProxyPort2", s"localhost:$ProxyPort3")
  }
}
