package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.data.config.Profile
import com.knoux.kxprivatevpn.platform.vpn.VpnController
import com.knoux.kxprivatevpn.platform.vpn.VpnGateway
import com.knoux.kxprivatevpn.platform.vpn.VpnIssue
import com.knoux.kxprivatevpn.platform.vpn.VpnPhase
import com.knoux.kxprivatevpn.platform.vpn.VpnTraffic
import com.wireguard.android.backend.BackendException
import com.wireguard.config.Config
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.Base64

/** Exercises the real state machine with a fake backend; no state is invented by the tests. */
@OptIn(ExperimentalCoroutinesApi::class)
class VpnControllerTest {
    private val key = Base64.getEncoder().encodeToString(ByteArray(32) { (it + 1).toByte() })
    private val validConfig = """
        [Interface]
        PrivateKey = $key
        Address = 10.7.0.2/32
        DNS = 192.0.2.53
        MTU = 1420
        [Peer]
        PublicKey = $key
        Endpoint = 192.0.2.1:51820
        AllowedIPs = 0.0.0.0/0, ::/0
    """.trimIndent()
    private val profile = Profile("profile-1", "Test tunnel", "192.0.2.1:51820")
    private val startedAt = 1_700_000_000_000L

    private class FakeGateway : VpnGateway {
        var up = false
        var version: String? = "test-backend"
        var traffic = VpnTraffic()
        var startCalls = 0
        var stopCalls = 0
        var lastConfig: Config? = null
        var startFailure: Exception? = null
        var stopFailure: Exception? = null

        override suspend fun start(config: Config): String? {
            startCalls++
            startFailure?.let { throw it }
            lastConfig = config
            up = true
            return version
        }

        override suspend fun stop() {
            stopCalls++
            stopFailure?.let { throw it }
            up = false
        }

        override suspend fun traffic(): VpnTraffic = traffic
        override suspend fun isUp(): Boolean = up
        override suspend fun isLockdownEnabled(): Boolean? = false
    }

    private fun TestScope.controller(
        gateway: VpnGateway,
        config: ByteArray? = validConfig.toByteArray(),
    ) = VpnController(
        scope = backgroundScope,
        gateway = gateway,
        loadConfig = { config },
        io = StandardTestDispatcher(testScheduler),
        now = { startedAt },
    )

    @Test fun connectPublishesOnlyBackendConfirmedSession() = runTest {
        val gateway = FakeGateway().apply { traffic = VpnTraffic(120, 80, 5_000) }
        val controller = controller(gateway)

        controller.connect(profile)
        assertEquals(VpnPhase.CONNECTING, controller.state.value.phase)
        runCurrent()

        val state = controller.state.value
        assertEquals(VpnPhase.CONNECTED, state.phase)
        assertEquals("profile-1", state.session?.profileId)
        assertEquals("Test tunnel", state.session?.profileName)
        assertEquals(startedAt, state.session?.startedAtMillis ?: -1L)
        assertEquals(120L, state.session?.rxBytes ?: -1L)
        assertEquals(80L, state.session?.txBytes ?: -1L)
        assertEquals(5_000L, state.session?.latestHandshakeMillis ?: -1L)
        assertEquals(listOf("192.0.2.53"), state.session?.dnsServers)
        assertEquals(listOf("10.7.0.2/32"), state.session?.addresses)
        // InetNetwork normalizes IPv6 notation in toString().
        assertEquals(2, state.session?.allowedIps?.size ?: -1)
        assertTrue(state.session?.allowedIps?.contains("0.0.0.0/0") == true)
        assertEquals(1420, state.session?.mtu)
        assertEquals("test-backend", state.session?.backendVersion)
        assertEquals(1, gateway.startCalls)
        assertEquals(1, gateway.lastConfig?.peers?.size)
    }

    @Test fun invalidConfigNeverReachesTheBackend() = runTest {
        val gateway = FakeGateway()
        val controller = controller(gateway, "not a WireGuard config".toByteArray())

        controller.connect(profile)
        runCurrent()

        assertEquals(VpnPhase.ERROR, controller.state.value.phase)
        assertEquals(VpnIssue.CONFIG_INVALID, controller.state.value.issue)
        assertEquals(0, gateway.startCalls)
    }

    @Test fun unreadableStoredConfigIsReported() = runTest {
        val gateway = FakeGateway()
        val controller = controller(gateway, null)

        controller.connect(profile)
        runCurrent()

        assertEquals(VpnIssue.CONFIG_UNREADABLE, controller.state.value.issue)
        assertEquals(0, gateway.startCalls)
    }

    @Test fun authorizationFailureSurfacesConsentIssue() = runTest {
        val gateway = FakeGateway().apply {
            startFailure = BackendException(BackendException.Reason.VPN_NOT_AUTHORIZED)
        }
        val controller = controller(gateway)

        controller.connect(profile)
        runCurrent()

        assertEquals(VpnPhase.ERROR, controller.state.value.phase)
        assertEquals(VpnIssue.AUTHORIZATION, controller.state.value.issue)
    }

    @Test fun startFailureIsReportedWithoutConnectedState() = runTest {
        val gateway = FakeGateway().apply { startFailure = IllegalStateException("backend refused") }
        val controller = controller(gateway)

        controller.connect(profile)
        runCurrent()

        assertEquals(VpnPhase.ERROR, controller.state.value.phase)
        assertEquals(VpnIssue.START_FAILED, controller.state.value.issue)
        assertNotNull(controller.state.value.session)
    }

    @Test fun countersFollowTheBackendWhileUp() = runTest {
        val gateway = FakeGateway()
        val controller = controller(gateway)
        controller.connect(profile)
        runCurrent()

        gateway.traffic = VpnTraffic(1_000, 2_000, 9_000)
        advanceTimeBy(1_001)
        runCurrent()

        val session = controller.state.value.session
        assertEquals(VpnPhase.CONNECTED, controller.state.value.phase)
        assertEquals(1_000L, session?.rxBytes ?: -1L)
        assertEquals(2_000L, session?.txBytes ?: -1L)
        assertEquals(9_000L, session?.latestHandshakeMillis ?: -1L)
    }

    @Test fun unexpectedBackendStopBreaksTheConnectedClaim() = runTest {
        val gateway = FakeGateway()
        val controller = controller(gateway)
        controller.connect(profile)
        runCurrent()

        gateway.up = false
        advanceTimeBy(1_001)
        runCurrent()

        assertEquals(VpnPhase.ERROR, controller.state.value.phase)
        assertEquals(VpnIssue.BACKEND_STOPPED, controller.state.value.issue)
    }

    @Test fun disconnectWaitsForBackendStop() = runTest {
        val gateway = FakeGateway()
        val controller = controller(gateway)
        controller.connect(profile)
        runCurrent()

        controller.disconnect()
        assertEquals(VpnPhase.DISCONNECTING, controller.state.value.phase)
        runCurrent()

        assertEquals(VpnPhase.IDLE, controller.state.value.phase)
        assertNull(controller.state.value.session)
        assertEquals(1, gateway.stopCalls)
    }

    @Test fun failedStopStaysVisibleInsteadOfClaimingShutdown() = runTest {
        val gateway = FakeGateway().apply { stopFailure = IllegalStateException("still owned") }
        val controller = controller(gateway)
        controller.connect(profile)
        runCurrent()

        controller.disconnect()
        runCurrent()

        assertEquals(VpnPhase.ERROR, controller.state.value.phase)
        assertEquals(VpnIssue.STOP_FAILED, controller.state.value.issue)
        assertNotNull(controller.state.value.session)
    }

    @Test fun restoreClaimsConnectionOnlyWhenBackendIsUp() = runTest {
        val gateway = FakeGateway()
        val controller = controller(gateway)

        controller.restore(profile)
        assertEquals(VpnPhase.IDLE, controller.state.value.phase)

        gateway.up = true
        controller.restore(profile)
        assertEquals(VpnPhase.CONNECTED, controller.state.value.phase)
        assertNull(controller.state.value.session?.startedAtMillis)
    }

    @Test fun noProfileAndDeniedConsentAreHonestErrors() = runTest {
        val controller = controller(FakeGateway())

        controller.reportNoProfile()
        assertEquals(VpnIssue.NO_PROFILE, controller.state.value.issue)
        assertEquals(VpnPhase.ERROR, controller.state.value.phase)

        controller.clearIssue()
        controller.reportConsentDenied()
        assertEquals(VpnIssue.CONSENT_DENIED, controller.state.value.issue)
        assertNull(controller.state.value.session)
    }

    @Test fun duplicateConnectWhileActiveDoesNotStartTwice() = runTest {
        val gateway = FakeGateway()
        val controller = controller(gateway)

        controller.connect(profile)
        runCurrent()
        controller.connect(profile)
        runCurrent()

        assertEquals(VpnPhase.CONNECTED, controller.state.value.phase)
        assertEquals(1, gateway.startCalls)
        assertTrue(gateway.up)
    }
}
