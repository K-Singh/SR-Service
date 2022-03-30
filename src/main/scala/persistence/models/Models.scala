package persistence.models

import java.sql.{Connection, PreparedStatement, ResultSet}
import java.time.LocalDateTime
import java.util.Date

object Models {
  abstract class DatabaseConversion[T] {
    protected def fromResultSet(resultSet: ResultSet): T

    protected def str(idx: Int)(implicit rs: ResultSet): String = {
      rs.getString(idx)
    }

    protected def long(idx: Int)(implicit rs: ResultSet): Long = {
      rs.getLong(idx)
    }

    protected def date(idx: Int)(implicit rs: ResultSet): LocalDateTime = {
      rs.getObject(idx, classOf[LocalDateTime])
    }

    protected def dec(idx: Int)(implicit rs: ResultSet): Double = {
      rs.getDouble(idx)
    }

    protected def int(idx: Int)(implicit rs: ResultSet): Int = {
      rs.getInt(idx)
    }

  }

  case class DbConn(c: Connection) {
    def state(s: String): PreparedStatement = c.prepareStatement(s)

    def close(): Unit = c.close()
  }

  case class PoolMember(subpool: String, subpool_id: Long, tx: String, box: String, g_epoch: Long, epoch: Long,
                        height: Long, miner: String, share_score: Long, share: Long, share_perc: Double,
                        minpay: Long, stored: Long, paid: Long, change: Long, epochs_mined: Long,
                        token: String, token_paid: Long, block: Long, created: LocalDateTime)

  object PoolMember extends DatabaseConversion[PoolMember] {
    override def fromResultSet(rs: ResultSet): PoolMember = {
      implicit val resultSet: ResultSet = rs
      PoolMember(str(1), long(2), str(3), str(4), long(5), long(6), long(7),
        str(8), long(9), long(10), dec(11), long(12), long(13), long(14), long(15),
        long(16), str(17), long(18), long(19), date(20))
    }
  }

  case class PoolState(subpool: String, subpool_id: Long, name: String, box: String, tx: String, g_epoch: Long, epoch: Long,
                       g_height: Long, height: Long, status: String, members: Int, block: Long, creator: String,
                       stored_id: String, stored_val: Long, updated: LocalDateTime, created: LocalDateTime)

  object PoolState extends DatabaseConversion[PoolState] {
    override def fromResultSet(rs: ResultSet): PoolState = {
      implicit val resultSet: ResultSet = rs
      PoolState(str(1), long(2), str(3), str(4), str(5), long(6), long(7),
        long(8), long(9), str(10), int(11), long(12), str(13), str(14),
        long(15), date(16), date(17))
    }

    val SUCCESS   = "success"
    val FAILURE   = "failure"
    val INITIATED = "initiated"
    val CONFIRMED = "confirmed"

  }

  case class PoolPlacement(subpool: String, subpool_id: Long, block: Long, holding_id: String, holding_val: Long,
                           miner: String, score: Long, epochs_mined: Long, amount: Long)

  object PoolPlacement extends DatabaseConversion[PoolPlacement] {
    override def fromResultSet(rs: ResultSet): PoolPlacement = {
      implicit val resultSet: ResultSet = rs
      PoolPlacement(str(1), long(2), long(3), str(4), long(5), str(6), long(7),
        long(8), long(9))
    }
  }

}
