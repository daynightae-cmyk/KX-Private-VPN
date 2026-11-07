package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.security.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.Closeable
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Real Python HTTP/SQLite + Kotlin codec/policy, with ephemeral test-only credentials.
 * Only this test transport maps the reserved HTTPS origin to IPv4 loopback HTTP.
 * This is not a test of public TLS, Android Keystore or device networking.
 */
class ProBackendContractTest {
    @Test fun realAuthorityBindsRenewsRevokesAndNeverUnlocksOffline() = runBlocking {
        Fixture().use { fixture ->
            val identity = ProIdentity(newProNonce())
            val other = ProIdentity(newProNonce())
            val repository = fixture.repository(identity)
            val otherRepository = fixture.repository(other)
            assertEquals(AuthorityResult.Inactive, repository.refresh())
            val firstCode = fixture.issue(60)
            val activated = repository.redeem(firstCode) as AuthorityResult.Current
            val firstGrant = activated.grant
            assertTrue(firstGrant.subject == identity.subject)
            assertEquals("com.knoux.kxprivatevpn", firstGrant.audience)
            assertEquals(setOf(AccessFeature.PRO_AUTOMATION), firstGrant.features)
            assertEquals(ProAccess.Allowed, repository.authorizeAutomationNow())
            assertEquals(AuthorityResult.Inactive, otherRepository.redeem(firstCode))
            assertEquals(AuthorityResult.Inactive, otherRepository.refresh())
            fixture.advance(500)
            val retry = repository.redeem(firstCode) as AuthorityResult.Current
            assertEquals(firstGrant.issuedAtMillis, retry.grant.issuedAtMillis)
            assertEquals(firstGrant.expiresAtMillis, retry.grant.expiresAtMillis)
            assertEquals(firstGrant.authorityNowMillis + 500, retry.grant.authorityNowMillis)

            val renewedCode = fixture.issue(120)
            val renewed = repository.redeem(renewedCode) as AuthorityResult.Current
            assertTrue(renewed.grant.expiresAtMillis > firstGrant.expiresAtMillis)
            assertEquals(AuthorityResult.Inactive, repository.redeem(firstCode))
            assertEquals(ProAccess.Allowed, repository.authorizeAutomationNow())
            fixture.revoke(identity.subject)
            assertEquals(ProAccess.Denied(ProDenial.INACTIVE), repository.authorizeAutomationNow())
            assertEquals(AuthorityResult.Inactive, repository.redeem(renewedCode))
            val shortCode = fixture.issue(1)
            assertTrue(repository.redeem(shortCode) is AuthorityResult.Current)
            fixture.advance(1000)
            assertEquals(ProAccess.Denied(ProDenial.INACTIVE), repository.authorizeAutomationNow())
            fixture.advance(-1000)
            assertEquals(AuthorityResult.Inactive, repository.redeem(shortCode))

            assertTrue(repository.redeem(fixture.issue(60)) is AuthorityResult.Current)
            fixture.limit()
            assertEquals(ProAccess.Allowed, repository.authorizeAutomationNow())
            assertEquals(ProAccess.Denied(ProDenial.RATE_LIMITED), repository.authorizeAutomationNow())
            fixture.stop()
            assertEquals(ProAccess.Denied(ProDenial.UNAVAILABLE), repository.authorizeAutomationNow())
            AccessFeature.entries.filter { it.essentialSafety }.forEach {
                assertEquals(ProAccess.Allowed, repository.authorizeNow(it))
            }
        }
    }

    private class Fixture : Closeable {
        private val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "backend/pro/client_contract_fixture.py").isFile }
            ?: error("Run the contract test from this repository")
        private val process = ProcessBuilder("python", "-B", File(root, "backend/pro/client_contract_fixture.py").path)
            .redirectError(ProcessBuilder.Redirect.PIPE).start()
        private val input = process.outputStream.bufferedWriter(Charsets.UTF_8)
        private val output = process.inputStream.bufferedReader(Charsets.UTF_8)
        private val reader = Executors.newSingleThreadExecutor { task ->
            Thread(task, "pro-contract-fixture-pipe").apply { isDaemon = true }
        }
        private val port: Int = read().getInt("port").also { require(it in 1..65535) }
        private var stopped = false

        private fun read(): JSONObject = try {
            reader.submit(java.util.concurrent.Callable {
                checkNotNull(output.readLine()) { "Contract fixture closed unexpectedly" }
            }).get(10, TimeUnit.SECONDS).let(::JSONObject)
        } catch (_: Exception) {
            process.destroyForcibly()
            reader.shutdownNow()
            error("Contract fixture did not return a valid response")
        }

        private fun admin(command: JSONObject): JSONObject {
            input.write(command.toString())
            input.newLine()
            input.flush()
            return read()
        }
        fun issue(seconds: Int): String = admin(JSONObject().put("operation", "issue").put("seconds", seconds)).getString("code")
        fun revoke(subject: String) { check(admin(JSONObject().put("operation", "revoke").put("subject", subject)).getBoolean("revoked")) }
        fun advance(millis: Int) { admin(JSONObject().put("operation", "advance").put("millis", millis)) }
        fun limit() { admin(JSONObject().put("operation", "limit")) }
        fun stop() {
            if (stopped) return
            admin(JSONObject().put("operation", "stop"))
            check(process.waitFor(5, TimeUnit.SECONDS)) { "Fixture shutdown timed out" }
            check(process.exitValue() == 0) { "Fixture exited unsuccessfully" }
            stopped = true
        }

        fun repository(identity: ProIdentity): ProRepository = ProRepository(
            ProConfiguration("https://authority.example", ""), { identity },
            ProHttpTransport { request -> withContext(Dispatchers.IO) {
                require(request.url.host == "authority.example" && request.url.scheme == "https")
                require(request.url.rawPath in setOf("/v1/redeem", "/v1/validate"))
                val url = URI("http", null, "127.0.0.1", port, request.url.rawPath, null, null).toURL()
                val connection = url.openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 2000
                    connection.readTimeout = 2000
                    connection.instanceFollowRedirects = false
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setRequestProperty("Authorization", "Bearer ${request.bearer}")
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setFixedLengthStreamingMode(request.json.size)
                    connection.outputStream.use { it.write(request.json) }
                    val status = connection.responseCode
                    val body = (if (status == 200) connection.inputStream else connection.errorStream)
                        ?.use { it.readNBytes(PRO_RESPONSE_LIMIT + 1) } ?: byteArrayOf()
                    assertEquals("no-store", connection.getHeaderField("Cache-Control"))
                    assertEquals("nosniff", connection.getHeaderField("X-Content-Type-Options"))
                    assertNull(connection.getHeaderField("Server"))
                    ProHttpResponse(status, connection.contentType, body)
                } finally { connection.disconnect() }
            } },
        )

        override fun close() {
            try { if (process.isAlive) stop() }
            finally {
                if (process.isAlive) process.destroyForcibly().waitFor(5, TimeUnit.SECONDS)
                input.close()
                output.close()
                process.errorStream.close()
                reader.shutdownNow()
            }
        }
    }
}
