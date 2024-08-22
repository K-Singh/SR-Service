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
import utils.ConcurrentBoxLoader

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

class BlockRelayController @Inject()(ws: WSClient, system: ActorSystem, val controllerComponents: ControllerComponents, config: Configuration)
extends BaseController{

  val nodeConfig    = new NodeConfig(config)
  val paramsConfig  = new ParamsConfig(config)

  val client: ErgoClient = nodeConfig.getClient
  val logger: Logger = Logger("API")

  // States
  def getBlockTemplateSR: Action[AnyContent] = Action.async {
    implicit val apiContext: ExecutionContext = system.dispatchers.lookup("sr-contexts.api-dispatcher")
    val utxoObj = Json.toJson(queryRentUtxos)
    val post = ws.url(nodeConfig.getNodeURL+"/mining/candidateWithRent").post(utxoObj)
    logger.info(utxoObj.toString())
    logger.info(Await.result(post, 10.seconds).body)
    post.map(ws => Ok(ws.json))

  }


  def queryRentUtxos: Seq[String] = {
    val polledUtxos = ConcurrentBoxLoader.poll(paramsConfig.numClaims).flatten
    var allTokens   = Map.empty[ErgoId, Long]
    client.execute{
      ctx =>
        val utxos = ctx.getBoxesById(polledUtxos: _*)
        utxos.foreach{
          u =>
            u.getTokens.forEach{
              t =>
                allTokens = allTokens + (t.id -> (allTokens.getOrElse(t.id, 0L) + t.value))
            }
        }
    }
    logger.info(s"Polled UTXOs: ${polledUtxos.mkString(", ")}")
    if(allTokens.size < 256)
      polledUtxos
    else
      polledUtxos.take(paramsConfig.numClaims/2)
  }

  def okJSON[T](o: T)(implicit tjs: Writes[T]): Result = {
    Ok(Json.prettyPrint(Json.toJson(o)))
  }
}
