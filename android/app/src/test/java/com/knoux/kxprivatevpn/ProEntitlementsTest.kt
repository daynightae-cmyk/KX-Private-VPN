package com.knoux.kxprivatevpn

import com.knoux.kxprivatevpn.platform.security.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ProEntitlementsTest {
    private val feature = AccessFeature.PRO_AUTOMATION
    private val grant = AuthorityGrant("test-subject", "com.knoux.kxprivatevpn", 100, 1_000, 500,
        revoked = false, features = setOf(feature))

    @Test fun unconfiguredAuthorityCannotUnlockPro() = runTest {
        assertEquals(ProAccess.Denied(ProDenial.UNCONFIGURED),
            ProEntitlements().authorize(feature, "test-subject"))
    }

    @Test fun freeVpnSafetyNeverConsultsEntitlements() = runTest {
        val policy = ProEntitlements { error("Safety must not contact the Pro authority") }
        AccessFeature.entries.filter { it.essentialSafety }.forEach {
            assertEquals(ProAccess.Allowed, policy.authorize(it, ""))
        }
    }

    @Test fun everyPaidOperationRevalidatesAndOfflineNeverReusesAGrant() = runTest {
        var calls = 0
        val policy = ProEntitlements {
            calls++
            if (calls == 1) AuthorityResult.Current(grant) else AuthorityResult.Unavailable
        }
        assertEquals(ProAccess.Allowed, policy.authorize(feature, "test-subject"))
        assertEquals(ProAccess.Denied(ProDenial.UNAVAILABLE), policy.authorize(feature, "test-subject"))
        assertEquals(2, calls)
    }

    @Test fun expiryRevocationBindingAndFeatureScopeFailClosed() = runTest {
        for (invalid in listOf(grant.copy(revoked = true), grant.copy(authorityNowMillis = 1_000),
            grant.copy(subject = "different"), grant.copy(audience = "different"),
            grant.copy(features = emptySet()), grant.copy(issuedAtMillis = 600),
            grant.copy(issuedAtMillis = 0), grant.copy(expiresAtMillis = 99))) {
            assertTrue(ProEntitlements { AuthorityResult.Current(invalid) }
                .authorize(feature, "test-subject") is ProAccess.Denied)
        }
    }

    @Test fun failureDeniesButCancellationPropagates() = runTest {
        assertEquals(ProAccess.Denied(ProDenial.UNAVAILABLE),
            ProEntitlements { throw java.io.IOException("test failure") }.authorize(feature, "test-subject"))
        val cancellation = CancellationException("test cancellation")
        try {
            ProEntitlements { throw cancellation }.authorize(feature, "test-subject")
            fail("Cancellation must not turn into a grant")
        } catch (caught: CancellationException) {
            assertSame(cancellation, caught)
        }
    }
}
