package com.integrity_shield.crypto

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.Test

class CryptoUtilsUT {

    @Test
    fun `GIVEN known input WHEN sha256Hex THEN returns correct hash`() {
        val result = CryptoUtils.sha256Hex(ANY_INPUT.toByteArray(Charsets.UTF_8))

        assertThat(result).isEqualTo(EXPECTED_SHA256_OF_HELLO)
    }

    @Test
    fun `GIVEN empty byte array WHEN sha256Hex THEN returns empty hash constant`() {
        val result = CryptoUtils.sha256Hex(ByteArray(0))

        assertThat(result).isEqualTo(CryptoUtils.emptyHash())
    }

    @Test
    fun `GIVEN two parts WHEN sha256HexConcat THEN returns hash of concatenated string`() {
        val concatenated = CryptoUtils.sha256Hex("$PART_A$PART_B".toByteArray(Charsets.UTF_8))
        val result = CryptoUtils.sha256HexConcat(PART_A, PART_B)

        assertThat(result).isEqualTo(concatenated)
    }

    @Test
    fun `GIVEN same input WHEN sha256Hex called twice THEN returns identical hash`() {
        val first = CryptoUtils.sha256Hex(ANY_INPUT.toByteArray(Charsets.UTF_8))
        val second = CryptoUtils.sha256Hex(ANY_INPUT.toByteArray(Charsets.UTF_8))

        assertThat(first).isEqualTo(second)
    }

    @Test
    fun `GIVEN different inputs WHEN sha256Hex THEN returns different hashes`() {
        val hash1 = CryptoUtils.sha256Hex(ANY_INPUT.toByteArray(Charsets.UTF_8))
        val hash2 = CryptoUtils.sha256Hex(ANOTHER_INPUT.toByteArray(Charsets.UTF_8))

        assertThat(hash1).isNotEqualTo(hash2)
    }

    @Test
    fun `GIVEN emptyHash WHEN called THEN returns well-known SHA-256 of empty string`() {
        val result = CryptoUtils.emptyHash()

        assertSoftly {
            it.assertThat(result).hasSize(64)
            it.assertThat(result).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        }
    }
}

private const val ANY_INPUT = "hello"
private const val ANOTHER_INPUT = "world"
private const val PART_A = "foo"
private const val PART_B = "bar"
private const val EXPECTED_SHA256_OF_HELLO = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"

