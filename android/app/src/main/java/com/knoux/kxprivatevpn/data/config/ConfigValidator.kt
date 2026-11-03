package com.knoux.kxprivatevpn.data.config

import com.wireguard.config.Config
import java.io.ByteArrayInputStream

/** Never propagate parser messages: malformed input can contain private keys. */
class InvalidConfiguration : IllegalArgumentException("Invalid WireGuard configuration")

object ConfigValidator {
    const val MAX_BYTES = 65_536
    fun parse(input: ByteArray): Config {
        if (input.isEmpty() || input.size > MAX_BYTES || input.any { it == 0.toByte() })
            throw InvalidConfiguration()
        try {
            val config = Config.parse(ByteArrayInputStream(input))
            if (config.`interface`.addresses.isEmpty() || config.peers.isEmpty() ||
                config.peers.any { !it.endpoint.isPresent || it.allowedIps.isEmpty() })
                throw InvalidConfiguration()
            return config
        } catch (_: Exception) { throw InvalidConfiguration() }
    }
    fun name(input: String): String {
        val clean = input.trim()
        require(clean.length in 1..48 && clean.none {
            it.isISOControl() || Character.getType(it) == Character.FORMAT.toInt()
        }) { "Invalid profile name" }
        return clean
    }
}
