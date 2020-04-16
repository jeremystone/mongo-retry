import com.mongodb.connection.ClusterConnectionMode
import org.mongodb.scala.{MongoClient, MongoClientSettings, MongoCollection, MongoDatabase, ServerAddress}
import org.mongodb.scala.bson.Document
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.collection.JavaConverters._
import scala.util.control.NonFatal
import scala.concurrent.ExecutionContext.Implicits.global

trait ScalaDriverTestRepositoryComponent extends TestRepositoryComponent {

  private val logger = LoggerFactory.getLogger(getClass)

  override lazy val testRepository: TestRepository = new TestRepository with Ports {

    val settings: MongoClientSettings =
      MongoClientSettings.builder()
        .applyToClusterSettings(b => b.hosts(List(
          new ServerAddress(s"127.0.0.1:$ProxyPort")).asJava).mode(ClusterConnectionMode.SINGLE))
        .build()

    private val mongoClient: MongoClient = MongoClient(settings)

    private def database: MongoDatabase = mongoClient.getDatabase("dbtest")

    private def testCollection: MongoCollection[Document] = database.getCollection("test")

    override def clear: Future[Unit] =
      testCollection.deleteMany(Document.empty).toFuture().map(_ => Future.unit)

    override def count: Future[Long] =
      testCollection.countDocuments(Document.empty).toFuture()

    override def insert(num: Int): Future[Unit] =
      (1 to num).foldLeft(Future.unit) {
        (prev, i) =>
          for {
            _ <- prev
            _ <- testCollection.insertOne(Document("i" -> i)).toFuture()
              .recover {
                case NonFatal(e) => logger.error(s"Write $i failed", e)
              }
          } yield logger.debug(s"Done $i")
      }
  }
}