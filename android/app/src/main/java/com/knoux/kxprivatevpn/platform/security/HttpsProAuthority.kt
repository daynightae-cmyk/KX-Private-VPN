package com.knoux.kxprivatevpn.platform.security

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext

internal const val PRO_RESPONSE_LIMIT = 32 * 1024

/** No generated toString: request credentials and redemption codes are never diagnostics. */
internal class ProHttpRequest(val url: URI, val bearer: String, val json: ByteArray) {
    override fun toString() = "ProHttpRequest([redacted])"
}
internal class ProHttpResponse(val status: Int, val contentType: String?, val body: ByteArray)
internal fun interface ProHttpTransport { suspend fun post(request: ProHttpRequest): ProHttpResponse }
internal class InvalidProResponse : IOException()

/** Platform trust and hostname verification remain intact. No redirects, cache, or TLS bypass. */
internal class UrlConnectionProTransport(
    private val timeoutMillis: Long = 30_000,
    private val executor: ThreadPoolExecutor = workers,
    private val openConnection: (URI) -> HttpsURLConnection = { it.toURL().openConnection() as HttpsURLConnection },
) : ProHttpTransport {
    override suspend fun post(request: ProHttpRequest): ProHttpResponse {
        require(ProConfiguration(request.url.resolve("/").toString(), "").configured)
        return withTimeoutOrNull(timeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                val cancelled = AtomicBoolean(false)
                val future = executor.submit {
                    if (!continuation.isActive) return@submit
                    // The caller wipes its buffer on cancellation; the worker owns its copy.
                    val payload = request.json.copyOf()
                    try {
                        val result = exchange(request.url, request.bearer, payload) {
                            if (cancelled.get() || Thread.currentThread().isInterrupted) throw IOException()
                        }
                        if (continuation.isActive) continuation.resume(result)
                    } catch (failure: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(failure)
                    } finally { payload.fill(0) }
                }
                continuation.invokeOnCancellation {
                    cancelled.set(true)
                    // DNS, writes and even disconnect can block in the platform. Never wait on
                    // them in the caller's cancellation handler. The bounded worker cleans up.
                    future.cancel(true)
                    executor.purge()
                }
            }
        } ?: throw IOException()
    }

    private fun exchange(url: URI, bearer: String, payload: ByteArray, checkActive: () -> Unit): ProHttpResponse {
        checkActive()
        val connection = openConnection(url)
        try {
            // A fresh socket factory prevents pooled HTTPS connections/session state being reused.
            connection.sslSocketFactory = SSLContext.getInstance("TLS").apply { init(null, null, null) }.socketFactory
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.defaultUseCaches = false
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $bearer")
            connection.setRequestProperty("Cache-Control", "no-store, no-cache")
            connection.setRequestProperty("Connection", "close")
            connection.setFixedLengthStreamingMode(payload.size)
            checkActive()
            connection.outputStream.use { it.write(payload) }
            checkActive()
            val status = connection.responseCode
            // Error bodies are untrusted, unnecessary and must not enter logs/UI.
            if (status != 200) return ProHttpResponse(status, null, byteArrayOf())
            if (connection.contentLengthLong > PRO_RESPONSE_LIMIT) throw InvalidProResponse()
            val body = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(2048)
                while (true) {
                    checkActive()
                    val count = input.read(buffer, 0, minOf(buffer.size, PRO_RESPONSE_LIMIT + 1 - output.size()))
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    if (output.size() > PRO_RESPONSE_LIMIT) throw InvalidProResponse()
                }
                output.toByteArray()
            }
            checkActive()
            return ProHttpResponse(status, connection.contentType, body)
        } finally { connection.disconnect() }
    }

    private companion object {
        val workers = ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, ArrayBlockingQueue(1),
            { task -> Thread(task, "kx-pro-authority").apply { isDaemon = true } },
            ThreadPoolExecutor.AbortPolicy())
    }
}

/** Client of the project's own /v1 contract; deployment and real-server E2E are separate gates. */
internal class HttpsProAuthority(
    private val configuration: ProConfiguration,
    private val identity: ProIdentity,
    private val transport: ProHttpTransport,
) : ProAuthority {
    override suspend fun validateNow(subject: String): AuthorityResult {
        configuration.configurationFailure?.let { return it }
        if (subject != identity.subject) return AuthorityResult.Invalid
        return request("validate", null)
    }

    suspend fun redeem(code: String): AuthorityResult {
        configuration.configurationFailure?.let { return it }
        if (!isProHex(code)) return AuthorityResult.Invalid
        return request("redeem", code)
    }

    private suspend fun request(operation: String, code: String?): AuthorityResult {
        configuration.configurationFailure?.let { return it }
        val nonce = newProNonce() // New 256-bit challenge for EVERY authorize/refresh/redeem.
        val payload = (if (code == null) "{\"nonce\":\"$nonce\"}"
            else "{\"nonce\":\"$nonce\",\"code\":\"$code\"}").toByteArray(Charsets.US_ASCII)
        return try {
            currentCoroutineContext().ensureActive()
            val response = transport.post(ProHttpRequest(configuration.endpoint!!.resolve("/v1/$operation"), identity.secret, payload))
            currentCoroutineContext().ensureActive()
            when (response.status) {
                200 -> decode(response, nonce)
                403 -> AuthorityResult.Inactive
                429 -> AuthorityResult.RateLimited
                in 500..599 -> AuthorityResult.Unavailable
                else -> AuthorityResult.Invalid
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: InvalidProResponse) {
            currentCoroutineContext().ensureActive()
            AuthorityResult.Invalid
        }
        catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            AuthorityResult.Unavailable
        } finally { payload.fill(0) }
    }

    private fun decode(response: ProHttpResponse, nonce: String): AuthorityResult = try {
        require(response.body.size in 1..PRO_RESPONSE_LIMIT)
        require(response.contentType?.matches(Regex("application/json(?:\\s*;\\s*charset=utf-8)?", RegexOption.IGNORE_CASE)) == true)
        val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(response.body)).toString()
        val fields = ProGrantJson(text).read()
        require(fields.keys == setOf("nonce", "subject", "audience", "issuedAtMillis", "expiresAtMillis", "authorityNowMillis", "revoked", "features"))
        require(fields["nonce"] == nonce && fields["subject"] == identity.subject && fields["audience"] == "com.knoux.kxprivatevpn")
        val issued = fields["issuedAtMillis"] as Long
        val expires = fields["expiresAtMillis"] as Long
        val now = fields["authorityNowMillis"] as Long
        require(issued in 1..MAX_TIME && now in issued..MAX_TIME && expires in 1..MAX_TIME && expires > issued)
        val revoked = fields["revoked"] as Boolean
        val features = fields["features"] as List<*>
        require(features == listOf("PRO_AUTOMATION")) // Also rejects duplicates and unknown/free features.
        when {
            revoked -> AuthorityResult.Revoked
            now >= expires -> AuthorityResult.Expired
            else -> AuthorityResult.Current(AuthorityGrant(identity.subject, "com.knoux.kxprivatevpn", issued,
                expires, now, false, setOf(AccessFeature.PRO_AUTOMATION)))
        }
    } catch (_: Exception) { AuthorityResult.Invalid }

    private companion object { const val MAX_TIME = 253402300799999L }
}

/** Narrow strict JSON reader: Android JSONObject accepts non-JSON syntax and duplicate names.
 * Only the flat grant schema and its string array are supported; no recursion or coercion.
 */
private class ProGrantJson(private val text: String) {
    private var index = 0
    private fun space() { while (index < text.length && text[index] in " \r\n\t") index++ }
    private fun take(char: Char) { space(); require(index < text.length && text[index++] == char) }
    private fun peek(): Char { space(); require(index < text.length); return text[index] }
    fun read(): Map<String, Any> {
        take('{')
        val fields = mutableMapOf<String, Any>()
        do {
            val key = string()
            require(key !in fields && fields.size < 8)
            take(':')
            val value: Any = when (peek()) {
                '"' -> string()
                '[' -> strings()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                else -> integer()
            }
            fields[key] = value
            if (peek() != ',') break
            take(',')
        } while (true)
        take('}')
        space()
        require(index == text.length)
        return fields
    }
    private fun literal(word: String, value: Boolean): Boolean {
        require(text.startsWith(word, index)); index += word.length; return value
    }
    private fun integer(): Long {
        space()
        val start = index
        if (index < text.length && text[index] == '-') index++
        require(index < text.length && text[index] in '0'..'9')
        if (text[index] == '0') index++ else while (index < text.length && text[index] in '0'..'9') index++
        return text.substring(start, index).toLong()
    }
    private fun strings(): List<String> {
        take('[')
        val result = mutableListOf<String>()
        if (peek() != ']') do {
            require(result.size < 8)
            result += string()
            if (peek() != ',') break
            take(',')
        } while (true)
        take(']')
        return result
    }
    private fun string(): String {
        take('"')
        val result = StringBuilder()
        while (index < text.length) {
            val char = text[index++]
            if (char == '"') return result.toString()
            require(char >= ' ')
            if (char != '\\') result.append(char) else {
                require(index < text.length)
                when (val escaped = text[index++]) {
                    '"', '\\', '/' -> result.append(escaped)
                    'b' -> result.append('\b')
                    'f' -> result.append('\u000c')
                    'n' -> result.append('\n')
                    'r' -> result.append('\r')
                    't' -> result.append('\t')
                    'u' -> {
                        require(index + 4 <= text.length)
                        val digits = text.substring(index, index + 4)
                        require(digits.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' })
                        result.append(digits.toInt(16).toChar()); index += 4
                    }
                    else -> error("Invalid JSON")
                }
            }
        }
        error("Invalid JSON")
    }
}
