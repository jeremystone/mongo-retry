package support

trait  ReplicaSetMongoConnectionConfigComponent extends ConnectionConfigComponent {
  lazy val connectionConfig: ConnectionConfig = new ConnectionConfig {
    def hosts = List("localhost:27017", "localhost:27018", "localhost:27019")
  }
}
