//
//package controllers
//
//import org.ergoplatform.appkit.{Address, Eip4Token, ErgoId, ErgoToken, Parameters}
//import play.api.{Configuration, Logger}
//import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
//
//import java.time.LocalDateTime
//import javax.inject.{Inject, Named, Singleton}
//import scala.collection.mutable.ArrayBuffer
//import _root_.io.swagger.annotations._
//import actors.BlockingDbWriter.{InsertNewPoolInfo, UpdatePoolInfo}
//import actors.DbConnectionManager.NewConnectionRequest
//import actors.QuickDbReader
//import akka.actor.{ActorRef, ActorSystem}
//import akka.pattern.ask
//import akka.util.Timeout
//import models.ResponseModels._
//import io.getblok.subpooling_core.groups.{GenesisGroup, GroupManager}
//import io.getblok.subpooling_core.groups.builders.GenesisBuilder
//import io.getblok.subpooling_core.groups.entities.{Pool, Subpool}
//import io.getblok.subpooling_core.groups.selectors.EmptySelector
//import io.getblok.subpooling_core.persistence.models.Models.{DbConn, MinerSettings, PoolBlock, PoolInformation, PoolMember, PoolPlacement, PoolState}
//import play.api.libs.json.{Json, Writes}
//import actors.QuickDbReader._
//import io.getblok.subpooling_core.contracts.MetadataContract
//import io.getblok.subpooling_core.contracts.emissions.EmissionsContract
//import io.getblok.subpooling_core.contracts.holding.TokenHoldingContract
//import io.getblok.subpooling_core.global.{AppParameters, Helpers}
//import io.getblok.subpooling_core.persistence.models.DataTable
//import io.getblok.subpooling_core.registers.PoolFees
//import models.InvalidIntervalException
//import models.ResponseModels.Intervals.{DAILY, HOURLY, MONTHLY, YEARLY}
//import persistence.Tables
//import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
//import slick.ast.TypedType
//import slick.jdbc.PostgresProfile
//
//import scala.collection.JavaConverters.{collectionAsScalaIterableConverter, seqAsJavaListConverter}
//import scala.concurrent.{Await, ExecutionContext, Future}
//import scala.concurrent.duration.{DurationDouble, DurationInt}
//import scala.language.postfixOps
//import scala.util.{Failure, Success, Try}
//
//@Api(value = "/pools", description = "Pool operations")
//@Singleton
//class PoolController @Inject()(@Named("quick-db-reader") quickQuery: ActorRef, @Named("blocking-db-writer") slowWrite: ActorRef,
//                               components: ControllerComponents, system: ActorSystem, config: Configuration,
//                               override protected val dbConfigProvider: DatabaseConfigProvider)
//                               extends SubpoolBaseController(components, config) with HasDatabaseConfigProvider[PostgresProfile] {
//
//  val log: Logger = Logger("PoolController")
//  val quickQueryContext: ExecutionContext = system.dispatchers.lookup("subpool-contexts.quick-query-dispatcher")
//  val slowWriteContext: ExecutionContext = system.dispatchers.lookup("subpool-contexts.blocking-io-dispatcher")
//  implicit val timeOut: Timeout = Timeout(15 seconds)
//  import dbConfig.profile.api._
//
//  log.info("Initiating pool controller")
///*  @ApiOperation(
//    value = "Creates a new set of pools",
//    notes = "Returns PoolGenerated response",
//    httpMethod = "GET"
//  )
//  @ApiResponses(Array(
//    new ApiResponse(code = 200, response = classOf[PoolGenerated], message = "Success"),
//    new ApiResponse(code = 500, message = "An error occurred while generating the pool")
//  ))*/
//
///*  // TODO: Make this part of task
//  def updatePoolInfo(tag: String): Action[AnyContent] = Action {
//    implicit val ec: ExecutionContext = slowWriteContext
//    val fPoolStates = quickQuery ? QueryAllSubPools(tag)
//    val fPoolMembers = quickQuery ? AllPoolMembers(tag)
//
//    for{
//      states <- fPoolStates.mapTo[Seq[PoolState]]
//      members <- fPoolMembers.mapTo[Seq[PoolMember]]
//    } yield slowWrite ! UpdatePoolInfo(tag, states.head.g_epoch, states.maxBy(s => s.block).block, members.count(m => m.g_epoch == states.head.g_epoch),
//      states.map(s => s.stored_val).sum, members.map(m => m.paid).sum)
//
//    Ok(s"Pool Information for pool ${tag} was updated")
//  }
//
//  def insertDefaultInfo(tag: String, numSubpools: Long, title: String, creator: String): Action[AnyContent] = Action.async {
//    val poolInformation = PoolInformation(tag, 0L, numSubpools, 0L, 0L, 0L, 0L, PoolInformation.CURR_ERG, PoolInformation.PAY_PPLNS,
//      100000L, official = true, 5L, 10L, title, creator, LocalDateTime.now(), LocalDateTime.now())
//
//    val writePool = slowWrite ? InsertNewPoolInfo(poolInformation)
//    writePool.mapTo[Long].map{
//      r =>
//        if(r > 0)
//          okJSON(poolInformation)
//        else
//          InternalServerError("There was an error writing information for the pool")
//    }(slowWriteContext)
//  }*/
//
//
//
//
//
//
///*
//  def addMiner(tag: String, miner: String): Action[AnyContent] = Action {
//
//    val tryUpdate = Try(settingsTable.updateMinerPool(miner, tag))
//    if(tryUpdate.getOrElse(0L) == 1L)
//      Ok(s"Miner $miner was added to pool with tag $tag")
//    else
//      InternalServerError("ERROR 500: An internal server error occurred while adding the miner.")
//  }
//*/
//
//
//}
