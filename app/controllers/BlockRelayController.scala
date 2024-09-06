package controllers

import akka.actor.ActorSystem
import configs._
import org.ergoplatform.appkit.ErgoClient
import org.ergoplatform.sdk.ErgoId
import play.api.{Configuration, Logger}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.{Json, Writes}
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, Result}
import slick.jdbc.PostgresProfile
import utils.{ConcurrentBoxLoader, UTXOCache}

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}
import scala.util.Try

class BlockRelayController @Inject()(ws: WSClient, system: ActorSystem, val controllerComponents: ControllerComponents, config: Configuration)
extends BaseController{

  val nodeConfig    = new NodeConfig(config)
  val paramsConfig  = new ParamsConfig(config)

  val client: ErgoClient = nodeConfig.getClient
  val logger: Logger = Logger("API")

  // States
  def getBlockTemplateSR: Action[AnyContent] = Action.async {
    implicit val apiContext: ExecutionContext = system.dispatchers.lookup("sr-contexts.api-dispatcher")
    val queriedUtxos = queryRentUtxos
    if(queriedUtxos.isEmpty)
      logger.warn("Found no utxos during query!")
    val utxoObj = Json.toJson(queriedUtxos)
    val post = ws.url(nodeConfig.getNodeURL+"/mining/candidateWithRent").post(utxoObj)

    val response = Await.result(post, 10.seconds).body
    // For response caching, to be added later
//    if(UTXOCache.isResponseCached(response)){
//      logger.warn("Received new response from Ergo Node")
//      logger.warn(response)
//    }
    post.map(ws => Ok(ws.json))

  }


  def queryRentUtxos: Seq[String] = {

    if(UTXOCache.hasSet){

      val cachedUtxos = UTXOCache.getCache.get
      client.execute{
        ctx =>
          val successfulUtxos = cachedUtxos.map(u => Try(ctx.getBoxesById(u))).filter(u => u.isSuccess).flatMap(_.get)
          if(successfulUtxos.nonEmpty){
            val withTokens = successfulUtxos.filter(!_.getTokens.isEmpty)
            //logger.info(s"Found ${withTokens.size} utxos with tokens")
           // logger.info(withTokens.map(_.getId.toString()).mkString(", "))
            if(cachedUtxos.size == successfulUtxos.size) {
              //logger.info(s"Successfully returned all ${cachedUtxos.size} cached UTXOs")
              cachedUtxos
            }else {
              logger.warn(s"Only returned ${successfulUtxos.size} of ${cachedUtxos.size} cached UTXOs")
              if(paramsConfig.numClaims - successfulUtxos.size > 0){
                logger.info(s"Now polling ${paramsConfig.numClaims - successfulUtxos.size} utxos")
                val newUtxoIds = pollUTXOs(paramsConfig.numClaims - successfulUtxos.size)
                UTXOCache.setCache(successfulUtxos.map(_.getId.toString()) ++ newUtxoIds)
                successfulUtxos.map(_.getId.toString()) ++ newUtxoIds
              }else{
                logger.error("!!!Large amount of Utxos retrieved from cache, not polling additional utxos!!!")
                successfulUtxos.map(_.getId.toString())
              }

            }
          }
          else{
            logger.warn("All cached UTXOs have been spent, now polling new UTXOs")
            pollUTXOs()
          }
      }

    }else{
      logger.warn("No cached UTXOs exist, now polling new UTXOs")
      pollUTXOs()
    }
  }

  def pollUTXOs(claims: Int = paramsConfig.numClaims) = {
    val polledUtxos = ConcurrentBoxLoader.poll(claims).flatten
    var allTokens = Map.empty[ErgoId, Long]
    var successfulIds = Seq.empty[String]
    client.execute {
      ctx =>
        // Check if utxos exist and filter ones that dont
        val successfulUtxos = polledUtxos.map(u => Try(ctx.getBoxesById(u))).filter(u => u.isSuccess).flatMap(_.get)
        //logger.debug(s"Num Existing Utxos: ${successfulUtxos.size}, Num Utxos Polled From Queue: ${polledUtxos.size}")

        successfulUtxos.foreach {
          u =>
            u.getTokens.forEach {
              t =>
                allTokens = allTokens + (t.id -> (allTokens.getOrElse(t.id, 0L) + t.value))
            }
        }

        successfulIds = successfulUtxos.map(_.getId.toString())
    }
    //logger.info(s"Polled UTXOs: ${successfulIds.mkString(", ")}")

    //logger.debug("adding polled utxos to cache")
    successfulIds = {
      if (allTokens.size < 256)
        successfulIds
      else
        successfulIds.take(paramsConfig.numClaims / 2)
    }
    UTXOCache.setCache(successfulIds)
    successfulIds
  }

  def okJSON[T](o: T)(implicit tjs: Writes[T]): Result = {
    Ok(Json.prettyPrint(Json.toJson(o)))
  }
}
