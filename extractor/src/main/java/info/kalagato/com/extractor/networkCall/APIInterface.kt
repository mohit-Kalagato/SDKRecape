package info.kalagato.com.extractor.networkCall

import retrofit2.Response
import retrofit2.http.GET

interface APIInterface {

    @GET("json")
    suspend fun getIpLocation():Response<IpResponse>



}