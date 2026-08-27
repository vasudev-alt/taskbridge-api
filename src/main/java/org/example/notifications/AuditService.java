package org.example.notifications;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * AuditService manages immutable audit log entries.
 * Audit entries cannot be updated or deleted once created.
 */
@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
 * Records an audit event. This method enforces immutability by only allowing inserts.
 * @param eventType the type of event (PROJECT_CREATED, PROJECT_STATUS_UPDATED, etc.)
 * @param entityType the type of entity (e.g., "PROJECT")
 * @param entityId the ID of the entity
 * @param actorUserId the user ID of the actor performing the action
 * @param actorOrganisation the organisation of the actor
 * @param actorIpAddress the IP address of the actor
 * @param previousState the state before the change (can be null for creation events)
 * @param newState the state after the change
 * @return the created AuditLog entry
 */
    public AuditLogEntry recordAuditEvent(
            AuditEventType eventType,
            String entityType,
            UUID entityId,
            UUID actorUserId,
            String actorOrganisation,
            String actorIpAddress,
            JsonNode previousState,
            JsonNode newState
    ) {
        AuditLogEntry auditLog = new AuditLogEntry();
        auditLog.setEventType(eventType);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setActorUserId(actorUserId);
        auditLog.setActorOrganisation(actorOrganisation);
        auditLog.setActorIpAddress(actorIpAddress);
        auditLog.setPreviousState(previousState);
        auditLog.setNewState(newState);

        return auditLogRepository.save(auditLog);
    }

    /**
 * Retrieves audit history for a specific project.
 * @param projectId the project ID
 * @return list of audit entries ordered by creation date (newest first)
 */
    public List<AuditLogEntry> getAuditHistoryByProjectId(UUID projectId) {
        return auditLogRepository.findByEntityIdOrderByCreatedAtDesc(projectId);
    }

    /**
 * Retrieves audit history filtered by date range.
 * @param projectId the project ID
 * @param fromDate start date (inclusive)
 * @param toDate end date (inclusive)
 * @return filtered list of audit entries
 */
    public List<AuditLogEntry> getAuditHistoryByDateRange(UUID projectId, LocalDateTime fromDate, LocalDateTime toDate) {
        return auditLogRepository.findByEntityIdAndDateRange(projectId, fromDate, toDate);
    }

    /**
 * Retrieves audit history filtered by event type.
 * @param projectId the project ID
 * @param eventType the type of event to filter by
 * @return filtered list of audit entries
 */
    public List<AuditLogEntry> getAuditHistoryByEventType(UUID projectId, AuditEventType eventType) {
        return auditLogRepository.findByEntityIdAndEventType(projectId, eventType);
    }

    /**
 * Retrieves audit history filtered by both event type and date range.
 * @param projectId the project ID
 * @param eventType the type of event to filter by
 * @param fromDate start date (inclusive)
 * @param toDate end date (inclusive)
 * @return filtered list of audit entries
 */
    public List<AuditLogEntry> getAuditHistoryByEventTypeAndDateRange(
            UUID projectId,
            AuditEventType eventType,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        return auditLogRepository.findByEntityIdEventTypeAndDateRange(projectId, eventType, fromDate, toDate);
    }

    /**
 * Enforces immutability: audit entries cannot be updated.
 * @throws UnsupportedOperationException always
 */
    public void updateAuditLog(UUID auditLogId, AuditLogEntry updatedLog) {
        throw new UnsupportedOperationException("Audit logs are immutable and cannot be updated.");
    }

    /**
 * Enforces immutability: audit entries cannot be deleted.
 * @throws UnsupportedOperationException always
 */
    public void deleteAuditLog(UUID auditLogId) {
        throw new UnsupportedOperationException("Audit logs are immutable and cannot be deleted.");
    }
}
