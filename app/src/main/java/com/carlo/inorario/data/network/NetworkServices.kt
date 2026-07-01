package com.carlo.inorario.data.network


import com.carlo.inorario.data.model.NewsItem
import com.carlo.inorario.data.model.MetroDeparturesResponse
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface ViaggiaTrenoService {
    @GET("cercaNumeroTrenoTrenoAutocomplete/{query}")
    suspend fun searchTrainAutocomplete(@Path("query") query: String): Response<ResponseBody>

    @GET("andamentoTreno/{originID}/{trainNumber}/{timestamp}")
    suspend fun getTrainProgressWithTimestamp(
        @Path("originID") originID: String,
        @Path("trainNumber") trainNumber: String,
        @Path("timestamp") timestamp: String
    ): Response<ResponseBody>

    @GET("andamentoTreno/{originID}/{trainNumber}")
    suspend fun getTrainProgress(
        @Path("originID") originID: String,
        @Path("trainNumber") trainNumber: String
    ): Response<ResponseBody>

    @GET("{endpoint}/{vtID}/{dateStr}")
    suspend fun getStationBoard(
        @Path("endpoint") endpoint: String, // "partenze" or "arrivi"
        @Path("vtID") vtID: String,
        @Path(value = "dateStr", encoded = true) dateStr: String
    ): Response<ResponseBody>
}

interface BackendService {
    @GET("news")
    suspend fun getNews(@Query("region") region: String?): List<NewsItem>


    @GET("metro/departures/{line}/{pdfID}")
    suspend fun getMetroDepartures(
        @Path("line") line: String,
        @Path("pdfID") pdfID: String,
        @Query("direction") direction: Int,
        @Query("time") time: String? = null
    ): MetroDeparturesResponse

    @GET("trains/{trainNumber}/reports")
    suspend fun getComfortReports(
        @Path("trainNumber") trainNumber: String
    ): Response<ResponseBody>

    @POST("trains/report")
    suspend fun postComfortReport(
        @Body body: RequestBody
    ): Response<ResponseBody>

    @POST("feedback")
    suspend fun sendFeedback(@Body body: RequestBody): Response<ResponseBody>
}

object NetworkClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val viaggiatrenoService: ViaggiaTrenoService by lazy {
        Retrofit.Builder()
            .baseUrl("http://www.viaggiatreno.it/infomobilita/resteasy/viaggiatreno/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ViaggiaTrenoService::class.java)
    }

    val backendService: BackendService by lazy {
        Retrofit.Builder()
            .baseUrl("https://gestioneinorario.toreroclub.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BackendService::class.java)
    }
}
