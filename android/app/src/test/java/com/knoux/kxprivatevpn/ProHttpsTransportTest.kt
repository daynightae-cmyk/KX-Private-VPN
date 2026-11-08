package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.security.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.Socket
import java.net.URI
import java.security.cert.Certificate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class ProHttpsTransportTest {
    private val elapsedMillis = { System.nanoTime() / 1_000_000L }
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
                    try {
                        if (release.await(10, TimeUnit.MILLISECONDS)) break
                    } catch (_: InterruptedException) {
                        // Deliberately model a platform read that ignores cancellation.
                    }
                }
                return -1
            }
        }, finished)
        val transport = UrlConnectionProTransport(elapsedMillis = elapsedMillis) { connection }
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
                    try {
                        if (release.await(10, TimeUnit.MILLISECONDS)) break
                    } catch (_: InterruptedException) {
                        // Deliberately model a platform read that ignores cancellation.
                    }
                }
                return -1
            }
        }, finished)
        val transport = UrlConnectionProTransport(timeoutMillis = 200, elapsedMillis = elapsedMillis) { connection }
        try {
            val repository = ProRepository(ProConfiguration("https://authority.example", ""),
                { ProIdentity("a1".repeat(32)) }, transport, elapsedMillis = elapsedMillis)
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
            UrlConnectionProTransport(elapsedMillis = elapsedMillis) { error("Must not open cleartext") }
                .post(ProHttpRequest(URI("http://127.0.0.1/v1/validate"), "a1".repeat(32), byteArrayOf()))
            fail("Cleartext accepted")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun transportAppliesLimitsHeadersAndNeverFollowsRedirects() = runBlocking {
        val connection = StubConnection(ByteArrayInputStream("{}".toByteArray()))
        val result = UrlConnectionProTransport(elapsedMillis = elapsedMillis) { connection }.post(request())
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
            UrlConnectionProTransport(elapsedMillis = elapsedMillis) { connection }.post(request())
            fail("Oversized response accepted")
        } catch (_: InvalidProResponse) { }
        assertTrue(connection.disconnected)
    }

    @Test fun redirectsAreReturnedWithoutReadingTheirBody() = runBlocking {
        val connection = StubConnection(object : InputStream() {
            override fun read(): Int = error("Redirect body must not be read")
        }, status = 302)
        assertEquals(302, UrlConnectionProTransport(elapsedMillis = elapsedMillis) { connection }.post(request()).status)
        assertTrue(connection.disconnected)
    }

    @Test fun transportUsesFreshModernTlsWithoutReplacingPlatformTrust() = runBlocking {
        val originalFactory = HttpsURLConnection.getDefaultSSLSocketFactory()
        val originalVerifier = HttpsURLConnection.getDefaultHostnameVerifier()
        val connections = List(2) { StubConnection(ByteArrayInputStream("{}".toByteArray())) }
        connections.forEach { connection ->
            UrlConnectionProTransport(elapsedMillis = elapsedMillis) { connection }.post(request())
            assertNotSame(originalFactory, connection.sslSocketFactory)
            assertSame(originalVerifier, connection.hostnameVerifier)
            connection.sslSocketFactory.createSocket().use { socket ->
                val protocols = (socket as SSLSocket).enabledProtocols.toSet()
                assertTrue(protocols.isNotEmpty())
                assertTrue(setOf("TLSv1.2", "TLSv1.3").containsAll(protocols))
            }
        }
        assertNotSame(connections[0].sslSocketFactory, connections[1].sslSocketFactory)
        assertSame(originalFactory, HttpsURLConnection.getDefaultSSLSocketFactory())
        assertSame(originalVerifier, HttpsURLConnection.getDefaultHostnameVerifier())
    }

    @Test fun everyTlsSocketEntryPointRemovesLegacyProtocols() {
        val platform = SSLContext.getInstance("TLSv1.2").apply { init(null, null, null) }.socketFactory
        val delegate = SocketFactoryFixture {
            (platform.createSocket() as SSLSocket).apply {
                // Emulate a provider that enables old protocols; no network or handshake occurs.
                enabledProtocols = supportedProtocols
                assertTrue(enabledProtocols.any { it !in setOf("TLSv1.2", "TLSv1.3") })
            }
        }
        val factory = ProTlsSocketFactory(delegate)
        val loopback = InetAddress.getLoopbackAddress()
        Socket().use { underlying ->
            val calls = listOf<() -> Socket>(
                { factory.createSocket() },
                { factory.createSocket("localhost", 443) },
                { factory.createSocket("localhost", 443, loopback, 0) },
                { factory.createSocket(loopback, 443) },
                { factory.createSocket(loopback, 443, loopback, 0) },
                { factory.createSocket(underlying, "localhost", 443, false) },
            )
            calls.forEach { create ->
                create().use { socket ->
                    val protocols = (socket as SSLSocket).enabledProtocols.toSet()
                    assertTrue(protocols.isNotEmpty())
                    assertTrue(setOf("TLSv1.2", "TLSv1.3").containsAll(protocols))
                    assertFalse(socket.isConnected)
                }
            }
        }
    }

    @Test fun nonTlsSocketIsClosedAndRejected() {
        val socket = Socket()
        val factory = ProTlsSocketFactory(SocketFactoryFixture { socket })
        assertThrows(IOException::class.java) { factory.createSocket() }
        assertTrue(socket.isClosed)
    }

    /** Returns unconnected sockets without resolving names or contacting any endpoint. */
    private class SocketFactoryFixture(private val socket: () -> Socket) : SSLSocketFactory() {
        override fun getDefaultCipherSuites(): Array<String> = emptyArray()
        override fun getSupportedCipherSuites(): Array<String> = emptyArray()
        override fun createSocket(): Socket = socket()
        override fun createSocket(host: String, port: Int): Socket = socket()
        override fun createSocket(host: String, port: Int, local: InetAddress, localPort: Int): Socket = socket()
        override fun createSocket(host: InetAddress, port: Int): Socket = socket()
        override fun createSocket(host: InetAddress, port: Int, local: InetAddress, localPort: Int): Socket = socket()
        override fun createSocket(underlying: Socket, host: String, port: Int, autoClose: Boolean): Socket = socket()
    }

    private class StubConnection(
        private val body: InputStream,
        private val finished: CountDownLatch = CountDownLatch(0),
        private val status: Int = 200,
    ) : HttpsURLConnection(URI("https://authority.example/v1/validate").toURL()) {
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
