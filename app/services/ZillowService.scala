package services

import zio._
import sttp.client3._
import io.circe.generic.auto._
import io.circe.parser._
import javax.inject._
import play.api.Configuration
import sttp.client3.httpclient.zio.HttpClientZioBackend

@Singleton
class ZillowService @Inject()(config: Configuration) {
  private val apiKey = config.get[String]("zillow.api.key")
  private val baseUrl = "https://api.bridgedataoutput.com/api/v2/zestimates"
  
  private val backend = HttpClientZioBackend()
  
  def getPropertyData(address: String): Task[Option[PropertyData]] = {
    val request = basicRequest
      .get(uri"$baseUrl?address=$address")
      .header("Authorization", s"Bearer $apiKey")
      .response(asStringAlways)
    
    ZIO.scoped {
      backend.flatMap { backend =>
        request
          .send(backend)
          .map(_.body)
          .flatMap(body => ZIO.fromEither(decode[PropertyResponse](body)))
          .map(_.property)
      }
    }
  }
}

case class PropertyResponse(property: Option[PropertyData])
case class PropertyData(
    address: String,
    zestimate: Double,
    rentZestimate: Option[Double],
    lastUpdated: String,
    latitude: Double,
    longitude: Double
)
