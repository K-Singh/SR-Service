import akka.actor.Props
import akka.routing.RoundRobinPool
import com.google.inject.AbstractModule
import configs.Contexts
import play.api.{Configuration, Environment}
import play.api.inject.Binding
import play.api.libs.concurrent.AkkaGuiceSupport
import tasks.UTXOCollector
class Module(environment: Environment, configuration: Configuration) extends AbstractModule with AkkaGuiceSupport{
  @Override
  override def configure(): Unit = {
    bind[UTXOCollector](classOf[UTXOCollector]).asEagerSingleton()
  }
}
