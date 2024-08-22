package configs



import org.ergoplatform.appkit.{ErgoClient, ErgoProver, NetworkType, RestApiErgoClient, SecretStorage}
import org.ergoplatform.explorer.client.ExplorerApiClient
import org.ergoplatform.restapi.client.ApiClient
import play.api.Configuration
import play.api.libs.ws.WSClient
import utils.ExplorerDataSource

class NodeConfig(config: Configuration) {
  private val nodeURL: String = config.get[String]("node.url")
  private val nodeKey: String = config.get[String]("node.key")

  private val storagePath: String = config.get[String]("node.storagePath")
  private val password:    String = config.get[String]("node.pass")
  private val networkType: NetworkType = NetworkType.valueOf(config.get[String]("node.networkType"))

  private var explorerURL: String = config.get[String]("node.explorerURL")



  if(explorerURL == "default")
    explorerURL = RestApiErgoClient.getDefaultExplorerUrl(networkType)

  private val ergoClient: ErgoClient  = RestApiErgoClient.create(nodeURL, networkType, nodeKey, getExplorerURL)
  val apiClient                       = new ApiClient(nodeURL, "ApiKeyAuth", nodeKey)
  val explorerClient                  = new ExplorerApiClient(explorerURL)




  def getNetwork: NetworkType   = networkType
  def getExplorerURL: String    = explorerURL
  def getClient: ErgoClient     = ergoClient
  def getNodeURL: String        = nodeURL
  def getExplorer(wsClient: WSClient)     = new ExplorerDataSource(apiClient, explorerClient, wsClient)

}
