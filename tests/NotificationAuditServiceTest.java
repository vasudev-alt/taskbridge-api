package org.example.notifications;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class NotificationAuditServiceTest {

    @Autowired
    private NotificationAuditService notificationAuditService;

    @Test
    public void testCreateNotification() {
        UUID projectId = UUID.randomUUID();
        String message = "Test notification";

        Notification notification = notificationAuditService.createNotification(projectId, message);

        assertNotNull(notification);
        assertEquals(projectId, notification.getProjectId());
        assertEquals(message, notification.getMessage());
    }

    @Test
    public void testCreateAuditLog() {
        UUID projectId = UUID.randomUUID();
        String eventType = "CREATED";
        String changedBy = "user123";
        String details = "{\"key\":\"value\"}";

        AuditLog auditLog = notificationAuditService.createAuditLog(projectId, eventType, changedBy, details);

        assertNotNull(auditLog);
        assertEquals(projectId, auditLog.getProjectId());
        assertEquals(eventType, auditLog.getEventType());
        assertEquals(changedBy, auditLog.getChangedBy());
        assertEquals(details, auditLog.getDetails());
    }
}
