package org.example.notifications;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationAuditService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationAuditLogRepository notificationAuditLogRepository;

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
        return notificationAuditLogRepository.save(auditLog);
    }

    public List<AuditLog> getAuditLogs(UUID projectId, LocalDateTime startDate, LocalDateTime endDate, String eventType) {
        // Fetch by project and then apply optional filters in-memory for simplicity
        List<AuditLog> all = notificationAuditLogRepository.findByProjectIdOrderByTimestampDesc(projectId);

        return all.stream()
                .filter(a -> (startDate == null || !a.getTimestamp().isBefore(startDate)))
                .filter(a -> (endDate == null || !a.getTimestamp().isAfter(endDate)))
                .filter(a -> (eventType == null || eventType.isEmpty() || eventType.equals(a.getEventType())))
                .collect(Collectors.toList());
    }
}
