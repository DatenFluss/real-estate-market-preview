package services

import zio.{Duration => ZDuration, _}
import zio.kafka.consumer._
import zio.kafka.producer._
import zio.kafka.serde._
import zio.stream._
import javax.inject._
import play.api.Configuration
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.apache.kafka.clients.consumer.ConsumerRecord
import scala.concurrent.duration.DurationInt

@Singleton
class KafkaService @Inject()(
    config: Configuration,
    sparkService: SparkService
) {
  private val bootstrapServers = config.get[String]("kafka.bootstrap.servers")
  private val groupId = config.get[String]("kafka.group.id")
  
  private val consumerSettings = ConsumerSettings(List(bootstrapServers))
    .withGroupId(groupId)
    .withCloseTimeout(ZDuration.fromScala(DurationInt(5).seconds))
  
  private val producerSettings = ProducerSettings(List(bootstrapServers))
  
  def streamPropertyUpdate(propertyData: PropertyData): Task[Unit] = {
    val producer = Producer.make(producerSettings)
    
    ZIO.scoped {
      producer.flatMap { producer =>
        producer.produce(
          topic = "property-updates",
          key = propertyData.address,
          value = propertyData.asJson.noSpaces,
          keySerializer = Serde.string,
          valueSerializer = Serde.string
        ).unit
      }
    }
  }
  
  def processMarketTrends: ZIO[Any, Throwable, Unit] = {
    val consumer = Consumer.make(consumerSettings)
    
    ZIO.scoped {
      consumer.flatMap { consumer =>
        consumer.plainStream(
          Subscription.topics("property-updates"),
          Serde.string,
          Serde.string
        ).mapZIO { record =>
          for {
            propertyData <- ZIO.fromEither(decode[PropertyData](record.value))
            _ <- sparkService.analyzeMarketTrends(Seq(propertyData))
              .flatMap(analysis => 
                Producer.make(producerSettings).flatMap(_.produce(
                  "market-trends",
                  record.key,
                  analysis.asJson.noSpaces,
                  Serde.string,
                  Serde.string
                )))
          } yield ()
        }.runDrain
      }
    }
  }
  
  def streamPriceAlerts(threshold: Double): ZStream[Any, Throwable, PriceAlert] = {
    ZStream.unwrapScoped {
      Consumer.make(consumerSettings).map { consumer =>
        consumer.plainStream(
          Subscription.topics("property-updates"),
          Serde.string,
          Serde.string
        ).mapZIO { record =>
          for {
            propertyData <- ZIO.fromEither(decode[PropertyData](record.value))
            alert = PriceAlert(
              property = propertyData,
              threshold = threshold,
              exceededBy = propertyData.zestimate - threshold
            )
          } yield alert
        }.filter(_.exceededBy > 0)
      }
    }
  }
}

case class PriceAlert(
    property: PropertyData,
    threshold: Double,
    exceededBy: Double
) 
