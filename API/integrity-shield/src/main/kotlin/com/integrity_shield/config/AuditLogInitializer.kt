package com.integrity_shield.config

import com.integrity_shield.domain.exception.AuditInitializationException
import com.integrity_shield.domain.port.inbound.AuditVerificationUseCase
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Component

@Component
class AuditLogInitializer(
    private val auditVerificationUseCase: AuditVerificationUseCase
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun initializeAuditLog() {
        log.info("Initializing audit log from persisted state...")
        try {
            auditVerificationUseCase.reconstructTreeFromPersistence()
            log.info("Audit log successfully initialized")
        } catch (e: DataAccessException) {
            log.error("Database error during audit log initialization: {}", e.message, e)
            throw AuditInitializationException("Audit log initialization failed due to database error", e)
        } catch (e: IllegalStateException) {
            log.error("Illegal state during audit log initialization: {}", e.message, e)
            throw AuditInitializationException("Audit log initialization failed due to invalid state", e)
        }
    }
}

