package com.integrity_shield.adapter.inbound.web

import com.integrity_shield.adapter.inbound.web.dto.ErrorResponseDto
import com.integrity_shield.adapter.inbound.web.dto.UploadResponseDto
import com.integrity_shield.domain.port.inbound.FileUploadUseCase
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.Instant

@RestController
@RequestMapping("/api/v1/files")
class FileController(
    private val fileUploadUseCase: FileUploadUseCase
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Value($$"${aws.s3.max-file-size-bytes}")
    lateinit var maxFileSizeConfig: String

    @PostMapping("/upload")
    @Transactional
    fun uploadFile(@RequestParam("file") file: MultipartFile): ResponseEntity<Any> {

        if (file.isEmpty) {
            log.warn("Received empty file: {}", file.originalFilename)
            return ResponseEntity.badRequest().body(ErrorResponseDto(error = "File is empty"))
        }

        if (file.size > maxFileSizeConfig.toLong()) {
            log.warn("File {} exceeds max size of {} bytes", file.originalFilename, maxFileSizeConfig.toLong())
            return ResponseEntity.badRequest().body(ErrorResponseDto(error = "File exceeds maximum size"))
        }

        try {
            val fileName = file.originalFilename ?: "unnamed"
            val result = fileUploadUseCase.uploadAndAppend(
                fileName = fileName,
                fileContent = file.bytes,
                fileSize = file.size
            )

            if (result.fileVersion == null) {
                log.info("File version already in audit log (idempotent): $fileName")
                return ResponseEntity.ok().body(UploadResponseDto(
                    id = 0,
                    fileIdentifier = fileName,
                    storageVersionId = result.storageVersionId,
                    contentHash = result.contentHash,
                    leafIndex = 0,
                    uploadTimestamp = Instant.now(),
                    currentRoot = result.currentRoot,
                    leafCount = result.leafCount,
                    message = "File already audited",
                    status = "idempotent"
                ))
            }

            log.info("File uploaded and appended to audit log: $fileName")
            return ResponseEntity.status(201).body(UploadResponseDto(
                id = result.fileVersion.id,
                fileIdentifier = fileName,
                storageVersionId = result.storageVersionId,
                contentHash = result.contentHash,
                leafIndex = result.fileVersion.merkleLeafIndex,
                uploadTimestamp = result.fileVersion.uploadTimestamp,
                currentRoot = result.currentRoot,
                leafCount = result.leafCount,
                message = "File uploaded and appended to audit log"
            ))
        } catch (e: Exception) {
            log.error("Error during file upload and audit: {}", e.message, e)
            return ResponseEntity.internalServerError().body(ErrorResponseDto(error = e.message ?: "Unknown error"))
        }
    }
}

