package com.integrity_shield.domain.exception

class StorageUploadException(val fileName: String, cause: Throwable? = null) :
    RuntimeException("Failed to upload file '$fileName' to storage", cause)
