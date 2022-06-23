package persistence

import io.getblok.subpooling_core.persistence.models.Models.Share
import models.DatabaseModels.PoolShare
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

import java.time.LocalDateTime

class PoolSharesTable(tag: Tag) extends Table[PoolShare](tag, "pool_shares") {
  def poolId            = column[String]("poolid")
  def blockHeight       = column[Long]("blockheight")
  def difficulty        = column[Double]("difficulty")
  def networkDifficulty = column[Double]("networkdifficulty")
  def miner             = column[String]("miner")
  def worker            = column[Option[String]]("worker")
  def userAgent         = column[Option[String]]("useragent")
  def ipAddress         = column[String]("ipaddress")
  def source            = column[Option[String]]("source")
  def created           = column[LocalDateTime]("created")
  def poolTag           = column[String]("pool_tag")
  def *                 = (poolId, blockHeight, miner, worker, difficulty, networkDifficulty,
    userAgent, ipAddress, source, created, poolTag) <> (PoolShare.tupled, PoolShare.unapply)
}


