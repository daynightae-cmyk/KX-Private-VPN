package com.knoux.kxprivatevpn.platform.vpn

/**
 * Process-local seam between the ongoing session notification and the controller that owns
 * the live tunnel. The controller registers while its ViewModel exists, so the notification
 * asks it to stop and the UI observes DISCONNECTING instead of a backend that vanished.
 *
 * No registered handler means no UI owns the tunnel: the caller must stop the backend
 * directly and must never report a disconnect it did not perform.
 */
object VpnSessionControl {
    @Volatile private var disconnectHandler: (() -> Boolean)? = null

    fun register(handler: () -> Boolean) {
        disconnectHandler = handler
    }

    fun unregister(handler: () -> Boolean) {
        if (disconnectHandler === handler) disconnectHandler = null
    }

    /** True only when a live controller accepted the request. */
    fun requestDisconnect(): Boolean = disconnectHandler?.invoke() == true
}
