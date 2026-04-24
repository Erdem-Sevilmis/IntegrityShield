package com.integrity_shield.crypto

import java.security.MessageDigest

object CryptoUtils {
    private const val ALGORITHM = "SHA-256"
    private const val EMPTY_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

    fun sha256Hex(bytes: ByteArray): String {
        val md = MessageDigest.getInstance(ALGORITHM)
        val digest = md.digest(bytes)
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }

    fun sha256HexConcat(vararg parts: String): String {
        val combined = parts.joinToString(separator = "")
        return sha256Hex(combined.toByteArray(Charsets.UTF_8))
    }

    fun emptyHash(): String = EMPTY_HASH
}

