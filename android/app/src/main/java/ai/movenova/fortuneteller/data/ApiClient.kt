package ai.movenova.fortuneteller.data

import ai.movenova.fortuneteller.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Failures surfaced to the UI. Mirrors iOS APIError cases. */
sealed class ApiError(message: String) : Exception(message) {
    class Network(val host: String, cause: Throwable?) : ApiError("network:$host")
    data object Unauthorized : ApiError("unauthorized")
    class BadRequest(val detail: String) : ApiError("bad_request:$detail")
    data object TooLarge : ApiError("too_large")
    class Server(val status: Int) : ApiError("server:$status")
    class Decoding(cause: Throwable?) : ApiError("decoding")
}

/** JSON codec shared by the client and unit tests. */
object PalmJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun parseReading(text: String): PalmReadingResponse = json.decodeFromString(text)
    fun parseMatch(text: String): PalmMatchResponse = json.decodeFromString(text)
}

class ApiClient(
    baseUrl: String = BuildConfig.BACKEND_BASE_URL,
    private val apiKey: String = BuildConfig.BACKEND_API_KEY,
    private val client: OkHttpClient = defaultClient()
) {
    private val baseUrl: String = baseUrl.trim().trimEnd('/')

    val host: String
        get() = runCatching { java.net.URI(baseUrl).host ?: baseUrl }.getOrDefault(baseUrl)

    suspend fun analyzePalm(
        imageJpeg: ByteArray,
        scope: ReadingScope,
        language: String?,
        userId: String? = null
    ): PalmReadingResponse = withContext(Dispatchers.IO) {
        if (UploadLimits.exceedsCap(imageJpeg)) throw ApiError.TooLarge
        val form = PalmRequests.analyzeForm(imageJpeg, scope, language, userId)
        PalmJson.parseReading(execute("$baseUrl/analyze-palm", form))
    }

    suspend fun matchPalm(
        imageAJpeg: ByteArray,
        imageBJpeg: ByteArray,
        matchType: MatchType,
        personABirth: String,
        personBBirth: String,
        language: String?
    ): PalmMatchResponse = withContext(Dispatchers.IO) {
        if (UploadLimits.exceedsCap(imageAJpeg) || UploadLimits.exceedsCap(imageBJpeg)) {
            throw ApiError.TooLarge
        }
        val form = PalmRequests.matchForm(
            imageAJpeg = imageAJpeg,
            imageBJpeg = imageBJpeg,
            matchType = matchType,
            personABirth = personABirth,
            personBBirth = personBBirth,
            language = language
        )
        PalmJson.parseMatch(execute("$baseUrl/match-palm", form))
    }

    private fun execute(url: String, form: MultipartFormData): String {
        val requestBody = form.build().toRequestBody(form.contentType.toMediaType())
        val builder = Request.Builder().url(url).post(requestBody)
        if (apiKey.isNotBlank()) {
            builder.header("X-API-Key", apiKey)
        }

        val response: Response = try {
            client.newCall(builder.build()).execute()
        } catch (io: IOException) {
            throw ApiError.Network(host, io)
        }

        response.use { res ->
            val text = res.body?.string().orEmpty()
            return when {
                res.isSuccessful -> text
                res.code == 401 -> throw ApiError.Unauthorized
                res.code == 413 -> throw ApiError.TooLarge
                res.code == 400 -> throw ApiError.BadRequest(errorDetail(text))
                else -> throw ApiError.Server(res.code)
            }
        }
    }

    private fun errorDetail(text: String): String =
        runCatching {
            PalmJson.json.parseToJsonElement(text).let { element ->
                element.toString().takeIf { it.isNotBlank() } ?: text
            }
        }.getOrDefault(text).take(240)

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()
    }
}
