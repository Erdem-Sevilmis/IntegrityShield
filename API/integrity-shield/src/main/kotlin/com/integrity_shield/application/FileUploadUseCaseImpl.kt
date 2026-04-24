package com.integrity_shield.application

import com.integrity_shield.crypto.CryptoUtils
import com.integrity_shield.domain.exception.StorageUploadException
import com.integrity_shield.domain.port.inbound.FileUploadUseCase
import com.integrity_shield.domain.port.inbound.AuditVerificationUseCase
import com.integrity_shield.domain.port.outbound.FileStoragePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
@Transactional
class FileUploadUseCaseImpl(
    private val fileStoragePort: FileStoragePort,
    private val auditVerificationUseCase: AuditVerificationUseCase
) : FileUploadUseCase {


    override fun uploadAndAppend(
        fileName: String,
        fileContent: ByteArray,
        fileSize: Long
    ): FileUploadUseCase.UploadAndAppendResult {
        val uploadResult = fileStoragePort.upload(fileName, fileContent, fileSize)
            ?: throw StorageUploadException(fileName)

        val contentHash = CryptoUtils.sha256Hex(fileContent)
        val storageVersionId = uploadResult.versionId
        val uploadTimestamp = Instant.now().truncatedTo(ChronoUnit.MICROS)

        val fileVersion = auditVerificationUseCase.appendFileVersion(
            fileIdentifier = fileName,
            storageVersionId = storageVersionId,
            contentHash = contentHash,
            uploadTimestamp = uploadTimestamp
        )

        return FileUploadUseCase.UploadAndAppendResult(
            fileVersion = fileVersion,
            contentHash = contentHash,
            storageVersionId = storageVersionId,
            currentRoot = auditVerificationUseCase.getCurrentRoot(),
            leafCount = auditVerificationUseCase.getCurrentLeafCount()
        )
    }
}

