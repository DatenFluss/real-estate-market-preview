package controllers

import play.api.mvc._
import play.api.libs.json._
import play.api.http.ContentTypes
import org.analytics.estate.services._
import zio._
import scala.concurrent.{ExecutionContext, Future}
import javax.inject._
import io.circe.generic.auto._
import io.circe.syntax._
import zio.interop.reactivestreams._
import org.reactivestreams.Publisher
import play.api.libs.json.Json.toJson
import org.apache.pekko.stream.scaladsl.{Source, Sink}
import org.apache.pekko.util.ByteString

@Singleton
class AnalyticsController @Inject()(
    val controllerComponents: ControllerComponents,
    analyticsService: AnalyticsService
)(implicit ec: ExecutionContext)
    extends BaseController {

  implicit val propertyDataWrites: Writes[PropertyData] = Json.writes[PropertyData]
  implicit val marketAnalysisWrites: Writes[MarketAnalysis] = Json.writes[MarketAnalysis]
  implicit val neighborhoodAnalysisWrites: Writes[NeighborhoodAnalysis] = Json.writes[NeighborhoodAnalysis]
  implicit val priceAlertWrites: Writes[PriceAlert] = Json.writes[PriceAlert]

  def analyzeNeighborhood(): Action[JsValue] = Action(parse.json).async { request =>
    request.body.validate[List[String]] match {
      case JsSuccess(addresses, _) =>
        Unsafe.unsafe { implicit unsafe =>
          Runtime.default.unsafe.runToFuture(
            analyticsService.analyzeNeighborhood(addresses)
          ).map { result =>
            Ok(Json.toJson(result))
          }.recover {
            case e: Exception =>
              InternalServerError(Json.obj("error" -> e.getMessage))
          }
        }
        
      case JsError(errors) =>
        Future.successful(
          BadRequest(Json.obj("error" -> "Invalid address list format"))
        )
    }
  }
  
  def streamPriceAlerts(threshold: Double): Action[AnyContent] = Action {
    val publisher: Publisher[PriceAlert] = Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.run(
        analyticsService.streamPriceAlerts(threshold).toPublisher
      ).getOrThrow()
    }
    
    Ok.chunked(
      Source.fromPublisher(publisher)
        .map(alert => ByteString(s"data: ${toJson(alert)}\n\n"))
    ).as(ContentTypes.EVENT_STREAM)
  }
  
  def startMarketTrendProcessing(): Action[AnyContent] = Action.async {
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.runToFuture(
        analyticsService.startMarketTrendProcessing
      ).map(_ => Ok(Json.obj("status" -> "Market trend processing started")))
    }
  }
} 
