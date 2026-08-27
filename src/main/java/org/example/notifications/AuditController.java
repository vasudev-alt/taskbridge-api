package org.example.notifications;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @Autowired
    private AuditService auditService;

    /**
     * Internal endpoint: Record an audit event (called by Project Service).
     * This endpoint captures the actor's IP address automatically.
     *
     * @param request the HTTP request (to extract IP)
     * @param auditEventRequest the audit event details
     * @return the created AuditLog entry
     */
    @PostMapping
    public ResponseEntity<AuditLogEntry> recordAuditEvent(
            HttpServletRequest request,
            @RequestBody AuditEventRequest auditEventRequest
    ) {
        String clientIpAddress = getClientIpAddress(request);

        AuditLogEntry auditLog = auditService.recordAuditEvent(
                auditEventRequest.getEventType(),
                auditEventRequest.getEntityType(),
                auditEventRequest.getEntityId(),
                auditEventRequest.getActorUserId(),
                auditEventRequest.getActorOrganisation(),
                clientIpAddress,
                auditEventRequest.getPreviousState(),
                auditEventRequest.getNewState()
        );

        return ResponseEntity.ok(auditLog);
    }

    /**
     * GET /audit/:projectId - Get audit history for a project with optional filters.
     *
     * Query Parameters:
     * - from: Start date (ISO 8601 format, optional)
     * - to: End date (ISO 8601 format, optional)
     * - eventType: Filter by event type (optional)
     *
     * @param projectId the project ID
     * @param from the start date (optional)
     * @param to the end date (optional)
     * @param eventType the event type filter (optional)
     * @return list of audit entries matching the criteria
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<List<AuditLogEntry>> getAuditHistory(
            @PathVariable UUID projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) AuditEventType eventType
    ) {
        List<AuditLogEntry> auditHistory;

        if (from != null && to != null && eventType != null) {
            auditHistory = auditService.getAuditHistoryByEventTypeAndDateRange(projectId, eventType, from, to);
        } else if (from != null && to != null) {
            auditHistory = auditService.getAuditHistoryByDateRange(projectId, from, to);
        } else if (eventType != null) {
            auditHistory = auditService.getAuditHistoryByEventType(projectId, eventType);
        } else {
            auditHistory = auditService.getAuditHistoryByProjectId(projectId);
        }

        return ResponseEntity.ok(auditHistory);
    }

    /**
     * Extracts the client IP address from the HTTP request.
     * Handles X-Forwarded-For header for proxy scenarios.
     *
     * @param request the HTTP request
     * @return the client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
