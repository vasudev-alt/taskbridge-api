package org.example.notifications;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(NotificationAuditService.class)
@DisplayName("NotificationAuditService Tests")
public class NotificationAuditServiceTest {

    @Autowired
    private NotificationAuditService notificationAuditService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationAuditLogRepository notificationAuditLogRepository;

    private UUID testProjectId;
    private String testEventType;
    private String testChangedBy;
    private String testDetails;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        notificationAuditLogRepository.deleteAll();
        testProjectId = UUID.randomUUID();
        testEventType = "PROJECT_UPDATED";
        testChangedBy = "user@example.com";
        testDetails = "{\"status\": \"ACTIVE\"}";
    }

    @Test
    @DisplayName("Test 1: Notification is created successfully through service")
    void testCreateNotificationSuccess() {
        // Arrange
        String message = "Project status changed to active";

        // Act
        Notification notification = notificationAuditService.createNotification(testProjectId, message);

        // Assert
        assertNotNull(notification.getId());
        assertEquals(testProjectId, notification.getProjectId());
        assertEquals(message, notification.getMessage());
        assertNotNull(notification.getTimestamp());
        assertEquals(1, notificationRepository.count());
    }

    @Test
    @DisplayName("Test 2: Audit log is created successfully through service")
    void testCreateAuditLogSuccess() {
        // Arrange
        // Act
        AuditLog auditLog = notificationAuditService.createAuditLog(
                testProjectId,
                testEventType,
                testChangedBy,
                testDetails
        );

        // Assert
        assertNotNull(auditLog.getId());
        assertEquals(testProjectId, auditLog.getProjectId());
        assertEquals(testEventType, auditLog.getEventType());
        assertEquals(testChangedBy, auditLog.getChangedBy());
        assertEquals(testDetails, auditLog.getDetails());
        assertNotNull(auditLog.getTimestamp());
    }

    @Test
    @DisplayName("Test 3: Audit logs can be retrieved by project ID and ordered by timestamp")
    void testGetAuditLogsByProjectId() {
        // Arrange
        notificationAuditService.createAuditLog(
                testProjectId,
                "PROJECT_CREATED",
                testChangedBy,
                "{\"status\": \"CREATED\"}"
        );

        notificationAuditService.createAuditLog(
                testProjectId,
                "PROJECT_UPDATED",
                testChangedBy,
                "{\"status\": \"UPDATED\"}"
        );

        // Act
        List<AuditLog> auditLogs = notificationAuditLogRepository.findByProjectIdOrderByTimestampDesc(testProjectId);

        // Assert
        assertEquals(2, auditLogs.size());
        // Most recent should be first
        assertEquals("PROJECT_UPDATED", auditLogs.get(0).getEventType());
        assertEquals("PROJECT_CREATED", auditLogs.get(1).getEventType());
    }

    @Test
    @DisplayName("Test 4: Audit logs can be filtered by date range")
    void testGetAuditLogsFilteredByDateRange() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);
        LocalDateTime outsideRange = LocalDateTime.now().minusHours(3);

        AuditLog auditLog1 = new AuditLog();
        auditLog1.setProjectId(testProjectId);
        auditLog1.setEventType("EVENT_1");
        auditLog1.setChangedBy(testChangedBy);
        auditLog1.setTimestamp(LocalDateTime.now());
        auditLog1.setDetails("Inside range");
        notificationAuditLogRepository.save(auditLog1);

        // Act
        List<AuditLog> auditLogs = notificationAuditLogRepository.findByProjectIdOrderByTimestampDesc(testProjectId);
        List<AuditLog> filtered = auditLogs.stream()
                .filter(a -> !a.getTimestamp().isBefore(startDate) && !a.getTimestamp().isAfter(endDate))
                .toList();

        // Assert
        assertEquals(1, filtered.size());
        assertEquals("EVENT_1", filtered.get(0).getEventType());
    }

    @Test
    @DisplayName("Test 5: Audit logs can be filtered by event type")
    void testGetAuditLogsFilteredByEventType() {
        // Arrange
        String eventType1 = "PROJECT_CREATED";
        String eventType2 = "PROJECT_DELETED";

        notificationAuditService.createAuditLog(testProjectId, eventType1, testChangedBy, testDetails);
        notificationAuditService.createAuditLog(testProjectId, eventType2, testChangedBy, testDetails);
        notificationAuditService.createAuditLog(testProjectId, eventType1, testChangedBy, testDetails);

        // Act
        List<AuditLog> auditLogs = notificationAuditService.getAuditLogs(testProjectId, null, null, eventType1);

        // Assert
        assertEquals(2, auditLogs.size());
        assertTrue(auditLogs.stream().allMatch(a -> a.getEventType().equals(eventType1)));
    }

    @Test
    @DisplayName("Test 6: getAuditLogs returns all logs when no filters are applied")
    void testGetAuditLogsNoFilters() {
        // Arrange
        notificationAuditService.createAuditLog(testProjectId, "EVENT_1", testChangedBy, testDetails);
        notificationAuditService.createAuditLog(testProjectId, "EVENT_2", testChangedBy, testDetails);
        notificationAuditService.createAuditLog(testProjectId, "EVENT_3", testChangedBy, testDetails);

        // Act
        List<AuditLog> auditLogs = notificationAuditService.getAuditLogs(testProjectId, null, null, null);

        // Assert
        assertEquals(3, auditLogs.size());
    }

    @Test
    @DisplayName("Test 7: getAuditLogs filters by start date correctly")
    void testGetAuditLogsFilterByStartDate() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);

        AuditLog auditLog = new AuditLog();
        auditLog.setProjectId(testProjectId);
        auditLog.setEventType(testEventType);
        auditLog.setChangedBy(testChangedBy);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setDetails(testDetails);
        notificationAuditLogRepository.save(auditLog);

        // Act
        List<AuditLog> auditLogs = notificationAuditService.getAuditLogs(
                testProjectId,
                startDate,
                null,
                null
        );

        // Assert
        assertEquals(1, auditLogs.size());
    }

    @Test
    @DisplayName("Test 8: getAuditLogs filters by end date correctly")
    void testGetAuditLogsFilterByEndDate() {
        // Arrange
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);

        AuditLog auditLog = new AuditLog();
        auditLog.setProjectId(testProjectId);
        auditLog.setEventType(testEventType);
        auditLog.setChangedBy(testChangedBy);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setDetails(testDetails);
        notificationAuditLogRepository.save(auditLog);

        // Act
        List<AuditLog> auditLogs = notificationAuditService.getAuditLogs(
                testProjectId,
                null,
                endDate,
                null
        );

        // Assert
        assertEquals(1, auditLogs.size());
    }

    @Test
    @DisplayName("Test 9: Multiple notifications and audit logs can coexist for the same project")
    void testMultipleNotificationsAndAuditLogs() {
        // Arrange
        // Act
        Notification notification1 = notificationAuditService.createNotification(testProjectId, "Notification 1");
        Notification notification2 = notificationAuditService.createNotification(testProjectId, "Notification 2");
        AuditLog auditLog1 = notificationAuditService.createAuditLog(testProjectId, "EVENT_1", testChangedBy, testDetails);
        AuditLog auditLog2 = notificationAuditService.createAuditLog(testProjectId, "EVENT_2", testChangedBy, testDetails);

        // Assert
        assertEquals(2, notificationRepository.count());
        assertEquals(2, notificationAuditLogRepository.count());
    }

    @Test
    @DisplayName("Test 10: Audit log details are preserved correctly")
    void testAuditLogDetailsPreservation() {
        // Arrange
        String complexDetails = "{\"oldStatus\": \"PENDING\", \"newStatus\": \"APPROVED\", \"reason\": \"Milestone completed\"}";

        // Act
        AuditLog auditLog = notificationAuditService.createAuditLog(
                testProjectId,
                testEventType,
                testChangedBy,
                complexDetails
        );

        // Assert
        assertEquals(complexDetails, auditLog.getDetails());
    }
}
