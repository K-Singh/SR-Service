package configs

import play.api.Configuration

import scala.concurrent.duration.{Duration, FiniteDuration}

class TasksConfig(config: Configuration){
  val utxoCollectorConfig:   TasksConfig.TaskConfiguration = TasksConfig.TaskConfiguration.fromConfig(config, "utxo-collector")

}

object TasksConfig {
  case class TaskConfiguration(enabled: Boolean, startup: FiniteDuration, interval: FiniteDuration)
  object TaskConfiguration {
    def fromConfig(configuration: Configuration, name: String): TaskConfiguration = {
      val isEnabled = configuration.get[Boolean](s"sr-tasks.${name}.enabled")
      val startupTime = configuration.get[FiniteDuration](s"sr-tasks.${name}.startup")
      val intervalTime = configuration.get[FiniteDuration](s"sr-tasks.${name}.interval")
      TaskConfiguration(isEnabled, startupTime, intervalTime)
    }
  }
}
