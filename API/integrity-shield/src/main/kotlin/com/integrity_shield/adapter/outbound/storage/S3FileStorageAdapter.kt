package com.integrity_shield.adapter.outbound.storage

import com.integrity_shield.domain.exception.StorageUploadException
import com.integrity_shield.domain.port.outbound.FileStoragePort
import com.integrity_shield.domain.port.outbound.StorageResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.*

@Service
class S3FileStorageAdapter(
    private val s3Client: S3Client
) : FileStoragePort {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value($$"${aws.s3.bucket-name}")
    lateinit var bucketName: String

    override fun upload(fileName: String, content: ByteArray, size: Long): StorageResult? {
        require(bucketName.isNotBlank()) { "S3 bucket name must not be blank" }
        require(fileName.isNotBlank()) { "File name must not be blank" }
        require(size > 0) { "File size must be greater than 0" }

        return try {
            val md5Hash = calculateMD5(content)

            val putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentLength(size)
                .contentMD5(md5Hash)
                .build()

            val res = s3Client.putObject(putRequest, RequestBody.fromInputStream(
                ByteArrayInputStream(content), size
            ))
            log.info("S3 uploaded {} to bucket {} ({} bytes)", fileName, bucketName, size)

            StorageResult(versionId = res.versionId() ?: "unknown")
        } catch (e: S3Exception) {
            log.error("S3 service error uploading file {}: status={}, message={}", fileName, e.statusCode(), e.awsErrorDetails()?.errorMessage(), e)
            throw StorageUploadException(fileName, e)
        } catch (e: SdkClientException) {
            log.error("S3 client error uploading file {}: {}", fileName, e.message, e)
            throw StorageUploadException(fileName, e)
        }
    }

    private fun calculateMD5(data: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val messageDigest = md.digest(data)
        return Base64.getEncoder().encodeToString(messageDigest)
    }
}

