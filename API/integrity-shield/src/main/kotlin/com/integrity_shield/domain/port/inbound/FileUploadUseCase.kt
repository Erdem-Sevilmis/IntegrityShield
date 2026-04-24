package com.integrity_shield.domain.port.inbound

import com.integrity_shield.domain.model.DomainFileVersion

interface FileUploadUseCase {
    fun uploadAndAppend(
        fileName: String,
        fileContent: ByteArray,
        fileSize: Long
    ): UploadAndAppendResult

    data class UploadAndAppendResult(
        val fileVersion: DomainFileVersion?,
        val contentHash: String,
        val storageVersionId: String,
        val currentRoot: String,
        val leafCount: Long
    )
}

