package com.internship.tool.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.internship.tool.entity.AuditLog;
import com.internship.tool.repository.AuditLogRepository;
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
    private final ObjectMapper objectMapper;

    public AuditAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Around("execution(* com.internship.tool.service.*.*(..))")
    public Object auditServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String lowerMethod = methodName.toLowerCase();

        if (!isAuditableMethod(lowerMethod)) {
            return joinPoint.proceed();
        }

        String action = determineAction(lowerMethod);
        String entityType = resolveEntityType(joinPoint.getArgs());
        Long entityId = extractEntityId(joinPoint.getArgs());
        String oldData = serializeArguments(joinPoint.getArgs());

        Object result = joinPoint.proceed();

        if (entityType == null) {
            entityType = resolveEntityType(result);
        }

        if (entityId == null) {
            entityId = extractEntityId(result);
        }

        String newData = serializeObject(result);

        saveAuditLog(entityType, entityId, action, oldData, newData);

        return result;
    }

    private boolean isAuditableMethod(String methodName) {
        return methodName.contains("create") || methodName.contains("save") || methodName.contains("update") || methodName.contains("delete");
    }

    private String determineAction(String methodName) {
        if (methodName.contains("create") || methodName.contains("save")) {
            return "CREATE";
        }
        if (methodName.contains("update")) {
            return "UPDATE";
        }
        if (methodName.contains("delete") || methodName.contains("remove")) {
            return "DELETE";
        }
        return "UNKNOWN";
    }

    private String resolveEntityType(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        for (Object arg : args) {
            if (arg != null && !(arg instanceof String) && !(arg instanceof Number)) {
                return arg.getClass().getSimpleName();
            }
        }
        return null;
    }

    private String resolveEntityType(Object result) {
        if (result == null) {
            return null;
        }
        return result.getClass().getSimpleName();
    }

    private Long extractEntityId(Object[] args) {
        if (args == null) {
            return null;
        }

        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
            if (arg != null) {
                Long id = extractIdFromObject(arg);
                if (id != null) {
                    return id;
                }
            }
        }
        return null;
    }

    private Long extractEntityId(Object result) {
        if (result == null) {
            return null;
        }
        return extractIdFromObject(result);
    }

    private Long extractIdFromObject(Object object) {
        try {
            var idField = object.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            Object idValue = idField.get(object);
            if (idValue instanceof Long) {
                return (Long) idValue;
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // Skip if the object does not have an id field
        }
        return null;
    }

    private String serializeArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(args);
        } catch (Exception e) {
            logger.warn("Unable to serialize method arguments for audit logging: {}", e.getMessage());
            return null;
        }
    }

    private String serializeObject(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            logger.warn("Unable to serialize result object for audit logging: {}", e.getMessage());
            return null;
        }
    }

    private void saveAuditLog(String entityType, Long entityId, String action, String oldData, String newData) {
        try {
            if (entityType == null) {
                entityType = "UnknownEntity";
            }
            AuditLog auditLog = new AuditLog(entityType, entityId, action, oldData, newData, LocalDateTime.now());
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            logger.error("Audit logging failed: {}", e.getMessage());
        }
    }
}