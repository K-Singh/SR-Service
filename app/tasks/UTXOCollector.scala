package tasks


import akka.actor.ActorSystem
import com.google.gson.stream.JsonReader
import configs.TasksConfig.TaskConfiguration
import configs.{Contexts, NodeConfig, ParamsConfig, TasksConfig}
import org.ergoplatform.appkit.ErgoClient
import play.api.libs.ws.WSClient
import play.api.{Configuration, Logger}
import utils.ConcurrentBoxLoader

import javax.inject.{Inject, Singleton}
import scala.language.{higherKinds, postfixOps}

@Singleton
class UTXOCollector @Inject()(WSClient: WSClient, system: ActorSystem, config: Configuration) {


  val logger: Logger = Logger("UTXO Collector")
  val tasks = new TasksConfig(config)
  val taskConfig: TaskConfiguration = tasks.utxoCollectorConfig
  val nodeConfig: NodeConfig        = new NodeConfig(config)
  val client: ErgoClient = nodeConfig.getClient
  val explorer = nodeConfig.getExplorer(WSClient)

  val contexts: Contexts = new Contexts(system)
  val params: ParamsConfig = new ParamsConfig(config)
  final val FOUR_YEARS     = 1051200 // Number of blocks mined in 4 years

  if(taskConfig.enabled) {

    var start = params.startHeight
    var end = start + params.heightInterval
    logger.info(s"UTXO Collection will start in ${taskConfig.startup.toString()} with an interval of" +
      s" ${taskConfig.interval}")
    system.scheduler.scheduleWithFixedDelay(initialDelay = taskConfig.startup, delay = taskConfig.interval)({
      () =>
        logger.info("Collecting utxos...")
        val boxIds = explorer.getUnspentBoxIdsByHeight(start, end, params.apiWait)
        logger.info(s"Found ${boxIds.size} boxes available to spend between heights ${start} and ${end}")
        //logger.info(s"Box Ids: ${boxIds.mkString("(", ", " , ")")}")

        ConcurrentBoxLoader.loadUTXOs(boxIds)

        client.execute{
          ctx =>
            end = Math.min(end + params.heightInterval, ctx.getHeight - FOUR_YEARS)
        }
        logger.info(s"Chose new ending height ${end}")
        start = end - params.heightInterval
        logger.info(s"Chose new starting height ${start}")

        logger.info("Finished collecting utxos")
    })(contexts.taskContext)
  }



}
