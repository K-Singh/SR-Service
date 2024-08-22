package configs

import play.api.Configuration

class ParamsConfig(config: Configuration){


    val heightInterval: Int       = config.get[Int]("params.heightInterval")
    val startHeight:    Int       = config.get[Int]("params.startHeight")
    val numClaims:      Int       = config.get[Int]("params.numClaims")

}
