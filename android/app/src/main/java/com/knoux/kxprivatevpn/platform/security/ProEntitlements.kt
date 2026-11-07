package com.knoux.kxprivatevpn.platform.security

import kotlinx.coroutines.CancellationException

/** Internal domain boundary, NOT an HTTP schema or an implemented activation service. */
internal enum class AccessFeature(val essentialSafety: Boolean) {
    VPN_CONNECT(true), VPN_DISCONNECT(true), VPN_CONSENT(true), VPN_LOCKDOWN(true),
    PRO_AUTOMATION(false),
}

internal enum class ProDenial { UNCONFIGURED, UNAVAILABLE, INACTIVE, INVALID_GRANT }
internal sealed interface ProAccess {
    data object Allowed : ProAccess
    data class Denied(val reason: ProDenial) : ProAccess
}

/**
 * Only a future authenticated, response-integrity-verified authority adapter may supply a grant.
 * Times come from that authority, never the device wall clock. The adapter must validate freshness
 * and prevent replay. This app-local check does not enforce server security on a modified APK.
 */
internal data class AuthorityGrant(
    val subject: String,
    val audience: String,
    val issuedAtMillis: Long,
    val expiresAtMillis: Long,
    val authorityNowMillis: Long,
    val revoked: Boolean,
    val features: Set<AccessFeature>,
)

internal sealed interface AuthorityResult {
    data object Unconfigured : AuthorityResult
    data object Unavailable : AuthorityResult
    data object Inactive : AuthorityResult
    data class Current(val grant: AuthorityGrant) : AuthorityResult
}

/** No transport implementation exists until a real API/authentication contract is supplied. */
internal fun interface ProAuthority {
    suspend fun validateNow(subject: String): AuthorityResult
}

internal object UnconfiguredProAuthority : ProAuthority {
    override suspend fun validateNow(subject: String) = AuthorityResult.Unconfigured
}

/**
 * Online-only authorization policy. Each Pro operation revalidates; no local unlock boolean,
 * cached entitlement, master password, generated activation code or guessed owner contact.
 * Unique-code issuance/redemption, rate limits and revocation enforcement belong on the server.
 * No production feature is gated by this foundation yet. Essential VPN safety remains free.
 */
internal class ProEntitlements(
    private val authority: ProAuthority = UnconfiguredProAuthority,
) {
    suspend fun authorize(feature: AccessFeature, subject: String): ProAccess {
        if (feature.essentialSafety) return ProAccess.Allowed
        if (subject.isBlank()) return ProAccess.Denied(ProDenial.INVALID_GRANT)
        val result = try {
            authority.validateNow(subject)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return ProAccess.Denied(ProDenial.UNAVAILABLE)
        }
        return when (result) {
            AuthorityResult.Unconfigured -> ProAccess.Denied(ProDenial.UNCONFIGURED)
            AuthorityResult.Unavailable -> ProAccess.Denied(ProDenial.UNAVAILABLE)
            AuthorityResult.Inactive -> ProAccess.Denied(ProDenial.INACTIVE)
            is AuthorityResult.Current -> {
                val grant = result.grant
                when {
                    grant.subject != subject || grant.audience != APPLICATION_AUDIENCE ||
                        grant.issuedAtMillis <= 0 || grant.issuedAtMillis > grant.authorityNowMillis ||
                        grant.expiresAtMillis <= grant.issuedAtMillis -> ProAccess.Denied(ProDenial.INVALID_GRANT)
                    grant.revoked || grant.authorityNowMillis >= grant.expiresAtMillis || feature !in grant.features ->
                        ProAccess.Denied(ProDenial.INACTIVE)
                    else -> ProAccess.Allowed
                }
            }
        }
    }

    private companion object {
        const val APPLICATION_AUDIENCE = "com.knoux.kxprivatevpn"
    }
}
