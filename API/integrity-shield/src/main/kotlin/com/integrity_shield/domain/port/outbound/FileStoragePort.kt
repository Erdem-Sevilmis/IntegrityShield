package com.integrity_shield.domain.port.outbound

interface FileStoragePort {
    fun upload(fileName: String, content: ByteArray, size: Long): StorageResult?
}

data class StorageResult(val versionId: String)

