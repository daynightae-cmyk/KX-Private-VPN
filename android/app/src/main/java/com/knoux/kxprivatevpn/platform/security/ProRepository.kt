package com.knoux.kxprivatevpn.platform.security

import kotlinx.coroutines.CancellationException
import java.net.URI
import java.net.URLEncoder

/** Release-owned resource configuration, never a user-editable endpoint or contact. */
internal class ProConfiguration(val authorityUrl: String, val whatsappE164: String) {
    val endpoint: URI? = try {
        URI(authorityUrl).takeIf {
            it.scheme == "https" && !it.host.isNullOrBlank() && it.rawUserInfo == null &&
                it.rawQuery == null && it.rawFragment == null && (it.port == -1 || it.port in 1..65535) &&
                it.rawPath.orEmpty().let { path -> path.isEmpty() || path == "/" }
        }
    } catch (_: Exception) { null }
    val configurationFailure: AuthorityResult? = when {
        authorityUrl.isEmpty() -> AuthorityResult.Unconfigured
        endpoint == null -> AuthorityResult.Invalid
        else -> null
    }
    val configured: Boolean get() = configurationFailure == null
    val contactAvailable: Boolean get() = whatsappE164.matches(Regex("\\+[1-9][0-9]{1,14}"))

    /** Call only with the localized generic request, never a code, nonce or installation identity. */
    fun contactUrl(genericRequest: String): String? = if (contactAvailable) {
        "https://wa.me/${whatsappE164.drop(1)}?text=${URLEncoder.encode(genericRequest, "UTF-8")}"
    } else null
}

/** Explicit refresh/redeem only. No storage/network work occurs during construction. */
internal class ProRepository(
    val configuration: ProConfiguration,
    private val identities: ProIdentitySource,
    private val transport: ProHttpTransport = UrlConnectionProTransport(),
    private val elapsedMillis: () -> Long = android.os.SystemClock::elapsedRealtime,
) {
    suspend fun refresh(): AuthorityResult = withIdentity { identity ->
        HttpsProAuthority(configuration, identity, elapsedMillis, transport).validateNow(identity.subject)
    }

    suspend fun redeem(code: String): AuthorityResult {
        configuration.configurationFailure?.let { return it }
        if (!isProHex(code)) return AuthorityResult.Invalid
        return withIdentity { identity -> HttpsProAuthority(configuration, identity, elapsedMillis, transport).redeem(code) }
    }

    /** Invoke immediately before each future PRO_AUTOMATION operation, never from a UI snapshot. */
    suspend fun authorizeAutomationNow(): ProAccess = authorizeNow(AccessFeature.PRO_AUTOMATION)

    suspend fun authorizeNow(feature: AccessFeature): ProAccess {
        if (feature.essentialSafety) return ProAccess.Allowed // Not even a Keystore read for free safety.
        configuration.configurationFailure?.let { failure ->
            return ProEntitlements { failure }.authorize(feature, "unavailable")
        }
        val identity = try { identities.load() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { return ProAccess.Denied(ProDenial.IDENTITY_UNAVAILABLE) }
        // The policy itself invokes the live adapter. No earlier refresh/redeem result is reused.
        return ProEntitlements(HttpsProAuthority(configuration, identity, elapsedMillis, transport))
            .authorize(feature, identity.subject)
    }

    private suspend fun withIdentity(action: suspend (ProIdentity) -> AuthorityResult): AuthorityResult {
        configuration.configurationFailure?.let { return it }
        val identity = try { identities.load() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { return AuthorityResult.IdentityUnavailable }
        return action(identity)
    }
}
