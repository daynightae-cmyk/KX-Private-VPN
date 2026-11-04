package com.knoux.kxprivatevpn.data.config

import java.io.InputStream
import java.io.ByteArrayOutputStream

object ConfigInput {
    fun read(stream: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            if (output.size() + count > ConfigValidator.MAX_BYTES) throw InvalidConfiguration()
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
