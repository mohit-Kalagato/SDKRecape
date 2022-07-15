package info.kalagato.com.extractor.networkCall

import info.kalagato.com.extractor.BuildConfig
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object ApiClient {
    private const val BASE_URL = "http://ip-api.com/"
    private const val BASE_URL_1 = "https://backend.kalagato.co/api/v1/extractor/"
    private var apiInterface: APIInterface? = null
    private var retrofit: Retrofit? = null

    @JvmStatic
    val client: APIInterface
         get() {

            if (retrofit == null) {
                //val credentials: String = Credentials.basic("username", "password")
                val interceptor = HttpLoggingInterceptor()
                interceptor.level = HttpLoggingInterceptor.Level.BODY
                val okHttpClient = OkHttpClient.Builder()
                    .readTimeout(6000, TimeUnit.SECONDS)
                    .connectTimeout(6000, TimeUnit.SECONDS)
                if (BuildConfig.DEBUG) okHttpClient.addInterceptor(interceptor)
                    .addInterceptor(Interceptor { chain ->
                        val original: Request = chain.request()

                        val request: Request = original.newBuilder()
                            //.header("Authorization", credentials)
                            .method(original.method, original.body)
                            .build()

                        return@Interceptor chain.proceed(request)
                    })

                retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient.build())
                    .build()
                apiInterface =
                    retrofit!!.create(APIInterface::class.java)
            }

            return apiInterface!!
        }

    @JvmStatic
    val client2: APIInterface
         get() {

            if (retrofit == null) {
                //val credentials: String = Credentials.basic("username", "password")
                val interceptor = HttpLoggingInterceptor()
                interceptor.level = HttpLoggingInterceptor.Level.BODY
                val okHttpClient = OkHttpClient.Builder()
                    .readTimeout(6000, TimeUnit.SECONDS)
                    .connectTimeout(6000, TimeUnit.SECONDS)
                if (BuildConfig.DEBUG) okHttpClient.addInterceptor(interceptor)
                    .addInterceptor(Interceptor { chain ->
                        val original: Request = chain.request()

                        val request: Request = original.newBuilder()
                            //.header("Authorization", credentials)
                            .method(original.method, original.body)
                            .build()

                        return@Interceptor chain.proceed(request)
                    })

                retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL_1)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient.build())
                    .build()
                apiInterface =
                    retrofit!!.create(APIInterface::class.java)
            }

            return apiInterface!!
        }


}