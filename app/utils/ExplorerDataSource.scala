package utils

import okhttp3.OkHttpClient
import org.ergoplatform.appkit.{Address, ErgoClientException, InputBox, RestApiErgoClient}
import org.ergoplatform.appkit.impl.NodeAndExplorerDataSourceImpl
import org.ergoplatform.explorer.client.{DefaultApi, ExplorerApiClient}
import org.ergoplatform.explorer.client.model.OutputInfo
import org.ergoplatform.restapi.client.ApiClient
import play.api.Logger
import play.api.libs.ws.WSClient
import retrofit2.{Call, Response, Retrofit}

import java.util
import java.util.List
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}

class ExplorerDataSource(nodeClient: ApiClient, explorerClient: ExplorerApiClient, wsClient: WSClient)
  extends NodeAndExplorerDataSourceImpl(nodeClient, explorerClient){
  val logger: Logger = Logger("ExplorerDataSource")
  val okExplorer: OkHttpClient = explorerClient.getOkBuilder.build
  val retrofitExplorer: Retrofit = explorerClient.getAdapterBuilder.client(okExplorer).build
  val explorerApi: DefaultApi = retrofitExplorer.create(classOf[DefaultApi])

  @throws[ErgoClientException]
  def executeRawCall[T](apiCall: Call[T]): String = try {
    val response = apiCall.execute
    if (!response.isSuccessful) throw new ErgoClientException(response.code + ": " + (if (response.errorBody != null) response.errorBody.string
    else "Server returned error"), null)
    else response.toString
  } catch {
    case e: Exception =>
      throw new ErgoClientException(String.format("Error executing API request to %s: %s", apiCall.request.url, e.getMessage), e)
  }
  def getUnspentBoxIdsByHeight(minHeight: Int, maxHeight: Int, waitTime: FiniteDuration): Seq[String] = {
    val response =
      wsClient
      .url(RestApiErgoClient.defaultTestnetExplorerUrl+"/api/v1/boxes/unspent/stream")
      .withQueryStringParameters("minHeight" -> minHeight.toString, "maxHeight" -> maxHeight.toString)
      .get()
    val boxResponse = Await.result(response, waitTime).body

    var boxIds = Seq.empty[String]
    var lastIdx = 0
    while(boxResponse.indexOf("\"boxId\":\"", lastIdx) != -1){

      val idx = boxResponse.indexOf("\"boxId\":\"", lastIdx) + 9
      boxIds = boxIds ++ Seq(boxResponse.substring(idx, idx+64))
      logger.info(s"Adding id ${boxResponse.substring(idx, idx+64)}")
      lastIdx = idx+64
    }


    boxIds
  }

}
