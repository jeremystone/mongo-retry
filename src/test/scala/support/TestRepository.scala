package support

import scala.concurrent.Future

trait TestRepository {

  def clear: Future[Unit]

  def count: Future[Long]

  def insert(num: Int)(callback: Int => Unit = Function.const(())): Future[Unit]

}

trait TestRepositoryComponent {
  def testRepository: TestRepository
}
