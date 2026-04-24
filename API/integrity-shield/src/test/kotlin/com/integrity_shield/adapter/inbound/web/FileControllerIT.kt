package com.integrity_shield.adapter.inbound.web

import com.integrity_shield.domain.model.DomainFileVersion
import com.integrity_shield.domain.port.inbound.FileUploadUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@WebMvcTest(FileController::class)
class FileControllerIT {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var fileUploadUseCase: FileUploadUseCase

    @TestConfiguration
    class TestConfig {
        @Bean
        fun fileUploadUseCase(): FileUploadUseCase = mock()
    }

    @Test
    fun `GIVEN valid file WHEN POST upload THEN returns 201 with upload response`() {
        val fileVersion = DomainFileVersion(
            id = 1L, fileIdentifier = ANY_FILE_NAME, storageVersionId = ANY_VERSION_ID,
            contentHash = ANY_CONTENT_HASH, uploadTimestamp = FIXED_TIMESTAMP,
            leafHash = "leafHash", merkleLeafIndex = 0L
        )
        val result = FileUploadUseCase.UploadAndAppendResult(
            fileVersion = fileVersion, contentHash = ANY_CONTENT_HASH,
            storageVersionId = ANY_VERSION_ID, currentRoot = ANY_ROOT, leafCount = 1L
        )
        whenever(fileUploadUseCase.uploadAndAppend(eq(ANY_FILE_NAME), any(), any())).thenReturn(result)

        val file = MockMultipartFile("file", ANY_FILE_NAME, MediaType.APPLICATION_OCTET_STREAM_VALUE, ANY_FILE_CONTENT)

        mockMvc.perform(multipart("/api/v1/files/upload").file(file))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.fileIdentifier").value(ANY_FILE_NAME))
            .andExpect(jsonPath("$.storageVersionId").value(ANY_VERSION_ID))
            .andExpect(jsonPath("$.contentHash").value(ANY_CONTENT_HASH))
            .andExpect(jsonPath("$.leafCount").value(1))
    }

    @Test
    fun `GIVEN empty file WHEN POST upload THEN returns 400`() {
        val file = MockMultipartFile("file", ANY_FILE_NAME, MediaType.APPLICATION_OCTET_STREAM_VALUE, ByteArray(0))

        mockMvc.perform(multipart("/api/v1/files/upload").file(file))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("File is empty"))
    }

    @Test
    fun `GIVEN duplicate file WHEN POST upload THEN returns 200 with idempotent status`() {
        val result = FileUploadUseCase.UploadAndAppendResult(
            fileVersion = null, contentHash = ANY_CONTENT_HASH,
            storageVersionId = ANY_VERSION_ID, currentRoot = ANY_ROOT, leafCount = 1L
        )
        whenever(fileUploadUseCase.uploadAndAppend(eq(ANY_FILE_NAME), any(), any())).thenReturn(result)

        val file = MockMultipartFile("file", ANY_FILE_NAME, MediaType.APPLICATION_OCTET_STREAM_VALUE, ANY_FILE_CONTENT)

        mockMvc.perform(multipart("/api/v1/files/upload").file(file))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("idempotent"))
    }
}

private const val ANY_FILE_NAME = "document.pdf"
private const val ANY_VERSION_ID = "v1-abc123"
private const val ANY_CONTENT_HASH = "abcdef1234567890"
private const val ANY_ROOT = "rootHash123"
private val ANY_FILE_CONTENT = "file content".toByteArray()
private val FIXED_TIMESTAMP =Instant.now()

