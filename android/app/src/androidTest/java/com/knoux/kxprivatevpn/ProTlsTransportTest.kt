package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.security.ProTlsSocketFactory
import org.junit.Assert.*
import org.junit.Test
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket

/** Inspects the real Android TLS provider without connecting, changing trust, or touching app data. */
class ProTlsTransportTest {
    @Test fun platformSocketsEnableOnlyTls12OrNewer() {
        val originalFactory = HttpsURLConnection.getDefaultSSLSocketFactory()
        val originalVerifier = HttpsURLConnection.getDefaultHostnameVerifier()
        val first = ProTlsSocketFactory()
        val second = ProTlsSocketFactory()
        assertNotSame(first, second)
        listOf(first, second).forEach { factory ->
            factory.createSocket().use { socket ->
                val protocols = (socket as SSLSocket).enabledProtocols.toSet()
                assertTrue("TLS 1.2 must remain available on every supported API", "TLSv1.2" in protocols)
                assertTrue(setOf("TLSv1.2", "TLSv1.3").containsAll(protocols))
                assertFalse(socket.isConnected)
            }
        }
        assertSame(originalFactory, HttpsURLConnection.getDefaultSSLSocketFactory())
        assertSame(originalVerifier, HttpsURLConnection.getDefaultHostnameVerifier())
    }
}
