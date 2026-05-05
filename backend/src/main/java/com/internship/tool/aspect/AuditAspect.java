package com.internship.tool.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.internship.tool.entity.AuditLog;
import com.internship.tool.entity.ComplianceObligation;
import com.internship.tool.repository.AuditLogRepository;
import com.internship.tool.repository.ComplianceObligationRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Aspect
@Component
public class AuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogRepository auditLogRepository;
    private final ComplianceObligationRepository obligationRepository;
    private final ObjectMapper objectMapper;

    public AuditAspect(AuditLogRepository auditLogRepository,
                       ComplianceObligationRepository obligationRepository) {
        this.auditLogRepository    = auditLogRepository;
        this.obligationRepository  = obligationRepository;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.INDENT_OUTPUT); // compact for DB storage
    }

    /**
     * Intercept only ComplianceObligationService write methods.
     *
     * Pointcut is intentionally narrow — only the service that owns entity
     * mutations. AlertScheduler and EmailService are excluded.
     */
    @Around("execution(* com.internship.tool.service.ComplianceObligationService." +
            "create(..)) || " +
            "execution(* com.internship.tool.service.ComplianceObligationService." +
            "update(..)) || " +
            "execution(* com.internship.tool.service.ComplianceObligationService." +
            "delete(..))")
    public Object auditWriteMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String action     = methodName.toUpperCase(); // CREATE / UPDATE / DELETE

        // ── Capture old state BEFORE the operation ────────────────────────────
        String oldData  = captureOldState(methodName, joinPoint.getArgs());
        Long   entityId = extractEntityId(joinPoint.getArgs());

        // ── Execute the actual service method ─────────────────────────────────
        Object result = joinPoint.proceed();

        // ── Capture new state AFTER the operation ─────────────────────────────
        String entityType = resolveEntityType(result, methodName);
        if (entityId == null) {
            entityId = extractEntityId(result);
        }
        String newData = "DELETE".equals(action) ? null : serializeObject(result);

        saveAuditLog(entityType, entityId, action, oldData, newData);
        return result;
    }

    // ── Old-state capture ─────────────────────────────────────────────────────

    /**
     * For UPDATE and DELETE, load the current entity from the DB so that
     * oldData reflects the actual state before the change.
     *
     * Bug fix: the previous implementation serialized the *input arguments*
     * as oldData, which for update() meant oldData == newData (both showed
     * the new values). Now oldData is the entity as it exists in the DB
     * before the operation runs.
     */
    private String captureOldState(String methodName, Object[] args) {
        if ("update".equals(methodName) || "delete".equals(methodName)) {
            Long id = extractLongArg(args);
            if (id != null) {
                return obligationRepository.findById(id)
                        .map(this::serializeObject)
                        .orElse(null);
            }
        }
        // For create, there is no old state
        return null;
    }

    // ── Entity type resolution ────────────────────────────────────────────────

    private String resolveEntityType(Object result, String methodName) {
        if ("delete".equals(methodName)) {
            return "ComplianceObligation";
        }
        if (result == null || result instanceof String) {
            return "ComplianceObligation";
        }
        return result.getClass().getSimpleName();
    }

    // ── ID extraction ─────────────────────────────────────────────────────────

    private Long extractEntityId(Object[] args) {
        if (args == null) return null;
        Long id = extractLongArg(args);
        if (id != null) return id;
        for (Object arg : args) {
            if (arg instanceof ComplianceObligation) {
                return ((ComplianceObligation) arg).getId();
            }
        }
        return null;
    }

    private Long extractEntityId(Object result) {
        if (result instanceof ComplianceObligation) {
            return ((ComplianceObligation) result).getId();
        }
        return null;
    }

    private Long extractLongArg(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg instanceof Long) return (Long) arg;
        }
        return null;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    private String serializeObject(Object object) {
        if (object == null) return null;
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            logger.warn("Audit serialization failed: {}", e.getMessage());
            return null;
        }
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void saveAuditLog(String entityType, Long entityId,
                               String action, String oldData, String newData) {
        try {
            AuditLog log = new AuditLog(
                    entityType != null ? entityType : "ComplianceObligation",
                    entityId, action, oldData, newData, LocalDateTime.now());
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Audit failure must never crash the main operation
            logger.error("Audit log save failed for action={} entityId={}: {}",
                    action, entityId, e.getMessage());
        }
    }
}
