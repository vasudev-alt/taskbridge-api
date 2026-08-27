package org.example.notifications;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
@Import(AuditService.class)
@DisplayName("AuditService Tests")
public class AuditServiceTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private ObjectMapper objectMapper;
    private UUID testProjectId;
    private UUID testActorUserId;
    private String testOrganisation;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        testProjectId = UUID.randomUUID();
        testActorUserId = UUID.randomUUID();
        testOrganisation = "test-org";
        auditLogRepository.deleteAll();
    }

    @Test
    @DisplayName("Test 1: Audit entry is created correctly when a project milestone is updated")
    void testAuditEntryCreatedOnProjectUpdate() {
        // Arrange
        JsonNode previousState = objectMapper.createObjectNode()
                .put("status", "PENDING");
        JsonNode newState = objectMapper.createObjectNode()
                .put("status", "IN_PROGRESS");

        // Act
        AuditLogEntry auditLog = auditService.recordAuditEvent(
                AuditEventType.PROJECT_STATUS_UPDATED,
                "PROJECT",
                testProjectId,
                testActorUserId,
                testOrganisation,
                "192.168.1.1",
                previousState,
                newState
        );

        // Assert
        assertNotNull(auditLog.getId());
        assertEquals(AuditEventType.PROJECT_STATUS_UPDATED, auditLog.getEventType());
        assertEquals("PROJECT", auditLog.getEntityType());
        assertEquals(testProjectId, auditLog.getEntityId());
        assertEquals(testActorUserId, auditLog.getActorUserId());
        assertEquals(testOrganisation, auditLog.getActorOrganisation());
        assertEquals("192.168.1.1", auditLog.getActorIpAddress());
        assertEquals(previousState, auditLog.getPreviousState());
        assertEquals(newState, auditLog.getNewState());
        assertNotNull(auditLog.getCreatedAt());
    }

    @Test
    @DisplayName("Test 2: Audit entry cannot be deleted or overwritten (immutability enforcement)")
    void testAuditEntryImmutability() {
        // Arrange
        JsonNode newState = objectMapper.createObjectNode()
                .put("status", "CREATED");
        AuditLogEntry auditLog = auditService.recordAuditEvent(
                AuditEventType.PROJECT_CREATED,
                "PROJECT",
                testProjectId,
                testActorUserId,
                testOrganisation,
                "192.168.1.1",
                null,
                newState
        );
        UUID auditLogId = auditLog.getId();

        // Act & Assert - Verify delete throws exception
        assertThrows(UnsupportedOperationException.class, () -> {
            auditService.deleteAuditLog(auditLogId);
        });

        // Act & Assert - Verify update throws exception
        assertThrows(UnsupportedOperationException.class, () -> {
            auditService.updateAuditLog(auditLogId, new AuditLogEntry());
        });

        // Verify audit log still exists in database
        assertTrue(auditLogRepository.existsById(auditLogId));
    }

    @Test
    @DisplayName("Test 3: Audit history query returns correct results filtered by date range")
    void testAuditHistoryQueryByDateRange() {
        // Arrange
        JsonNode state = objectMapper.createObjectNode().put("id", testProjectId.toString());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earlier = now.minusHours(2);
        LocalDateTime later = now.plusHours(2);

        // Create audit entries
        auditService.recordAuditEvent(
                AuditEventType.PROJECT_CREATED,
                "PROJECT",
                testProjectId,
                testActorUserId,
                testOrganisation,
                "192.168.1.1",
                null,
                state
        );

        // Act
        List<AuditLogEntry> auditHistory = auditService.getAuditHistoryByDateRange(
                testProjectId,
                earlier,
                later
        );

        // Assert
        assertEquals(1, auditHistory.size());
        assertEquals(AuditEventType.PROJECT_CREATED, auditHistory.get(0).getEventType());
    }

    @Test
    @DisplayName("Test 4: Audit history query filtered by event type returns only matching entries")
    void testAuditHistoryQueryByEventType() {
        // Arrange
        JsonNode state = objectMapper.createObjectNode();

        // Create multiple audit entries with different event types
        auditService.recordAuditEvent(
                AuditEventType.PROJECT_CREATED,
                "PROJECT",
                testProjectId,
                testActorUserId,
                testOrganisation,
                "192.168.1.1",
                null,
                state
        );

        auditService.recordAuditEvent(
                AuditEventType.PROJECT_STATUS_UPDATED,
                "PROJECT",
                testProjectId,
                testActorUserId,
                testOrganisation,
                "192.168.1.1",
                state,
                state
        );

        auditService.recordAuditEvent(
                AuditEventType.PROJECT_CREATED,
                "PROJECT",
                testProjectId,
                testActorUserId,
                testOrganisation,
                "192.168.1.1",
                null,
                state
        );

        // Act
        List<AuditLogEntry> statusUpdates = auditService.getAuditHistoryByEventType(
                testProjectId,
                AuditEventType.PROJECT_STATUS_UPDATED
        );

        // Assert
        assertEquals(1, statusUpdates.size());
        assertEquals(AuditEventType.PROJECT_STATUS_UPDATED, statusUpdates.get(0).getEventType());
    }

    @Test
    @DisplayName("Test 5: Audit entries capture IP address for compliance and security tracking")
    void testAuditEntryCapuresIpAddress() {
        // Arrange
        String testIpAddress = "203.0.113.42";
        JsonNode state = objectMapper.createObjectNode();

        // Act
        AuditLogEntry auditLog = auditService.recordAuditEvent(
                AuditEventType.MILESTONE_REOPENED,
                "PROJECT",
                testProjectId,
                testActorUserId,
                testOrganisation,
                testIpAddress,
                state,
                state
        );

        // Assert
        assertEquals(testIpAddress, auditLog.getActorIpAddress());
    }

    @Test
    @DisplayName("Test 6: Multiple audit entries for same project are retrievable in chronological order")
    void testMultipleAuditEntriesOrderedByDate() {
        // Arrange
        JsonNode state = objectMapper.createObjectNode();

        // Create multiple audit entries
        auditService.recordAuditEvent(
                AuditEventType.PROJECT_CREATED,
                "PROJECT",
                testProjectId,
                testActorUserId,
                testOrganisation,
                "192.168.1.1",
                null,
                state
        );

        auditService.recordAuditEvent(
                AuditEventType.PROJECT_STATUS_UPDATED,
                "PROJECT",
                testProjectId,
                testActorUserId,
                testOrganisation,
                "192.168.1.2",
                state,
                state
        );

        auditService.recordAuditEvent(
                AuditEventType.MILESTONE_REOPENED,
                "PROJECT",
                testProjectId,
                testActorUserId,
                testOrganisation,
                "192.168.1.3",
                state,
                state
        );

        // Act
        List<AuditLogEntry> auditHistory = auditService.getAuditHistoryByProjectId(testProjectId);

        // Assert
        assertEquals(3, auditHistory.size());
        // Verify newest entries first
        assertEquals(AuditEventType.MILESTONE_REOPENED, auditHistory.get(0).getEventType());
        assertEquals(AuditEventType.PROJECT_STATUS_UPDATED, auditHistory.get(1).getEventType());
        assertEquals(AuditEventType.PROJECT_CREATED, auditHistory.get(2).getEventType());
    }
}
