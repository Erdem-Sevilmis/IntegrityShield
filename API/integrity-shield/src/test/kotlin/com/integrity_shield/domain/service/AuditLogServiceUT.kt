package com.integrity_shield.domain.service

import com.integrity_shield.crypto.CryptoUtils
import com.integrity_shield.domain.model.DomainFileVersion
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class AuditLogServiceUT {

    private lateinit var auditLogService: AuditLogService

    @BeforeEach
    fun init() {
        auditLogService = AuditLogService()
    }

    @Test
    fun `GIVEN file metadata WHEN deriveLeafHash THEN returns deterministic SHA-256 hash`() {
        val hash1 = auditLogService.deriveLeafHash(ANY_FILE_ID, ANY_VERSION_ID, ANY_CONTENT_HASH, FIXED_TIMESTAMP)
        val hash2 = auditLogService.deriveLeafHash(ANY_FILE_ID, ANY_VERSION_ID, ANY_CONTENT_HASH, FIXED_TIMESTAMP)

        assertSoftly {
            it.assertThat(hash1).hasSize(64)
            it.assertThat(hash1).isEqualTo(hash2)
        }
    }

    @Test
    fun `GIVEN different file identifiers WHEN deriveLeafHash THEN returns different hashes`() {
        val hash1 = auditLogService.deriveLeafHash(ANY_FILE_ID, ANY_VERSION_ID, ANY_CONTENT_HASH, FIXED_TIMESTAMP)
        val hash2 = auditLogService.deriveLeafHash(ANOTHER_FILE_ID, ANY_VERSION_ID, ANY_CONTENT_HASH, FIXED_TIMESTAMP)

        assertThat(hash1).isNotEqualTo(hash2)
    }

    @Test
    fun `GIVEN valid leaf hash WHEN appendLeaf THEN returns leaf index and updates root`() {
        val leafHash = auditLogService.deriveLeafHash(ANY_FILE_ID, ANY_VERSION_ID, ANY_CONTENT_HASH, FIXED_TIMESTAMP)

        val index = auditLogService.appendLeaf(leafHash)

        assertSoftly {
            it.assertThat(index).isEqualTo(0L)
            it.assertThat(auditLogService.getCurrentLeafCount()).isEqualTo(1L)
            it.assertThat(auditLogService.getCurrentRoot()).isNotEqualTo(CryptoUtils.emptyHash())
        }
    }

    @Test
    fun `GIVEN blank leaf hash WHEN appendLeaf THEN throws IllegalArgumentException`() {
        assertThatThrownBy { auditLogService.appendLeaf("  ") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `GIVEN empty tree WHEN getCurrentRoot THEN returns empty hash`() {
        assertThat(auditLogService.getCurrentRoot()).isEqualTo(CryptoUtils.emptyHash())
    }

    @Test
    fun `GIVEN one leaf WHEN generateProof for index 0 THEN returns empty proof`() {
        val leafHash = auditLogService.deriveLeafHash(ANY_FILE_ID, ANY_VERSION_ID, ANY_CONTENT_HASH, FIXED_TIMESTAMP)
        auditLogService.appendLeaf(leafHash)

        val proof = auditLogService.generateProof(0)

        assertThat(proof).isNotNull
        assertThat(proof).isEmpty()
    }

    @Test
    fun `GIVEN invalid index WHEN generateProof THEN returns null`() {
        val proof = auditLogService.generateProof(99)

        assertThat(proof).isNull()
    }

    @Test
    fun `GIVEN appended leaf WHEN verifyInclusion with correct root THEN returns true`() {
        val leafHash = auditLogService.deriveLeafHash(ANY_FILE_ID, ANY_VERSION_ID, ANY_CONTENT_HASH, FIXED_TIMESTAMP)
        auditLogService.appendLeaf(leafHash)
        val currentRoot = auditLogService.getCurrentRoot()

        val result = auditLogService.verifyInclusion(leafHash, 0, currentRoot)

        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN appended leaf WHEN verifyInclusion with wrong root THEN returns false`() {
        val leafHash = auditLogService.deriveLeafHash(ANY_FILE_ID, ANY_VERSION_ID, ANY_CONTENT_HASH, FIXED_TIMESTAMP)
        auditLogService.appendLeaf(leafHash)

        val result = auditLogService.verifyInclusion(leafHash, 0, ANY_WRONG_ROOT)

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN persisted versions WHEN reconstructFromFileVersions THEN tree is rebuilt`() {
        val leafHash1 = auditLogService.deriveLeafHash(ANY_FILE_ID, ANY_VERSION_ID, ANY_CONTENT_HASH, FIXED_TIMESTAMP)
        val leafHash2 = auditLogService.deriveLeafHash(ANOTHER_FILE_ID, ANOTHER_VERSION_ID, ANOTHER_CONTENT_HASH, FIXED_TIMESTAMP)
        auditLogService.appendLeaf(leafHash1)
        auditLogService.appendLeaf(leafHash2)
        val originalRoot = auditLogService.getCurrentRoot()

        val versions = listOf(
            DomainFileVersion(id = 1, fileIdentifier = ANY_FILE_ID, storageVersionId = ANY_VERSION_ID, contentHash = ANY_CONTENT_HASH, uploadTimestamp = FIXED_TIMESTAMP, leafHash = leafHash1, merkleLeafIndex = 0),
            DomainFileVersion(id = 2, fileIdentifier = ANOTHER_FILE_ID, storageVersionId = ANOTHER_VERSION_ID, contentHash = ANOTHER_CONTENT_HASH, uploadTimestamp = FIXED_TIMESTAMP, leafHash = leafHash2, merkleLeafIndex = 1)
        )

        val freshService = AuditLogService()
        freshService.reconstructFromFileVersions(versions)

        assertSoftly {
            it.assertThat(freshService.getCurrentRoot()).isEqualTo(originalRoot)
            it.assertThat(freshService.getCurrentLeafCount()).isEqualTo(2L)
        }
    }
}

private const val ANY_FILE_ID = "document.pdf"
private const val ANOTHER_FILE_ID = "report.pdf"
private const val ANY_VERSION_ID = "v1-abc123"
private const val ANOTHER_VERSION_ID = "v2-def456"
private const val ANY_CONTENT_HASH = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890"
private const val ANOTHER_CONTENT_HASH = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
private const val ANY_WRONG_ROOT = "0000000000000000000000000000000000000000000000000000000000000000"
private val FIXED_TIMESTAMP = Instant.now()

