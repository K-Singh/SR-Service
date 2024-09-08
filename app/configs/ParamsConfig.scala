package configs

import play.api.Configuration

import scala.concurrent.duration.FiniteDuration

class ParamsConfig(config: Configuration){


    val heightInterval: Int         = config.get[Int]("params.heightInterval")
    val startHeight:    Int         = config.get[Int]("params.startHeight")
    val numClaims:      Int         = config.get[Int]("params.numClaims")

    val enableStorageRent: Boolean  = config.get[Boolean]("params.enableStorageRent")
    val collectionLimit:   Int      = config.get[Int]("params.collectionLimit")

    val apiWait:        FiniteDuration  = config.get[FiniteDuration]("params.apiWaitTime")
}
