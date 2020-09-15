package support

trait  ReplicaSetMongoConnectionConfigComponent extends ConnectionConfigComponent {
  lazy val connectionConfig: ConnectionConfig = new ConnectionConfig {
    def hosts = List(s"mongo0:27017", s"mongo1:27018", s"mongo2:27019")
  }
}
