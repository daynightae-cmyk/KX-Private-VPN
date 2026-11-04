package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.network.NetworkDoctor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NetworkDoctorTest {
    @Test fun externalProbeRequiresApprovalBeforeAnyNetworkOperation() = runBlocking {
        try { NetworkDoctor.probe("https://example.invalid", false); fail() } catch (_: IllegalArgumentException) { }
    }
    @Test fun restrictsRequestsToExplicitHttpsWithoutEmbeddedSecrets() {
        assertEquals("example.invalid", NetworkDoctor.validate("https://example.invalid/status").host)
        listOf("http://example.invalid", "https://user:password@example.invalid", "https://example.invalid?token=secret",
            "https://example.invalid/#private", "file:///data", "https://example.invalid:99999").forEach {
            try { NetworkDoctor.validate(it); fail() } catch (_: IllegalArgumentException) { }
        }
    }
}
