package com.integrity_shield.domain.service

import com.integrity_shield.crypto.CryptoUtils
import com.integrity_shield.domain.model.HashPosition
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MerkleTreeServiceUT {

    private lateinit var merkleTreeService: MerkleTreeService

    @BeforeEach
    fun init() {
        merkleTreeService = MerkleTreeService()
    }

    @Test
    fun `GIVEN empty tree WHEN getRoot THEN returns empty hash`() {
        val result = merkleTreeService.getRoot()

        assertThat(result).isEqualTo(CryptoUtils.emptyHash())
    }

    @Test
    fun `GIVEN empty tree WHEN leafCount THEN returns zero`() {
        assertThat(merkleTreeService.leafCount()).isEqualTo(0L)
    }

    @Test
    fun `GIVEN single leaf WHEN append THEN root equals that leaf`() {
        merkleTreeService.append(ANY_LEAF_HASH)

        assertSoftly {
            it.assertThat(merkleTreeService.leafCount()).isEqualTo(1L)
            it.assertThat(merkleTreeService.getRoot()).isEqualTo(ANY_LEAF_HASH)
        }
    }

    @Test
    fun `GIVEN two leaves WHEN append THEN root is hash of both`() {
        merkleTreeService.append(LEAF_HASH_A)
        merkleTreeService.append(LEAF_HASH_B)

        val expectedRoot = CryptoUtils.sha256HexConcat(LEAF_HASH_A, LEAF_HASH_B)

        assertSoftly {
            it.assertThat(merkleTreeService.leafCount()).isEqualTo(2L)
            it.assertThat(merkleTreeService.getRoot()).isEqualTo(expectedRoot)
        }
    }

    @Test
    fun `GIVEN three leaves WHEN append THEN root is correctly computed`() {
        merkleTreeService.append(LEAF_HASH_A)
        merkleTreeService.append(LEAF_HASH_B)
        merkleTreeService.append(LEAF_HASH_C)

        val leftParent = CryptoUtils.sha256HexConcat(LEAF_HASH_A, LEAF_HASH_B)
        val rightParent = CryptoUtils.sha256HexConcat(LEAF_HASH_C, CryptoUtils.emptyHash())
        val expectedRoot = CryptoUtils.sha256HexConcat(leftParent, rightParent)

        assertThat(merkleTreeService.getRoot()).isEqualTo(expectedRoot)
    }

    @Test
    fun `GIVEN blank leafHash WHEN append THEN throws IllegalArgumentException`() {
        assertThatThrownBy { merkleTreeService.append("   ") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `GIVEN two leaves WHEN generateProof for index 0 THEN returns sibling on RIGHT`() {
        merkleTreeService.append(LEAF_HASH_A)
        merkleTreeService.append(LEAF_HASH_B)

        val proof = merkleTreeService.generateProof(0)

        assertSoftly {
            it.assertThat(proof).hasSize(1)
            it.assertThat(proof[0].siblingHash).isEqualTo(LEAF_HASH_B)
            it.assertThat(proof[0].position).isEqualTo(HashPosition.RIGHT)
        }
    }

    @Test
    fun `GIVEN two leaves WHEN generateProof for index 1 THEN returns sibling on LEFT`() {
        merkleTreeService.append(LEAF_HASH_A)
        merkleTreeService.append(LEAF_HASH_B)

        val proof = merkleTreeService.generateProof(1)

        assertSoftly {
            it.assertThat(proof).hasSize(1)
            it.assertThat(proof[0].siblingHash).isEqualTo(LEAF_HASH_A)
            it.assertThat(proof[0].position).isEqualTo(HashPosition.LEFT)
        }
    }

    @Test
    fun `GIVEN valid proof WHEN verifyProof THEN returns true`() {
        merkleTreeService.append(LEAF_HASH_A)
        merkleTreeService.append(LEAF_HASH_B)

        val proof = merkleTreeService.generateProof(0)
        val result = merkleTreeService.verifyProof(LEAF_HASH_A, 0, proof)

        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN tampered leaf WHEN verifyProof THEN returns false`() {
        merkleTreeService.append(LEAF_HASH_A)
        merkleTreeService.append(LEAF_HASH_B)

        val proof = merkleTreeService.generateProof(0)
        val result = merkleTreeService.verifyProof(TAMPERED_HASH, 0, proof)

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN out of bounds index WHEN generateProof THEN throws IllegalArgumentException`() {
        merkleTreeService.append(LEAF_HASH_A)

        assertThatThrownBy { merkleTreeService.generateProof(5) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `GIVEN out of bounds index WHEN verifyProof THEN returns false`() {
        merkleTreeService.append(LEAF_HASH_A)

        val result = merkleTreeService.verifyProof(LEAF_HASH_A, 99, emptyList())

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN four leaves WHEN getDepth THEN returns 3`() {
        merkleTreeService.append(LEAF_HASH_A)
        merkleTreeService.append(LEAF_HASH_B)
        merkleTreeService.append(LEAF_HASH_C)
        merkleTreeService.append(LEAF_HASH_D)

        assertThat(merkleTreeService.getDepth()).isEqualTo(3)
    }
}

private const val ANY_LEAF_HASH = "abc123def456"
private const val LEAF_HASH_A = "aaaa"
private const val LEAF_HASH_B = "bbbb"
private const val LEAF_HASH_C = "cccc"
private const val LEAF_HASH_D = "dddd"
private const val TAMPERED_HASH = "ffffffffffffffffffffffffffffffffffffffff"

