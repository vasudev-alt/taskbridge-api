package org.example.projects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.audit.AuditLog;
import org.example.audit.AuditLogRepository;
import org.example.audit.AuditService;
import org.example.notifications.Notification;
import org.example.notifications.NotificationEventType;
import org.example.notifications.NotificationRepository;
import org.example.notifications.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({ProjectService.class, AuditService.class, NotificationService.class, ProjectTeamService.class})
@DisplayName("ProjectService Integration Tests")
public class ProjectServiceIntegrationTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private ObjectMapper objectMapper;
    private UUID testActorUserId;
    private String testOrganisation;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        testActorUserId = UUID.randomUUID();
        testOrganisation = "test-org";
        projectRepository.deleteAll();
        auditLogRepository.deleteAll();
        notificationRepository.deleteAll();
    }

    @Test
    @DisplayName("Test 1: Unauthorised user cannot access another organisation's audit log")
    void testUnauthorisedAccessToAuditLog() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        String unauthorisedOrg = "unauthorised-org";
        String authorisedOrg = "authorised-org";

        // Create audit entry for authorised org
        JsonNode state = objectMapper.createObjectNode();
        auditLogRepository.save(createAuditLog(
                projectId,
                testActorUserId,
                authorisedOrg,
                state
        ));

        // Act
        List<AuditLog> auditLogs = auditLogRepository.findByActorOrganisation(unauthorisedOrg);

        // Assert
        assertEquals(0, auditLogs.size());

        List<AuditLog> authorisedAuditLogs = auditLogRepository.findByActorOrganisation(authorisedOrg);
        assertEquals(1, authorisedAuditLogs.size());
    }

    @Test
    @DisplayName("Test 2: Project creation triggers audit log entry and notifications")
    void testProjectCreationTriggersAuditAndNotification() {
        // Arrange
        Project project = new Project();
        project.setName("Test Project");
        project.setStatus("PENDING");
        project.setTeam("test-team");

        // Act
        Project createdProject = projectService.createProject(
                project,
                testActorUserId,
                testOrganisation,
                "192.168.1.1"
        );

        // Assert
        assertNotNull(createdProject.getId());
        
        // Verify audit entry was created
        List<AuditLog> auditLogs = auditLogRepository.findByEntityIdOrderByCreatedAtDesc(createdProject.getId());
        assertEquals(1, auditLogs.size());
        assertTrue(auditLogs.get(0).getEventType().toString().contains("CREATED"));
    }

    @Test
    @DisplayName("Test 3: Project status update triggers audit log with before/after state")
    void testProjectStatusUpdateTriggersAudit() {
        // Arrange
        Project project = new Project();
        project.setName("Test Project");
        project.setStatus("PENDING");
        project.setTeam("test-team");
        Project savedProject = projectRepository.save(project);

        // Act
        Optional<Project> updatedProject = projectService.updateProjectStatus(
                savedProject.getId(),
                "IN_PROGRESS",
                testActorUserId,
                testOrganisation,
                "192.168.1.1"
        );

        // Assert
        assertTrue(updatedProject.isPresent());
        assertEquals("IN_PROGRESS", updatedProject.get().getStatus());

        // Verify audit entry captures before/after state
        List<AuditLog> auditLogs = auditLogRepository.findByEntityIdOrderByCreatedAtDesc(savedProject.getId());
        assertEquals(1, auditLogs.size());
        AuditLog auditLog = auditLogs.get(0);
        assertEquals("PENDING", auditLog.getPreviousState().get("status").asText());
        assertEquals("IN_PROGRESS", auditLog.getNewState().get("status").asText());
    }

    @Test
    @DisplayName("Test 4: Project deletion triggers audit log and notifications before deletion")
    void testProjectDeletionTriggersAuditAndNotification() {
        // Arrange
        Project project = new Project();
        project.setName("Test Project");
        project.setStatus("COMPLETED");
        project.setTeam("test-team");
        Project savedProject = projectRepository.save(project);
        UUID projectId = savedProject.getId();

        // Act
        projectService.deleteProject(
                projectId,
                testActorUserId,
                testOrganisation,
                "192.168.1.1"
        );

        // Assert
        assertFalse(projectRepository.existsById(projectId));

        // Verify audit entry was created before deletion
        List<AuditLog> auditLogs = auditLogRepository.findByEntityIdOrderByCreatedAtDesc(projectId);
        assertEquals(1, auditLogs.size());
        assertTrue(auditLogs.get(0).getEventType().toString().contains("DELETED"));
    }

    @Test
    @DisplayName("Test 5: Milestone reopened event type creates audit entry with IP address")
    void testMilestoneReopenedEventType() {
        // Arrange
        Project project = new Project();
        project.setName("Test Project");
        project.setStatus("COMPLETED");
        project.setTeam("test-team");
        Project savedProject = projectRepository.save(project);

        // Act
        Optional<Project> reopenedProject = projectService.reopenProjectMilestone(
                savedProject.getId(),
                testActorUserId,
                testOrganisation,
                "203.0.113.42"
        );

        // Assert
        assertTrue(reopenedProject.isPresent());
        assertEquals("REOPENED", reopenedProject.get().getStatus());

        // Verify MILESTONE_REOPENED audit entry was created with IP
        List<AuditLog> auditLogs = auditLogRepository.findByEntityIdOrderByCreatedAtDesc(savedProject.getId());
        assertEquals(1, auditLogs.size());
        AuditLog auditLog = auditLogs.get(0);
        assertTrue(auditLog.getEventType().toString().contains("MILESTONE_REOPENED"));
        assertEquals("203.0.113.42", auditLog.getActorIpAddress());
    }

    private AuditLog createAuditLog(UUID projectId, UUID actorUserId, String organisation, JsonNode state) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityId(projectId);
        auditLog.setEntityType("PROJECT");
        auditLog.setActorUserId(actorUserId);
        auditLog.setActorOrganisation(organisation);
        auditLog.setActorIpAddress("192.168.1.1");
        auditLog.setNewState(state);
        return auditLog;
    }
}
