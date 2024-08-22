package utils

import org.slf4j.{Logger, LoggerFactory}

import java.util.concurrent.ConcurrentLinkedQueue
import scala.language.postfixOps


object ConcurrentBoxLoader {
  val logger: Logger = LoggerFactory.getLogger("ConcurrentBoxLoader")
  val loadedBoxes: ConcurrentLinkedQueue[String] = new ConcurrentLinkedQueue[String]()

  // Load box ids into concurrent queue
  def loadUTXOs(utxos: Seq[String]) = {
    utxos.foreach(loadedBoxes.add)
    logger.info(s"Loaded ${utxos.size} utxos into queue")
    logger.info("Loaded utxos into concurrent queue")
  }

  def poll(amnt: Int) = {
    for(i <- 0 to amnt) yield Option(loadedBoxes.poll())
  }

  def size = loadedBoxes.size()
}
