package support

trait ConnectionConfig {
  def hosts:Seq[String]
}

trait ConnectionConfigComponent {
  def connectionConfig: ConnectionConfig
}
