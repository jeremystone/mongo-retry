package support

import docker.DockerToxiproxyService._

trait  ProxiedMongoConnectionConfigComponent extends ConnectionConfigComponent {
  lazy val connectionConfig: ConnectionConfig = new ConnectionConfig {
    def hosts = List(s"localhost:$ProxyPort1")
  }
}
