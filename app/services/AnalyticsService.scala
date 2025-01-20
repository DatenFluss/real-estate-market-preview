package services

import javax.inject._
import zio._
import zio.stream._

@Singleton
class AnalyticsService @Inject()(
    zillowService: ZillowService,
    sparkService: SparkService,
    kafkaService: KafkaService
) {
  def analyzeNeighborhood(addresses: List[String]): Task[NeighborhoodAnalysis] = {
    for {
      properties <- ZIO.collectAllPar(
        addresses.map(addr => zillowService.getPropertyData(addr))
      )
      validProperties = properties.flatten
      _ <- ZIO.collectAllPar(
        validProperties.map(kafkaService.streamPropertyUpdate)
      )
      marketAnalysis <- sparkService.analyzeMarketTrends(validProperties)
    } yield NeighborhoodAnalysis(
      properties = validProperties,
      marketAnalysis = marketAnalysis,
      centerLatitude = validProperties.map(_.latitude).sum / validProperties.size,
      centerLongitude = validProperties.map(_.longitude).sum / validProperties.size
    )
  }
  
  def startMarketTrendProcessing: Task[Unit] = {
    kafkaService.processMarketTrends
  }
  
  def streamPriceAlerts(threshold: Double): ZStream[Any, Throwable, PriceAlert] = {
    kafkaService.streamPriceAlerts(threshold)
  }
}

case class NeighborhoodAnalysis(
    properties: Seq[PropertyData],
    marketAnalysis: MarketAnalysis,
    centerLatitude: Double,
    centerLongitude: Double
) 
