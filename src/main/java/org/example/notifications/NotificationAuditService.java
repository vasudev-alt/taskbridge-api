package org.example.notifications;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationAuditService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    public Notification createNotification(UUID projectId, String message) {
        Notification notification = new Notification();
        notification.setProjectId(projectId);
        notification.setMessage(message);
        notification.setTimestamp(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    public AuditLog createAuditLog(UUID projectId, String eventType, String changedBy, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setProjectId(projectId);
        auditLog.setEventType(eventType);
        auditLog.setChangedBy(changedBy);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setDetails(details);
        return auditLogRepository.save(auditLog);
    }

    public List<AuditLog> getAuditLogs(UUID projectId, LocalDateTime startDate, LocalDateTime endDate, String eventType) {
        // Implement filtering logic based on parameters
        return auditLogRepository.findByProjectIdAndFilters(projectId, startDate, endDate, eventType);
    }
}
