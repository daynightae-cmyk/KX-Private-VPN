package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.features.ProActivationState
import com.knoux.kxprivatevpn.features.ProStatus
import com.knoux.kxprivatevpn.platform.security.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.PrintStream
import java.net.URI
import java.net.URLDecoder
import java.security.MessageDigest

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProActivationClientTest {
    private val identity = ProIdentity("a1".repeat(32))
    private val code = "b2".repeat(32)
    // Reserved test origin; injected transport never contacts it.
    private val configuration = ProConfiguration("https://authority.example", "")
    private fun valid(request: ProHttpRequest): JSONObject = JSONObject()
        .put("nonce", JSONObject(request.json.toString(Charsets.UTF_8)).getString("nonce"))
        .put("subject", identity.subject).put("audience", "com.knoux.kxprivatevpn")
        .put("issuedAtMillis", 100L).put("expiresAtMillis", 1000L).put("authorityNowMillis", 500L)
        .put("revoked", false).put("features", JSONArray().put("PRO_AUTOMATION"))
    private fun response(json: String, status: Int = 200, type: String = "application/json") =
        ProHttpResponse(status, type, json.toByteArray(Charsets.UTF_8))
    private fun repo(transport: ProHttpTransport) = ProRepository(configuration, { identity }, transport)

    @Test fun validRedeemUsesPostContractAndBoundSecretOnlyInHeader() = runTest {
        var calls = 0
        val repository = repo { request ->
            calls++
            assertEquals("https://authority.example/v1/redeem", request.url.toString())
            assertEquals(identity.secret, request.bearer)
            val json = JSONObject(request.json.toString(Charsets.UTF_8))
            assertEquals(setOf("nonce", "code"), json.keys().asSequence().toSet())
            assertEquals(code, json.getString("code"))
            assertTrue(isProHex(json.getString("nonce")))
            assertFalse(request.url.toString().contains(identity.secret))
            assertFalse(request.toString().contains(identity.secret))
            assertFalse(request.toString().contains(code))
            response(valid(request).toString())
        }
        val result = repository.redeem(code) as AuthorityResult.Current
        assertEquals(identity.subject, result.grant.subject)
        assertEquals(setOf(AccessFeature.PRO_AUTOMATION), result.grant.features)
        assertEquals(1, calls)
    }

    @Test fun installationSubjectHashesAsciiLowerHexNotDecodedRandomBytes() {
        val expected = MessageDigest.getInstance("SHA-256").digest(identity.secret.toByteArray(Charsets.US_ASCII)).lowerHex()
        assertEquals(expected, identity.subject)
        assertFalse(identity.toString().contains(identity.secret))
    }

    @Test fun everyAuthorizationHasFreshNonceAndOfflineNeverReusesSnapshot() = runTest {
        val nonces = mutableSetOf<String>()
        var offline = false
        val repository = repo { request ->
            assertEquals("/v1/validate", request.url.path)
            val json = JSONObject(request.json.toString(Charsets.UTF_8))
            assertEquals(setOf("nonce"), json.keys().asSequence().toSet())
            assertTrue(nonces.add(json.getString("nonce")))
            if (offline) throw IOException()
            response(valid(request).toString())
        }
        repeat(12) { assertEquals(ProAccess.Allowed, repository.authorizeAutomationNow()) }
        offline = true
        assertEquals(ProAccess.Denied(ProDenial.UNAVAILABLE), repository.authorizeAutomationNow())
        assertEquals(13, nonces.size)
    }

    @Test fun replayedResponseFromPriorRequestIsRejected() = runTest {
        var cached: String? = null
        val repository = repo { request ->
            if (cached == null) cached = valid(request).toString()
            response(cached!!)
        }
        assertTrue(repository.refresh() is AuthorityResult.Current)
        assertEquals(AuthorityResult.Invalid, repository.refresh())
    }

    @Test fun schemaBindingTypesAndTimesAreStrict() = runTest {
        val changes: List<(JSONObject) -> Unit> = listOf(
            { it.put("nonce", "0".repeat(64)) },
            { it.put("subject", "f".repeat(64)) },
            { it.put("audience", "another.app") },
            { it.put("issuedAtMillis", 501) },
            { it.put("issuedAtMillis", 0) },
            { it.put("issuedAtMillis", -1) },
            { it.put("expiresAtMillis", 100) },
            { it.put("expiresAtMillis", Long.MAX_VALUE) },
            { it.put("authorityNowMillis", -1) },
            { it.put("issuedAtMillis", "100") },
            { it.put("issuedAtMillis", 100.5) },
            { it.put("revoked", "false") },
            { it.put("features", JSONArray().put("PRO_AUTOMATION").put("PRO_AUTOMATION")) },
            { it.put("features", JSONArray().put("UNKNOWN")) },
            { it.put("features", JSONArray().put("VPN_CONNECT")) },
            { it.put("features", JSONArray()) },
            { it.put("features", "PRO_AUTOMATION") },
            { it.put("extra", "ignored?") },
            { it.remove("subject") },
        )
        changes.forEachIndexed { index, change ->
            val repository = repo { request -> response(valid(request).also(change).toString()) }
            assertEquals("Invalid variant $index", AuthorityResult.Invalid, repository.refresh())
        }
    }

    @Test fun expiredAndRevokedAreNeverGrants() = runTest {
        assertEquals(AuthorityResult.Expired, repo { response(valid(it).put("authorityNowMillis", 1000).toString()) }.refresh())
        assertEquals(AuthorityResult.Revoked, repo { response(valid(it).put("revoked", true).toString()) }.refresh())
        assertEquals(ProAccess.Denied(ProDenial.INACTIVE),
            repo { response(valid(it).put("revoked", true).toString()) }.authorizeAutomationNow())
    }

    @Test fun malformedDuplicateAndNonJsonSyntaxFailClosed() = runTest {
        val corruptions: List<(String) -> String> = listOf(
            { it.dropLast(1) + ",\"subject\":\"${identity.subject}\"}" },
            { it.dropLast(1) + ",}" },
            { "$it trailing" },
            { it.replace('"', '\'') },
            { it.replace("\"issuedAtMillis\":100", "\"issuedAtMillis\":1e2") },
            { it.replace("\"issuedAtMillis\":100", "\"issuedAtMillis\":0100") },
            { "/* comment */$it" },
            { "null" },
            { "[]" },
            { "" },
        )
        corruptions.forEachIndexed { index, corrupt ->
            assertEquals("Malformed variant $index", AuthorityResult.Invalid,
                repo { response(corrupt(valid(it).toString())) }.refresh())
        }
    }

    @Test fun bodyBoundContentTypeAndUtf8AreValidated() = runTest {
        assertEquals(AuthorityResult.Invalid, repo { response(" ".repeat(PRO_RESPONSE_LIMIT + 1)) }.refresh())
        assertEquals(AuthorityResult.Invalid, repo { response(valid(it).toString(), type = "text/html") }.refresh())
        assertEquals(AuthorityResult.Invalid, repo { ProHttpResponse(200, "application/json", byteArrayOf(0xc3.toByte(), 0x28)) }.refresh())
        assertEquals(AuthorityResult.Invalid, repo { throw InvalidProResponse() }.refresh())
        assertTrue(repo { response(valid(it).toString(), type = "application/json; charset=utf-8") }.refresh() is AuthorityResult.Current)
    }

    @Test fun responseCodesFailClosedWithoutReadingUntrustedErrorMessages() = runTest {
        for ((status, expected) in listOf(403 to AuthorityResult.Inactive, 429 to AuthorityResult.RateLimited,
            500 to AuthorityResult.Unavailable, 503 to AuthorityResult.Unavailable,
            301 to AuthorityResult.Invalid, 302 to AuthorityResult.Invalid, 307 to AuthorityResult.Invalid,
            308 to AuthorityResult.Invalid, 401 to AuthorityResult.Invalid, 201 to AuthorityResult.Invalid)) {
            assertEquals(expected, repo { response("untrusted error", status) }.refresh())
        }
        assertEquals(ProAccess.Denied(ProDenial.RATE_LIMITED), repo { response("", 429) }.authorizeAutomationNow())
    }

    @Test fun emptyOrUnsafeConfigurationNeverTouchesNetworkOrIdentity() = runTest {
        val identities = ProIdentitySource { error("Identity must not be read") }
        val transport = ProHttpTransport { error("Network must not be called") }
        val empty = ProRepository(ProConfiguration("", ""), identities, transport)
        assertEquals(AuthorityResult.Unconfigured, empty.refresh())
        assertEquals(AuthorityResult.Unconfigured, empty.redeem(code))
        assertEquals(ProAccess.Denied(ProDenial.UNCONFIGURED), empty.authorizeAutomationNow())
        for (url in listOf("http://localhost", "http://authority.example", "https:///no-host", "https://user:pass@authority.example",
            "https://authority.example?x=1", "https://authority.example#x", "https://authority.example/path", " https://authority.example")) {
            val config = ProConfiguration(url, "")
            assertFalse(url, config.configured)
            assertEquals(AuthorityResult.Invalid, ProRepository(config, identities, transport).refresh())
        }
    }

    @Test fun essentialSafetyAndMalformedCodeDoNotEvenReadIdentity() = runTest {
        val repository = ProRepository(configuration, { error("No identity read") }, { error("No network call") })
        AccessFeature.entries.filter { it.essentialSafety }.forEach {
            assertEquals(ProAccess.Allowed, repository.authorizeNow(it))
        }
        for (invalid in listOf("", "abc", "A".repeat(64), "g".repeat(64), code + "0", " " + code)) {
            assertEquals(AuthorityResult.Invalid, repository.redeem(invalid))
        }
    }

    @Test fun identityLossFailsClosedAndWrongSubjectNeverRequests() = runTest {
        assertEquals(AuthorityResult.IdentityUnavailable,
            ProRepository(configuration, { throw IOException() }, { error("No network") }).refresh())
        assertEquals(AuthorityResult.Invalid,
            HttpsProAuthority(configuration, identity) { error("No network") }.validateNow("wrong"))
    }

    @Test fun cancellationPropagatesThroughIdentityTransportAndPresentationFinally() = runTest {
        val cancelled = CancellationException("cancelled")
        try {
            ProRepository(configuration, { throw cancelled }, { error("No network") }).refresh()
            fail("Cancellation swallowed")
        } catch (caught: CancellationException) { assertSame(cancelled, caught) }
        val presenter = ProActivationState(repo { throw cancelled })
        try { presenter.refresh(); fail("Cancellation swallowed") }
        catch (caught: CancellationException) { assertSame(cancelled, caught) }
        assertFalse(presenter.state.value.busy)
        assertEquals(ProStatus.NOT_CHECKED, presenter.state.value.status)
    }

    @Test fun screenModelDoesNothingOnOpenSerializesBusyAndResetsOnCancellation() = runTest {
        var calls = 0
        val wait = CompletableDeferred<Unit>()
        val presenter = ProActivationState(repo { request ->
            calls++
            wait.await()
            response(valid(request).toString())
        })
        assertEquals(0, calls)
        assertFalse(presenter.state.value.busy)
        val first = launch { presenter.refresh() }
        runCurrent()
        assertTrue(presenter.state.value.busy)
        presenter.redeem(code)
        assertEquals(1, calls)
        first.cancelAndJoin()
        assertFalse(presenter.state.value.busy)
        wait.complete(Unit)
        presenter.refresh()
        assertEquals(ProStatus.ACTIVE_SNAPSHOT, presenter.state.value.status)
        assertEquals(1000L, presenter.state.value.expiresAtMillis)
        assertEquals(2, calls)
    }

    @Test fun concurrentAuthorizeCallsEachReachAuthority() = runTest {
        var calls = 0
        val repository = repo { calls++; response(valid(it).toString()) }
        val tasks = List(4) { async { repository.authorizeAutomationNow() } }
        tasks.forEach { assertEquals(ProAccess.Allowed, it.await()) }
        assertEquals(4, calls)
    }

    @Test fun failureContainingBearerIsNotLoggedOrExposed() = runTest {
        val captured = ByteArrayOutputStream()
        val oldOut = System.out
        val oldErr = System.err
        try {
            System.setOut(PrintStream(captured)); System.setErr(PrintStream(captured))
            val result = repo { throw IOException("Bearer ${identity.secret} $code") }.refresh()
            assertEquals(AuthorityResult.Unavailable, result)
            assertFalse(result.toString().contains(identity.secret))
        } finally { System.setOut(oldOut); System.setErr(oldErr) }
        assertEquals("", captured.toString())
    }

    @Test fun contactRequiresApprovedE164AndEncodesOnlyGenericRequest() {
        for (contact in listOf("", "971123", "+0123456789", "+1/attack", "+123?x=y", "+1234567890123456")) {
            assertNull(ProConfiguration("", contact).contactUrl("generic"))
        }
        // Fixture contact only, never shipped or opened.
        val url = URI(ProConfiguration("", "+12025550123").contactUrl("Generic request طلب"))
        assertEquals("https", url.scheme)
        assertEquals("wa.me", url.host)
        assertEquals("/12025550123", url.path)
        assertEquals("text=Generic request طلب", URLDecoder.decode(url.rawQuery, "UTF-8"))
        assertFalse(url.toString().contains(identity.secret))
    }
}
