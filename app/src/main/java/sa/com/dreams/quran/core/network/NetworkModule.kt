package sa.com.dreams.quran.core.network

import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/** Builds the single shared HTTP stack (one OkHttp client = one connection pool). */
object NetworkModule {

    private const val HTTP_CACHE_BYTES = 10L * 1024 * 1024
    private const val DEFAULT_MAX_AGE_SECONDS = 24 * 60 * 60

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun okHttpClient(cacheDir: File): OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(cacheDir, "http"), HTTP_CACHE_BYTES))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addNetworkInterceptor(CacheWhenUnspecified)
        .build()

    fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    /**
     * Lets OkHttp cache successful API responses for a day when the server sends no
     * caching headers, so repeated requests (e.g. edition lists) don't hit the network.
     */
    private object CacheWhenUnspecified : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val response = chain.proceed(chain.request())
            val cacheable = chain.request().method == "GET" && response.isSuccessful
            return if (cacheable && response.header("Cache-Control") == null) {
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=$DEFAULT_MAX_AGE_SECONDS")
                    .removeHeader("Pragma")
                    .build()
            } else {
                response
            }
        }
    }
}
