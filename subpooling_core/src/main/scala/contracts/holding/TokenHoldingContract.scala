package io.getblok.subpooling_core
package contracts.holding

import boxes.BoxHelpers
import boxes.builders.{CommandOutputBuilder, HoldingOutputBuilder, HoldingSetBuilder}
import global.AppParameters
import logging.LoggingHandler
import registers.{PoolFees, PropBytes, ShareDistribution}
import transactions.{CreateCommandTx, DistributionTx}

import io.getblok.subpooling_core.contracts.Models.Scripts
import org.ergoplatform.appkit._
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.slf4j.{Logger, LoggerFactory}
import sigmastate.eval.Colls


/**
 * Holding Contract that works the same as SimpleHoldingContract, except no dusting requirements, and usage of
 * tokens rather than ERG value
 * @param holdingContract ErgoContract to build SimpleHoldingContract from.
 */
class TokenHoldingContract(holdingContract: ErgoContract) extends HoldingContract(holdingContract) {
  val logger: Logger = LoggerFactory.getLogger("TokenHoldingContract")
  final val MIN_PAYMENT_THRESHOLD = Parameters.OneErg / 10L // TODO: Make this an AppParameter


  override def applyToCommand(commandTx: CreateCommandTx): CommandOutputBuilder = {
    val metadataBox = commandTx.metadataInputBox
    val storedPayouts = metadataBox.shareDistribution.dist.map(d => d._2.getStored).sum

    val holdingBoxes = commandTx.holdingInputs

    val currentDistribution = commandTx.cOB.metadataRegisters.shareDist
    val lastDistribution = metadataBox.shareDistribution

    val holdingBoxTokens = holdingBoxes.foldLeft(0L){
      (accum: Long, box: InputBox) =>
        accum + box.getTokens.get(0).getValue
    }

    val currentPoolFees = metadataBox.getPoolFees
    val currentTxFee = Parameters.MinFee * currentDistribution.size

    val totalOwedPayouts =
      lastDistribution.filter(c => c._2.getStored < c._2.getMinPay).dist.map(c => c._2.getStored).sum
    // Removed 7 tokens to help with change, should find better solution later
    val totalRewards = holdingBoxTokens - totalOwedPayouts - 8
    val feeList = currentPoolFees.fees.map{
      // Pool fee is defined as x/100000 of total inputs value.
      poolFee =>
        val feeAmount: Long = ((BigInt(poolFee._2.toLong) * totalRewards)/PoolFees.POOL_FEE_CONST.toLong).toLong

        (poolFee._1 , feeAmount)
    }
    // Total amount in holding after pool fees and tx fees.
    // This is the total amount of tokens to be distributed to pool members
    val totalValAfterFees = (feeList.toArray.foldLeft(totalRewards){
      (accum, poolFeeVal) => accum - poolFeeVal._2
    })
    val totalShares = currentDistribution.dist.map(d => d._2.getScore).sum
    logger.info("Now updating consensus")
    var shareScoreLeft = 0L
    var updatedConsensus = currentDistribution.dist
    lastDistribution.dist.foreach{
      ld =>
        if(ld._2.getStored > 0 && !updatedConsensus.exists(c => c._1.address.toString == ld._1.address.toString)){
          updatedConsensus = updatedConsensus ++ Seq(ld._1 -> ld._2.withScore(0L).withMinPay((0.001 * Parameters.OneErg).toLong / 10)
            .withStored(0L).withEpochs(-1))
        }
    }

    updatedConsensus = updatedConsensus.map{
      consVal =>
        val shareNum = consVal._2.getScore
        var currentMinPayout = consVal._2.getMinPay
        logger.info(s"Share score for ${consVal._1.address}: $shareNum")
        var valueFromShares = ((BigInt(totalValAfterFees) * BigInt(shareNum)) / totalShares).toLong
        if(currentMinPayout == (0.001 * Parameters.OneErg).toLong){
          currentMinPayout = currentMinPayout / 10 // Divide by 10 to get fractional NETA shares
        }

        logger.info("Member: " + consVal._1.address)
        logger.info("Value from shares: " + valueFromShares)

        logger.info("Current Min Payout: " + currentMinPayout)
        logger.info(s"Score: ${consVal._2.getScore}")
        val owedPayment =
          if(lastDistribution.dist.exists(sc => consVal._1 == sc._1)){
            val lastConsValues = lastDistribution.filter(sc => consVal._1 == sc._1 ).head._2
            val lastStoredPayout = lastConsValues.getStored
//            println("Last Stored Payout: " + lastStoredPayout)
            if(lastStoredPayout + valueFromShares >= currentMinPayout)
              0L
            else{
              lastStoredPayout + valueFromShares
            }
          }else{
            if(valueFromShares >= currentMinPayout)
              0L
            else{
              valueFromShares
            }
          }
        logger.info(s"Owed Payment: $owedPayment")
        val newConsensusInfo = consVal._2.withStored(owedPayment).withMinPay(currentMinPayout)
        (consVal._1, newConsensusInfo)
    }.filter{
      c =>
        // If value from shares addition causes error, add 1 share score to help move payouts out
        val oldMember = lastDistribution.dist.find(_._1.address.toString == c._1.address.toString)
        logger.info("Checking epoch assumptions")
        logger.info(s"Old member for assumption: ${oldMember}")
        if(oldMember.isDefined) {
          val epochAssumption = !(c._2.getScore == 0 && c._2.getStored == 0) ||
            (c._2.getMinPay == ((0.001 * Parameters.OneErg).toLong / 10) && oldMember.get._2.getMinPay != ((0.001 * Parameters.OneErg).toLong / 10))
          logger.info(s"epochAssumption for miner ${c._1.address}: ${epochAssumption}")
          epochAssumption
        }else {
          !(c._2.getScore == 0 && c._2.getStored == 0 &&
            c._2.getMinPay != ((0.001 * Parameters.OneErg).toLong / 10))
        }
//        if(oldMember.isDefined)
//          true
//        else
//          false
    }
    val distinctConsensus = updatedConsensus.map(c => c._1.address.toString).toSeq.distinct
    updatedConsensus = distinctConsensus.map(d => updatedConsensus.find(uc => uc._1.address.toString == d).get).toMap

    logger.info(s"Updated consensus length: ${updatedConsensus.size}")
    logger.info(s"Distinct consensus length: ${distinctConsensus.size}")
  //  logger.info(s"Next consensus length: ${nextConsensus.size}")
    val newShareDistribution = new ShareDistribution(updatedConsensus.toMap)
    val newMetadataRegisters = commandTx.cOB.metadataRegisters.copy(shareDist = newShareDistribution)

    commandTx.cOB.setMetadata(newMetadataRegisters)
  }

  /**
   * Generates a HoldingOutputBuilder that follows consensus.
   * @param ctx Blockchain context
   * @return Returns HoldingOutputBuilder to use in transaction
   */
  override def generateInitialOutputs(ctx: BlockchainContext, distributionTx: DistributionTx, holdingBoxes: List[InputBox]): HoldingOutputBuilder = {
    implicit val networkType: NetworkType = AppParameters.networkType
    logger.info("Now generating initial holding outputs for SimpleHoldingContract")
    val metadataBox = distributionTx.metadataInputBox
    val commandBox = distributionTx.commandInputBox
    val holdingAddress = distributionTx.holdingContract.toAddress
    val initBoxes: List[InputBox] = List(metadataBox.asInput, commandBox.asInput)
    val inputList = initBoxes++holdingBoxes
    val inputBoxes: Array[InputBox] = inputList.toArray
    val distributionTokenId = holdingBoxes.head.getTokens.get(0).getId
    val feeAddresses = metadataBox.getPoolFees.fees.map(c => c._1.address)

    val holdingBytes = PropBytes.ofAddress(holdingAddress)
    val totalTokenValue = inputBoxes.foldLeft(0L){
      (accum: Long, box: InputBox) =>
        val boxPropBytes = PropBytes.ofErgoTree(box.getErgoTree)
        if(boxPropBytes == holdingBytes){
          accum + box.getTokens.get(0).getValue
        }else
          accum
    } + 4
    logger.info("Total Token Value Held: " + totalTokenValue)

    val lastConsensus = metadataBox.shareDistribution
    val currentConsensus = commandBox.shareDistribution
    val currentPoolFees = metadataBox.poolFees


    val totalOwedPayouts =
      lastConsensus.filter(c => c._2.getStored < c._2.getMinPay).dist.map(c => c._2.getStored).sum
    val totalRewards = totalTokenValue - totalOwedPayouts - 8
    logger.info(s"Total owed payouts: ${totalOwedPayouts}")

    val feeList = currentPoolFees.fees.map{
      f =>
        val feeAmount = ((BigInt(f._2.toLong) * totalRewards) / PoolFees.POOL_FEE_CONST).toLong

        (f._1, feeAmount)
    }

    // Total amount in holding after pool fees and tx fees.
    // This is the total amount of ERG to be distributed to pool members
    val totalValAfterFees = (feeList.toArray.foldLeft(totalRewards){
      (accum: Long, poolFeeVal: (PropBytes, Long)) => accum - poolFeeVal._2
    })
    logger.info(s"Total Value After Fees: $totalValAfterFees")
    val totalShares = currentConsensus.dist.map(c => c._2.getScore).sum

    // Returns some value that is a percentage of the total rewards after the fees.
    // The percentage used is the proportion of the share number passed in over the total number of shares.
    def getValueFromShare(shareNum: Long) = {
      if(totalShares != 0) {
        val newBoxValue = ((BigInt(totalValAfterFees) * BigInt(shareNum)) / totalShares).toLong
        newBoxValue
      }else
        0L
    }


    // Maps each propositionBytes stored in the consensus to a value obtained from the shares.
    val boxValueMap = currentConsensus.dist.map{
      consVal =>

        val shareNum = consVal._2.getScore
        val currentMinPayout = consVal._2.getMinPay
        val valueFromShares = getValueFromShare(shareNum)
        logger.info("Current member in boxValueMap")
        logger.info(consVal._1.address.toString + s": ${consVal._2}")
        logger.info(s"Value From Shares: $valueFromShares")
        //println("Value From Shares: " + valueFromShares)
        if(lastConsensus.dist.exists(sc => consVal._1 == sc._1)){
          val lastConsValues = lastConsensus.filter(sc => consVal._1 == sc._1).head._2
          val lastStoredPayout = lastConsValues.getStored
          logger.info(s"Last stored payout: ${lastStoredPayout}")
          logger.info(s"Value from shares: ${valueFromShares}")
          logger.info(s"Total sum: ${lastStoredPayout + valueFromShares}")
          logger.info(s"Is greater than minPay? ${(lastStoredPayout + valueFromShares) >= currentMinPayout}")
          if(lastStoredPayout + valueFromShares >= currentMinPayout) {

            (consVal._1, lastStoredPayout + valueFromShares)
          } else{

            (consVal._1, 0L)
          }
        }else{
          if(valueFromShares >= currentMinPayout) {
            //println("This new value was higher than min payout" + valueFromShares + " | " + currentMinPayout)
            (consVal._1, valueFromShares)
          } else{
            //println("This new value was lower than min payout: " + valueFromShares + " | " + currentMinPayout)

            (consVal._1, 0L)
          }
        }
    }
    val changeValue =
      currentConsensus.filter(c => c._2.getStored < c._2.getMinPay).dist.map(c => c._2.getStored).sum

    var holdingBuilders = Array.empty[HoldingSetBuilder]
    logger.info(s"Total change value ${changeValue}")

    val amountPaid = boxValueMap.values.sum
    val otherChange = totalTokenValue - amountPaid
    logger.info(s"Other change value: ${otherChange}")
    boxValueMap.foreach{
      c =>
        logger.info(s"Current Address: ${c._1.address.toString}")
        logger.info(s"Current stored value: ${currentConsensus.dist(c._1).getStored}")
        logger.info(s"Current paid value: ${c._2}")
        logger.info(s"Current score ${currentConsensus.dist(c._1).getScore}")
        logger.info(s"Current min pay: ${currentConsensus.dist(c._1).getMinPay}")
        logger.info(s"Is valid ${
          if(c._2 > currentConsensus.dist(c._1).getMinPay && currentConsensus.dist(c._1).getStored == 0){
            true.toString
          }else if(c._2 == 0 && currentConsensus.dist(c._1).getStored < currentConsensus.dist(c._1).getMinPay){
            true.toString
          }else{
            false.toString
          }
        }")
    }

    boxValueMap.foreach{
      c =>
        val addr = c._1.address
        val addrBytes = c._1.arr

        logger.info(s" Value from shares for address ${addr}: ${c._2}")
        if(c._2 > 0L) {
          val outB = distributionTx.asUnsignedTxB.outBoxBuilder()
          val holdingBuilder = new HoldingSetBuilder(outB)
          val setBuilder = holdingBuilder
            .value(Parameters.MinFee)
            .contract(new ErgoTreeContract(addr.getErgoAddress.script, addr.getNetworkType))
            .tokens(new ErgoToken(distributionTokenId, c._2))
            .forMiner(true)
          holdingBuilders = holdingBuilders++Array(setBuilder)
        }
    }
    feeAddresses.foreach{
      (addr: Address) =>
        val outB = new HoldingSetBuilder(distributionTx.asUnsignedTxB.outBoxBuilder())
        val addrBytes = PropBytes.ofAddress(addr)
        val boxValue = feeList.filter(f => f._1 == addrBytes).head
        if(boxValue._2 > 0) {
          logger.info(s"Fee Value for address ${addr}: ${boxValue._2}")
          val holdingBuilder = outB
            .value(Parameters.MinFee)
            .tokens(new ErgoToken(distributionTokenId, boxValue._2))
            .contract(new ErgoTreeContract(addr.getErgoAddress.script, addr.getNetworkType))
          holdingBuilders = holdingBuilders++Array(holdingBuilder)
        }
    }

    if(changeValue > 0) {
      val outB = new HoldingSetBuilder(distributionTx.asUnsignedTxB.outBoxBuilder())
      val holdingBuilder = outB
        .value(Parameters.MinFee)
        .tokens(new ErgoToken(distributionTokenId, changeValue))
        .contract(new ErgoTreeContract(holdingAddress.getErgoAddress.script, holdingAddress.getNetworkType))
      holdingBuilders = holdingBuilders++Array(holdingBuilder)
    }
    new HoldingOutputBuilder(holdingBuilders)
  }



}
object TokenHoldingContract {

  val script: String = Scripts.TOKEN_HOLDING_SCRIPT

  /**
   * Generates Holding Contract with given constants
   * @param ctx Blockchain context used to generate contract
   * @param metadataAddress address of metadata
   * @return Compiled ErgoContract of Holding Smart Contract
   */
  def generateHoldingContract(ctx: BlockchainContext, metadataAddress: Address, subpoolToken: ErgoId): HoldingContract = {
    val metadataPropBytes: PropBytes = PropBytes.ofAddress(metadataAddress)(ctx.getNetworkType)
    val subpoolTokenBytes = Colls.fromArray(subpoolToken.getBytes)
    val constantsBuilder = ConstantsBuilder.create()

    val compiledContract = ctx.compileContract(constantsBuilder
      .item("const_metadataPropBytes", metadataPropBytes.coll)
      .item("const_smartPoolNFT", subpoolTokenBytes)
      .build(), script)
    new TokenHoldingContract(compiledContract)
  }
  //
  //
  //
  //
  def getTxFee(dist: ShareDistribution): Long = {
    0L
  }

  def getValAfterFees(totalRewards: Long, txFee: Long, poolFees: PoolFees): Long = {
    val feeList = poolFees.fees.map{
      // Pool fee is defined as x/100000 of total inputs value.
      poolFee =>
        val feeAmount: Long = ((BigInt(poolFee._2.toLong) * totalRewards)/PoolFees.POOL_FEE_CONST.toLong).toLong
        (poolFee._1 , feeAmount)
    }
    // Total amount in holding after pool fees and tx fees.
    // This is the total amount of ERG to be distributed to pool members
    val totalValAfterFees = (feeList.toArray.foldLeft(totalRewards){
      (accum, poolFeeVal) => accum - poolFeeVal._2
    })
    totalValAfterFees
  }


  def getBoxValue(shareNum: Long, totalShares: Long, totalValueAfterFees: Long): Long = {
    if(totalShares != 0) {
      val boxValue = ((BigInt(totalValueAfterFees) * BigInt(shareNum)) / BigInt(totalShares)).toLong
      boxValue
    } else
      0L
  }



}

