package com.knoux.kxprivatevpn.features

import com.knoux.kxprivatevpn.platform.security.AuthorityResult
import com.knoux.kxprivatevpn.platform.security.ProRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

internal enum class ProStatus {
    NOT_CHECKED, UNCONFIGURED, UNAVAILABLE, INACTIVE, INVALID, RATE_LIMITED,
    IDENTITY_UNAVAILABLE, EXPIRED, REVOKED, ACTIVE_SNAPSHOT,
}
internal data class ProUiState(
    val configured: Boolean,
    val contactAvailable: Boolean,
    val status: ProStatus = ProStatus.NOT_CHECKED,
    val busy: Boolean = false,
    val expiresAtMillis: Long? = null,
)

/** In-memory presentation only; never used to authorize an operation or restore an unlock. */
internal class ProActivationState(private val repository: ProRepository) {
    private val mutable = MutableStateFlow(ProUiState(repository.configuration.configured,
        repository.configuration.contactAvailable,
        status = repository.configuration.configurationFailure?.status() ?: ProStatus.NOT_CHECKED))
    val state = mutable.asStateFlow()
    private val operation = Mutex()

    suspend fun refresh() = execute { repository.refresh() }
    suspend fun redeem(code: String) = execute { repository.redeem(code) }

    private suspend fun execute(action: suspend () -> AuthorityResult) {
        if (!operation.tryLock()) return
        try {
            mutable.value = mutable.value.copy(busy = true, status = ProStatus.NOT_CHECKED, expiresAtMillis = null)
            val result = action()
            mutable.value = mutable.value.copy(status = result.status(),
                expiresAtMillis = (result as? AuthorityResult.Current)?.grant?.expiresAtMillis)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            mutable.value = mutable.value.copy(status = ProStatus.UNAVAILABLE)
        } finally {
            mutable.value = mutable.value.copy(busy = false)
            operation.unlock()
        }
    }
}

private fun AuthorityResult.status(): ProStatus = when (this) {
    AuthorityResult.Unconfigured -> ProStatus.UNCONFIGURED
    AuthorityResult.Unavailable -> ProStatus.UNAVAILABLE
    AuthorityResult.Inactive -> ProStatus.INACTIVE
    AuthorityResult.Invalid -> ProStatus.INVALID
    AuthorityResult.RateLimited -> ProStatus.RATE_LIMITED
    AuthorityResult.IdentityUnavailable -> ProStatus.IDENTITY_UNAVAILABLE
    AuthorityResult.Expired -> ProStatus.EXPIRED
    AuthorityResult.Revoked -> ProStatus.REVOKED
    is AuthorityResult.Current -> ProStatus.ACTIVE_SNAPSHOT
}
