package support

trait  ReplicaSetMongoConnectionConfigComponent extends ConnectionConfigComponent {
  lazy val connectionConfig: ConnectionConfig = new ConnectionConfig {
    def hosts = List(s"localhost:27017", s"localhost:27018", s"localhost:27019")
  }
}
