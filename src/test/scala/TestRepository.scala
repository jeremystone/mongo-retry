import scala.concurrent.Future

trait TestRepository {

  def clear: Future[Unit]

  def count: Future[Long]

  def insert(num: Int): Future[Unit]

}

trait TestRepositoryComponent {
  def testRepository : TestRepository
}
