package com.integrity_shield.adapter.inbound.web

import com.integrity_shield.domain.model.DomainFileVersion
import com.integrity_shield.domain.model.DomainMerkleProofData
import com.integrity_shield.domain.model.HashPosition
import com.integrity_shield.domain.model.ProofElement
import com.integrity_shield.domain.port.inbound.AuditVerificationUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@WebMvcTest(AuditVerificationController::class)
class AuditVerificationControllerIT {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var auditVerificationUseCase: AuditVerificationUseCase

    @TestConfiguration
    class TestConfig {
        @Bean
        fun auditVerificationUseCase(): AuditVerificationUseCase = mock()
    }

    @Test
    fun `GIVEN tree with leaves WHEN GET current-root THEN returns root and leaf count`() {
        whenever(auditVerificationUseCase.getCurrentRoot()).thenReturn(ANY_ROOT)
        whenever(auditVerificationUseCase.getCurrentLeafCount()).thenReturn(5L)

        mockMvc.perform(get("/api/v1/audit/current-root"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentRoot").value(ANY_ROOT))
            .andExpect(jsonPath("$.leafCount").value(5))
    }

    @Test
    fun `GIVEN valid request WHEN POST verify-inclusion THEN returns verification result`() {
        whenever(auditVerificationUseCase.verifyFileInclusion(any(), any(), any(), any(), any())).thenReturn(true)

        val requestBody = """
            {
                "fileIdentifier": "$ANY_FILE_ID",
                "storageVersionId": "$ANY_VERSION_ID",
                "contentHash": "$ANY_CONTENT_HASH",
                "uploadTimestamp": "$FIXED_TIMESTAMP",
                "knownRoot": "$ANY_ROOT"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/audit/verify-inclusion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.verified").value(true))
            .andExpect(jsonPath("$.fileIdentifier").value(ANY_FILE_ID))
    }

    @Test
    fun `GIVEN existing proof WHEN GET proof by fileVersionId THEN returns proof data`() {
        val proof = DomainMerkleProofData(
            id = 1L, leafHash = ANY_LEAF_HASH, leafIndex = 0L,
            proofPath = listOf(ProofElement(siblingHash = ANY_SIBLING_HASH, position = HashPosition.RIGHT)),
            rootHash = ANY_ROOT, fileVersionId = 1L
        )
        whenever(auditVerificationUseCase.getProofForFileVersion(1L)).thenReturn(proof)

        mockMvc.perform(get("/api/v1/audit/proof/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.leafHash").value(ANY_LEAF_HASH))
            .andExpect(jsonPath("$.rootHash").value(ANY_ROOT))
            .andExpect(jsonPath("$.proofPath[0].siblingHash").value(ANY_SIBLING_HASH))
            .andExpect(jsonPath("$.proofPath[0].position").value("RIGHT"))
    }

    @Test
    fun `GIVEN no proof WHEN GET proof by fileVersionId THEN returns 404`() {
        whenever(auditVerificationUseCase.getProofForFileVersion(999L)).thenReturn(null)

        mockMvc.perform(get("/api/v1/audit/proof/999"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GIVEN file versions WHEN GET all-versions THEN returns list`() {
        val version = DomainFileVersion(
            id = 1L, fileIdentifier = ANY_FILE_ID, storageVersionId = ANY_VERSION_ID,
            contentHash = ANY_CONTENT_HASH, uploadTimestamp = FIXED_TIMESTAMP,
            leafHash = ANY_LEAF_HASH, merkleLeafIndex = 0L
        )
        whenever(auditVerificationUseCase.getAllFileVersions()).thenReturn(listOf(version))

        mockMvc.perform(get("/api/v1/audit/all-versions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.versions[0].fileIdentifier").value(ANY_FILE_ID))
    }

    @Test
    fun `GIVEN file identifier WHEN GET versions THEN returns versions for that file`() {
        val version = DomainFileVersion(
            id = 1L, fileIdentifier = ANY_FILE_ID, storageVersionId = ANY_VERSION_ID,
            contentHash = ANY_CONTENT_HASH, uploadTimestamp = FIXED_TIMESTAMP,
            leafHash = ANY_LEAF_HASH, merkleLeafIndex = 0L
        )
        whenever(auditVerificationUseCase.getFileVersions(ANY_FILE_ID)).thenReturn(listOf(version))

        mockMvc.perform(get("/api/v1/audit/versions/$ANY_FILE_ID"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fileIdentifier").value(ANY_FILE_ID))
            .andExpect(jsonPath("$.count").value(1))
    }
}

private const val ANY_FILE_ID = "document.pdf"
private const val ANY_VERSION_ID = "v1-abc123"
private const val ANY_CONTENT_HASH = "abcdef1234567890"
private const val ANY_ROOT = "rootHash123"
private const val ANY_LEAF_HASH = "leafHash123"
private const val ANY_SIBLING_HASH = "siblingHash456"
private val FIXED_TIMESTAMP = Instant.now()
