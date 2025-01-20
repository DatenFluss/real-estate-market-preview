package services

import org.apache.spark.sql._
import javax.inject._
import zio._

@Singleton
class SparkService @Inject()() {
  private lazy val spark = SparkSession.builder()
    .appName("Real Estate Analytics")
    .config("spark.master", "local[*]")
    .getOrCreate()

  def analyzeMarketTrends(properties: Seq[PropertyData]): Task[MarketAnalysis] = {
    ZIO.attempt {
      val propertyDF = spark.createDataFrame(properties)
      
      val avgPrice = propertyDF.select("zestimate").agg(functions.avg("zestimate")).first().getDouble(0)
      val avgRent = propertyDF
        .filter(functions.col("rentZestimate").isNotNull)
        .select("rentZestimate")
        .agg(functions.avg("rentZestimate"))
        .first()
        .getDouble(0)
      
      MarketAnalysis(
        averagePrice = avgPrice,
        averageRent = avgRent,
        priceToRentRatio = avgPrice / avgRent,
        sampleSize = properties.size
      )
    }
  }

  def shutdown(): Task[Unit] = ZIO.attempt(spark.stop())
}

case class MarketAnalysis(
    averagePrice: Double,
    averageRent: Double,
    priceToRentRatio: Double,
    sampleSize: Int
) 
