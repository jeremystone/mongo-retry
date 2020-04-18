trait  SingleMongoConnectionConfigComponent extends ConnectionConfigComponent {
  lazy val connectionConfig: ConnectionConfig = new ConnectionConfig {
    def hosts = List("localhost:27017")
  }
}
