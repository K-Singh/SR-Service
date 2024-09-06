package utils

object UTXOCache {

  private var utxoSet: Option[Seq[String]] = None
  private var responseCache: Option[String] = None
  def hasSet: Boolean = utxoSet.getOrElse(Seq.empty[String]).nonEmpty


  def getResponseCache: Option[String] = responseCache
  def setResponseCache(s: String): Unit = this.responseCache = Some(s)
  def isResponseCached(resp: String): Boolean = responseCache.exists(s => s.equals(resp))

  def getCache: Option[Seq[String]] = utxoSet
  def setCache(utxos: Seq[String]): Unit = this.utxoSet = Some(utxos)
  def dump(): Unit = this.utxoSet = None
  def isCached(utxos: Seq[String]): Boolean = {
    if(utxoSet.isDefined){
      utxoSet.get == utxos
    }else{
      false
    }

  }
}
