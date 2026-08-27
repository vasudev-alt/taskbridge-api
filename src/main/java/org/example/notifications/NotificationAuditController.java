package org.example.notifications;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationAuditController {

    @Autowired
    private NotificationAuditService notificationAuditService;

    @PostMapping
    public ResponseEntity<Notification> createNotification(@RequestBody NotificationRequest request) {
        Notification notification = notificationAuditService.createNotification(request.getProjectId(), request.getMessage());
        return ResponseEntity.ok(notification);
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs(@RequestParam UUID projectId,
                                                       @RequestParam(required = false) LocalDateTime startDate,
                                                       @RequestParam(required = false) LocalDateTime endDate,
                                                       @RequestParam(required = false) String eventType) {
        List<AuditLog> auditLogs = notificationAuditService.getAuditLogs(projectId, startDate, endDate, eventType);
        return ResponseEntity.ok(auditLogs);
    }

    public static class NotificationRequest {
        private UUID projectId;
        private String message;

        public UUID getProjectId() {
            return projectId;
        }

        public void setProjectId(UUID projectId) {
            this.projectId = projectId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
