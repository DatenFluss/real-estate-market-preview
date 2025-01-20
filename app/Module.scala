import com.google.inject.AbstractModule
import play.api.{Configuration, Environment}
import services._

class Module(environment: Environment, configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[ZillowService]).asEagerSingleton()
    bind(classOf[AnalyticsService]).asEagerSingleton()
    bind(classOf[SparkService]).asEagerSingleton()
  }
} 
