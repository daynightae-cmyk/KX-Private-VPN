package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.features.NetworkReferencePolicy
import com.knoux.kxprivatevpn.features.NetworkUiState
import com.knoux.kxprivatevpn.platform.network.HttpsReading
import com.knoux.kxprivatevpn.platform.network.NetworkReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkReferencePolicyTest {
    private fun network(
        present: Boolean = true,
        validated: Boolean = true,
        dns: List<String> = listOf("1.1.1.1"),
        transports: List<String> = listOf("Wi-Fi"),
    ) = NetworkReading(
        present = present,
        validated = validated,
        captive = false,
        vpn = false,
        transports = transports,
        dns = dns,
    )

    @Test fun noAndroidReadingDoesNotInventFacts() {
        val summary = NetworkReferencePolicy.facts(NetworkUiState())
        assertNull(summary.transport)
        assertNull(summary.dnsCount)
        assertNull(summary.httpStatus)
        assertNull(summary.requestDurationMs)
        assertNull(summary.validatedByAndroid)
        assertNull(summary.networkPresent)
        assertFalse(summary.dnsReported)
        assertFalse(summary.httpObserved)
    }

    @Test fun wifiAndReportedDnsComeFromTheActualReading() {
        val summary = NetworkReferencePolicy.facts(NetworkUiState(reading = network()))
        assertEquals("Wi-Fi", summary.transport)
        assertEquals(1, summary.dnsCount)
        assertEquals(true, summary.validatedByAndroid)
        assertTrue(summary.dnsReported)
        assertNull(summary.httpStatus)
        assertNull(summary.requestDurationMs)
    }

    @Test fun disconnectedAndroidNetworkIsNotLabeledWifiEvenIfStaleTransportProvided() {
        val summary = NetworkReferencePolicy.facts(NetworkUiState(
            reading = network(present = false, validated = false),
        ))
        assertNull(summary.transport)
        assertEquals(false, summary.networkPresent)
        assertEquals(false, summary.validatedByAndroid)
    }

    @Test fun dnsNotReportedNeverBecomesLeakVerdict() {
        val summary = NetworkReferencePolicy.facts(NetworkUiState(
            reading = network(dns = emptyList()),
        ))
        assertEquals(0, summary.dnsCount)
        assertFalse(summary.dnsReported)
        assertNull(summary.httpStatus)
    }

    @Test fun durationAppearsOnlyAfterARealHttpsProbeReceipt() {
        val summary = NetworkReferencePolicy.facts(NetworkUiState(
            reading = network(),
            probeResult = HttpsReading(code = 204, elapsedMs = 87),
        ))
        assertEquals(204, summary.httpStatus)
        assertEquals(87L, summary.requestDurationMs)
        assertTrue(summary.httpObserved)
    }

    @Test fun http403DoesNotGetMisrepresentedAsVpnLeakOrInternetOutage() {
        val summary = NetworkReferencePolicy.facts(NetworkUiState(
            reading = network(validated = true),
            probeResult = HttpsReading(code = 403, elapsedMs = 35),
        ))
        assertEquals(403, summary.httpStatus)
        assertEquals(true, summary.validatedByAndroid)
        assertEquals(35L, summary.requestDurationMs)
        assertTrue(summary.httpObserved)
    }

    @Test fun failedProbeCannotFabricateAnElapsedTime() {
        val summary = NetworkReferencePolicy.facts(NetworkUiState(
            reading = network(), probeFailed = true,
        ))
        assertNull(summary.httpStatus)
        assertNull(summary.requestDurationMs)
    }
}
