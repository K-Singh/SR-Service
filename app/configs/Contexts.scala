package configs

import akka.actor.ActorSystem

import javax.inject.Inject
import scala.concurrent.ExecutionContext


class Contexts (system: ActorSystem) {
  private val prefix = "sr-contexts."
  val apiContext:       ExecutionContext = system.dispatchers.lookup(prefix+"api-dispatcher")
  val taskContext:      ExecutionContext = system.dispatchers.lookup(prefix+"task-dispatcher")
}
