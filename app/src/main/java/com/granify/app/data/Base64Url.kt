package com.granify.app.data

import java.util.Base64

/** Gmail encodes both text and binary bodies as unpadded base64url (RFC 4648 §5). */
internal object Base64Url {
    fun decodeBytes(data: String): ByteArray {
        val normalized = data.replace('-', '+').replace('_', '/')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        return Base64.getDecoder().decode(padded)
    }

    fun decodeText(data: String): String = String(decodeBytes(data), Charsets.UTF_8)
}
