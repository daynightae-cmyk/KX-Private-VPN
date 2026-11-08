package com.knoux.kxprivatevpn.platform.security

import kotlinx.coroutines.CancellationException

/** Internal operation policy; essential VPN safety never requires a Pro license. */
internal enum class AccessFeature(val essentialSafety: Boolean) {
    VPN_CONNECT(true), VPN_DISCONNECT(true), VPN_CONSENT(true), VPN_LOCKDOWN(true),
    PRO_AUTOMATION(false),
}

internal enum class ProDenial { UNCONFIGURED, UNAVAILABLE, INACTIVE, INVALID_GRANT, RATE_LIMITED, IDENTITY_UNAVAILABLE }
internal sealed interface ProAccess {
    data object Allowed : ProAccess
    data class Denied(val reason: ProDenial) : ProAccess
}

/**
 * HttpsProAuthority supplies nonce- and installation-bound grants over trusted HTTPS.
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
    data object Invalid : AuthorityResult
    data object RateLimited : AuthorityResult
    data object IdentityUnavailable : AuthorityResult
    data object Expired : AuthorityResult
    data object Revoked : AuthorityResult
    data class Current(val grant: AuthorityGrant) : AuthorityResult
}

/** Every call must reach the authority; an earlier UI snapshot is not authorization. */
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
 * ProRepository exposes authorizeAutomationNow for future operation entry points.
 * Activation alone does not implement automation or change existing free features.
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
            AuthorityResult.Inactive, AuthorityResult.Expired, AuthorityResult.Revoked -> ProAccess.Denied(ProDenial.INACTIVE)
            AuthorityResult.Invalid -> ProAccess.Denied(ProDenial.INVALID_GRANT)
            AuthorityResult.RateLimited -> ProAccess.Denied(ProDenial.RATE_LIMITED)
            AuthorityResult.IdentityUnavailable -> ProAccess.Denied(ProDenial.IDENTITY_UNAVAILABLE)
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
