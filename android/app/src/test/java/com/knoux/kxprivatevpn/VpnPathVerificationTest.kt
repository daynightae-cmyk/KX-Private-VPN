package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.vpn.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VpnPathVerificationTest {
    private val now = 1_800_000_000_000L
    private val before = VpnPathSnapshot(true, true, 17, VpnTraffic(100, 200, now - 1000), true, true, false)
    private val after = before.copy(traffic = VpnTraffic(500, 700, now))
    private class Port(val readings: List<VpnPathSnapshot>, val code: Int = 204) : VpnProbePort {
        var reads = 0
        var calls = 0
        var boundHandle: Long? = null
        override suspend fun snapshot() = readings[reads++]
        override suspend fun head(networkHandle: Long, httpsUrl: String): Int {
            calls++
            boundHandle = networkHandle
            return code
        }
    }

    @Test fun successfulHttpsAloneCannotVerifyWireGuard() {
        val evaluate = { end: VpnPathSnapshot -> VpnPathVerifier.evaluate(before, end, 204, now).outcome }
        assertEquals(VpnPathOutcome.NO_RECENT_HANDSHAKE, evaluate(after.copy(traffic = after.traffic.copy(latestHandshakeMillis = null))))
        assertEquals(VpnPathOutcome.NO_RECENT_HANDSHAKE, evaluate(after.copy(traffic = after.traffic.copy(latestHandshakeMillis = now - 180_001))))
        assertEquals(VpnPathOutcome.NO_RECENT_HANDSHAKE, evaluate(after.copy(traffic = after.traffic.copy(latestHandshakeMillis = now + 1))))
        assertEquals(VpnPathOutcome.NO_RECEIVED_TRAFFIC, evaluate(after.copy(traffic = before.traffic)))
        assertEquals(VpnPathOutcome.NO_RECEIVED_TRAFFIC, evaluate(after.copy(traffic = after.traffic.copy(txBytes = before.traffic.txBytes))))
        assertEquals(VpnPathOutcome.NO_RECEIVED_TRAFFIC, evaluate(after.copy(traffic = after.traffic.copy(rxBytes = 0))))
    }

    @Test fun lossSwitchAndHttpFailureCannotPass() {
        assertEquals(VpnPathOutcome.NO_OWNED_TUNNEL,
            VpnPathVerifier.evaluate(before, after.copy(ownedTunnelUp = false), 200, now).outcome)
        assertEquals(VpnPathOutcome.NOT_ROUTED_THROUGH_VPN,
            VpnPathVerifier.evaluate(before, after.copy(activeVpn = false), 200, now).outcome)
        assertEquals(VpnPathOutcome.NETWORK_CHANGED,
            VpnPathVerifier.evaluate(before, after.copy(networkHandle = 18), 200, now).outcome)
        assertEquals(VpnPathOutcome.HTTP_FAILED, VpnPathVerifier.evaluate(before, after, 302, now).outcome)
        assertEquals(VpnPathOutcome.HTTP_FAILED, VpnPathVerifier.evaluate(before, after, 500, now).outcome)
    }

    @Test fun bindsToObservedVpnAndReportsOnlyMeasuredValues() = runTest {
        val port = Port(listOf(before, after))
        val evidence = VpnPathVerifier(port) { now }.verify("https://example.com/", true)
        assertEquals(VpnPathOutcome.VERIFIED, evidence.outcome)
        assertEquals(17L, port.boundHandle)
        assertEquals(1, port.calls)
        assertEquals(400L, evidence.rxDelta)
        assertEquals(500L, evidence.txDelta)
        assertEquals(now, evidence.handshakeMillis)
        assertTrue(evidence.defaultIpv4)
        assertFalse("IPv4 success must not invent an IPv6 route", evidence.defaultIpv6)
    }

    @Test fun noApprovalOrInvalidUrlNeverTouchesNetwork() = runTest {
        val port = Port(emptyList())
        val verifier = VpnPathVerifier(port)
        assertTrue(runCatching { verifier.verify("https://example.com", false) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { verifier.verify("http://example.com", true) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { verifier.verify("https://user:secret@example.com", true) }.exceptionOrNull() is IllegalArgumentException)
        assertEquals(0, port.reads)
        assertEquals(0, port.calls)
    }

    @Test fun splitBypassedOrInactiveAppNeverFallsBackToDefaultNetwork() = runTest {
        for (reading in listOf(before.copy(activeVpn = false), before.copy(ownedTunnelUp = false))) {
            val port = Port(listOf(reading))
            val result = VpnPathVerifier(port).verify("https://example.com", true)
            assertNotEquals(VpnPathOutcome.VERIFIED, result.outcome)
            assertEquals(0, port.calls)
        }
    }

    @Test fun deadlineReturnsUnavailableButCallerCancellationPropagates() = runTest {
        val port = object : VpnProbePort {
            override suspend fun snapshot() = before
            override suspend fun head(networkHandle: Long, httpsUrl: String): Int = awaitCancellation()
        }
        assertEquals(VpnPathOutcome.UNAVAILABLE, VpnPathVerifier(port).verify("https://example.com", true).outcome)
        val cancelled = runCatching {
            withTimeout(1) { VpnPathVerifier(port).verify("https://example.com", true) }
        }.exceptionOrNull()
        assertTrue(cancelled is CancellationException)
    }
}
