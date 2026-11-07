package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.security.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.security.cert.Certificate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

class ProHttpsTransportTest {
    private fun request() = ProHttpRequest(URI("https://authority.example/v1/validate"),
        "a1".repeat(32), "{}".toByteArray())

    @Test(timeout = 6000) fun cancellationReleasesCallerEvenWhenPlatformReadIgnoresInterrupt() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val connection = StubConnection(object : InputStream() {
            override fun read(): Int {
                entered.countDown()
                val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
                while (release.count > 0 && System.nanoTime() < deadline) {
                    try { release.await(10, TimeUnit.MILLISECONDS) } catch (_: InterruptedException) { }
                }
                return -1
            }
        }, finished)
        val transport = UrlConnectionProTransport { connection }
        val call = launch(Dispatchers.Default) { transport.post(request()) }
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            val started = System.nanoTime()
            call.cancelAndJoin()
            val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
            assertTrue("Cancellation waited for a non-interruptible platform read", elapsedMillis < 1000)
            assertTrue(call.isCancelled)
        } finally {
            release.countDown()
            call.cancelAndJoin()
            assertTrue(finished.await(2, TimeUnit.SECONDS))
        }
    }

    @Test(timeout = 6000) fun ownDeadlineReleasesCallerAndReturnsUnavailable() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val connection = StubConnection(object : InputStream() {
            override fun read(): Int {
                entered.countDown()
                while (release.count > 0) {
                    try { release.await(10, TimeUnit.MILLISECONDS) } catch (_: InterruptedException) { }
                }
                return -1
            }
        }, finished)
        val transport = UrlConnectionProTransport(timeoutMillis = 200) { connection }
        try {
            val repository = ProRepository(ProConfiguration("https://authority.example", ""),
                { ProIdentity("a1".repeat(32)) }, transport)
            val started = System.nanoTime()
            val result = repository.refresh()
            assertEquals(AuthorityResult.Unavailable, result)
            assertEquals(0L, entered.count)
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 1500)
        } finally {
            release.countDown()
            assertTrue(finished.await(2, TimeUnit.SECONDS))
        }
    }

    @Test fun cleartextIsRejectedBeforeOpeningAnyConnection() = runBlocking {
        try {
            UrlConnectionProTransport { error("Must not open cleartext") }
                .post(ProHttpRequest(URI("http://127.0.0.1/v1/validate"), "a1".repeat(32), byteArrayOf()))
            fail("Cleartext accepted")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun transportAppliesLimitsHeadersAndNeverFollowsRedirects() = runBlocking {
        val connection = StubConnection(ByteArrayInputStream("{}".toByteArray()))
        val result = UrlConnectionProTransport { connection }.post(request())
        assertEquals(200, result.status)
        assertEquals("application/json", result.contentType)
        assertEquals("{}", result.body.toString(Charsets.UTF_8))
        assertFalse(connection.instanceFollowRedirects)
        assertFalse(connection.useCaches)
        assertEquals("POST", connection.requestMethod)
        assertEquals("application/json", connection.getRequestProperty("Content-Type"))
        assertEquals("no-store, no-cache", connection.getRequestProperty("Cache-Control"))
        assertEquals(10_000, connection.connectTimeout)
        assertEquals(10_000, connection.readTimeout)
        assertTrue(connection.disconnected)
    }

    @Test fun oversizedBodiesAreRejectedAndConnectionCloses() = runBlocking {
        val connection = StubConnection(ByteArrayInputStream(ByteArray(PRO_RESPONSE_LIMIT + 1)))
        try {
            UrlConnectionProTransport { connection }.post(request())
            fail("Oversized response accepted")
        } catch (_: InvalidProResponse) { }
        assertTrue(connection.disconnected)
    }

    @Test fun redirectsAreReturnedWithoutReadingTheirBody() = runBlocking {
        val connection = StubConnection(object : InputStream() {
            override fun read(): Int = error("Redirect body must not be read")
        }, status = 302)
        assertEquals(302, UrlConnectionProTransport { connection }.post(request()).status)
        assertTrue(connection.disconnected)
    }

    private class StubConnection(
        private val body: InputStream,
        private val finished: CountDownLatch = CountDownLatch(0),
        private val status: Int = 200,
    ) : HttpsURLConnection(URL("https://authority.example/v1/validate")) {
        @Volatile var disconnected = false
        override fun connect() = Unit
        override fun disconnect() { disconnected = true; finished.countDown() }
        override fun usingProxy() = false
        override fun getOutputStream() = ByteArrayOutputStream()
        override fun getInputStream() = body
        override fun getResponseCode() = status
        override fun getContentType() = "application/json"
        override fun getContentLengthLong() = -1L
        override fun getCipherSuite() = "fixture"
        override fun getLocalCertificates(): Array<Certificate>? = null
        override fun getServerCertificates(): Array<Certificate> = emptyArray()
    }
}
