package com.integrity_shield.application

import com.integrity_shield.domain.exception.StorageUploadException
import com.integrity_shield.domain.model.DomainFileVersion
import com.integrity_shield.domain.port.inbound.AuditVerificationUseCase
import com.integrity_shield.domain.port.outbound.FileStoragePort
import com.integrity_shield.domain.port.outbound.StorageResult
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

class FileUploadUseCaseImplUT {

    private val fileStoragePort: FileStoragePort = mock()
    private val auditVerificationUseCase: AuditVerificationUseCase = mock()

    private val fileUploadUseCase = FileUploadUseCaseImpl(
        fileStoragePort,
        auditVerificationUseCase
    )

    @BeforeEach
    fun init() {
        reset(fileStoragePort, auditVerificationUseCase)
    }

    @Test
    fun `GIVEN valid file WHEN uploadAndAppend THEN stores file and appends to audit log`() {
        whenever(fileStoragePort.upload(eq(ANY_FILE_NAME), any(), eq(ANY_FILE_SIZE)))
            .thenReturn(StorageResult(ANY_VERSION_ID))
        whenever(auditVerificationUseCase.appendFileVersion(eq(ANY_FILE_NAME), eq(ANY_VERSION_ID), any(), any()))
            .thenReturn(ANY_DOMAIN_FILE_VERSION)
        whenever(auditVerificationUseCase.getCurrentRoot()).thenReturn(ANY_ROOT)
        whenever(auditVerificationUseCase.getCurrentLeafCount()).thenReturn(1L)

        val result = fileUploadUseCase.uploadAndAppend(ANY_FILE_NAME, ANY_FILE_CONTENT, ANY_FILE_SIZE)

        assertSoftly {
            it.assertThat(result.fileVersion).isNotNull
            it.assertThat(result.storageVersionId).isEqualTo(ANY_VERSION_ID)
            it.assertThat(result.currentRoot).isEqualTo(ANY_ROOT)
            it.assertThat(result.leafCount).isEqualTo(1L)
        }
        verify(fileStoragePort).upload(eq(ANY_FILE_NAME), any(), eq(ANY_FILE_SIZE))
        verify(auditVerificationUseCase).appendFileVersion(eq(ANY_FILE_NAME), eq(ANY_VERSION_ID), any(), any())
    }

    @Test
    fun `GIVEN S3 upload fails WHEN uploadAndAppend THEN throws StorageUploadException`() {
        whenever(fileStoragePort.upload(any(), any(), any())).thenReturn(null)

        assertThatThrownBy { fileUploadUseCase.uploadAndAppend(ANY_FILE_NAME, ANY_FILE_CONTENT, ANY_FILE_SIZE) }
            .isInstanceOf(StorageUploadException::class.java)
            .hasMessageContaining(ANY_FILE_NAME)
    }

    @Test
    fun `GIVEN duplicate file WHEN uploadAndAppend THEN returns null fileVersion`() {
        whenever(fileStoragePort.upload(eq(ANY_FILE_NAME), any(), eq(ANY_FILE_SIZE)))
            .thenReturn(StorageResult(ANY_VERSION_ID))
        whenever(auditVerificationUseCase.appendFileVersion(eq(ANY_FILE_NAME), eq(ANY_VERSION_ID), any(), any()))
            .thenReturn(null)
        whenever(auditVerificationUseCase.getCurrentRoot()).thenReturn(ANY_ROOT)
        whenever(auditVerificationUseCase.getCurrentLeafCount()).thenReturn(1L)

        val result = fileUploadUseCase.uploadAndAppend(ANY_FILE_NAME, ANY_FILE_CONTENT, ANY_FILE_SIZE)

        assertThat(result.fileVersion).isNull()
    }
}

private const val ANY_FILE_NAME = "document.pdf"
private const val ANY_VERSION_ID = "v1-abc123"
private const val ANY_ROOT = "rootHash123"
private val ANY_FILE_CONTENT = "file content".toByteArray()
private const val ANY_FILE_SIZE = 12L
private val ANY_DOMAIN_FILE_VERSION = DomainFileVersion(
    id = 1L,
    fileIdentifier = ANY_FILE_NAME,
    storageVersionId = ANY_VERSION_ID,
    contentHash = "contenthash",
    uploadTimestamp = Instant.now(),
    leafHash = "leafHash",
    merkleLeafIndex = 0L
)

