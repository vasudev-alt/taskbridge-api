package org.example.notifications;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

@WebMvcTest(NotificationAuditController.class)
@DisplayName("NotificationAuditController Tests")
public class NotificationAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationAuditService notificationAuditService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID testProjectId;
    private Notification testNotification;
    private AuditLog testAuditLog;

    @BeforeEach
    void setUp() {
        testProjectId = UUID.randomUUID();

        testNotification = new Notification();
        testNotification.setId(UUID.randomUUID());
        testNotification.setProjectId(testProjectId);
        testNotification.setMessage("Test notification");
        testNotification.setTimestamp(LocalDateTime.now());

        testAuditLog = new AuditLog();
        testAuditLog.setId(UUID.randomUUID());
        testAuditLog.setProjectId(testProjectId);
        testAuditLog.setEventType("PROJECT_UPDATED");
        testAuditLog.setChangedBy("user@example.com");
        testAuditLog.setTimestamp(LocalDateTime.now());
        testAuditLog.setDetails("{\"status\": \"ACTIVE\"}");
    }

    @Test
    @DisplayName("Test 1: POST /notifications creates notification successfully")
    void testCreateNotificationSuccess() throws Exception {
        // Arrange
        NotificationAuditController.NotificationRequest request = new NotificationAuditController.NotificationRequest();
        request.setProjectId(testProjectId);
        request.setMessage("Test notification");

        when(notificationAuditService.createNotification(eq(testProjectId), anyString()))
                .thenReturn(testNotification);

        // Act & Assert
        mockMvc.perform(post("/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testNotification.getId().toString()))
                .andExpect(jsonPath("$.projectId").value(testProjectId.toString()))
                .andExpect(jsonPath("$.message").value("Test notification"));
    }

    @Test
    @DisplayName("Test 2: POST /notifications returns 400 for invalid request")
    void testCreateNotificationWithInvalidData() throws Exception {
        // Arrange
        String invalidRequest = "{}";

        // Act & Assert
        mockMvc.perform(post("/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Test 3: GET /notifications/audit-logs returns logs for project")
    void testGetAuditLogsSuccess() throws Exception {
        // Arrange
        List<AuditLog> auditLogs = new ArrayList<>();
        auditLogs.add(testAuditLog);

        when(notificationAuditService.getAuditLogs(eq(testProjectId), any(), any(), anyString()))
                .thenReturn(auditLogs);

        // Act & Assert
        mockMvc.perform(get("/notifications/audit-logs")
                .param("projectId", testProjectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testAuditLog.getId().toString()))
                .andExpect(jsonPath("$[0].projectId").value(testProjectId.toString()))
                .andExpect(jsonPath("$[0].eventType").value("PROJECT_UPDATED"));
    }

    @Test
    @DisplayName("Test 4: GET /notifications/audit-logs with startDate filter")
    void testGetAuditLogsWithStartDateFilter() throws Exception {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        List<AuditLog> auditLogs = new ArrayList<>();
        auditLogs.add(testAuditLog);

        when(notificationAuditService.getAuditLogs(eq(testProjectId), any(LocalDateTime.class), any(), anyString()))
                .thenReturn(auditLogs);

        // Act & Assert
        mockMvc.perform(get("/notifications/audit-logs")
                .param("projectId", testProjectId.toString())
                .param("startDate", startDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Test 5: GET /notifications/audit-logs with endDate filter")
    void testGetAuditLogsWithEndDateFilter() throws Exception {
        // Arrange
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);
        List<AuditLog> auditLogs = new ArrayList<>();
        auditLogs.add(testAuditLog);

        when(notificationAuditService.getAuditLogs(eq(testProjectId), any(), any(LocalDateTime.class), anyString()))
                .thenReturn(auditLogs);

        // Act & Assert
        mockMvc.perform(get("/notifications/audit-logs")
                .param("projectId", testProjectId.toString())
                .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("PROJECT_UPDATED"));
    }

    @Test
    @DisplayName("Test 6: GET /notifications/audit-logs with eventType filter")
    void testGetAuditLogsWithEventTypeFilter() throws Exception {
        // Arrange
        String eventType = "PROJECT_UPDATED";
        List<AuditLog> auditLogs = new ArrayList<>();
        auditLogs.add(testAuditLog);

        when(notificationAuditService.getAuditLogs(eq(testProjectId), any(), any(), eq(eventType)))
                .thenReturn(auditLogs);

        // Act & Assert
        mockMvc.perform(get("/notifications/audit-logs")
                .param("projectId", testProjectId.toString())
                .param("eventType", eventType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value(eventType));
    }

    @Test
    @DisplayName("Test 7: GET /notifications/audit-logs with all filters applied")
    void testGetAuditLogsWithAllFilters() throws Exception {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusHours(2);
        LocalDateTime endDate = LocalDateTime.now().plusHours(2);
        String eventType = "PROJECT_UPDATED";
        List<AuditLog> auditLogs = new ArrayList<>();
        auditLogs.add(testAuditLog);

        when(notificationAuditService.getAuditLogs(eq(testProjectId), any(LocalDateTime.class), any(LocalDateTime.class), eq(eventType)))
                .thenReturn(auditLogs);

        // Act & Assert
        mockMvc.perform(get("/notifications/audit-logs")
                .param("projectId", testProjectId.toString())
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("eventType", eventType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value(eventType));
    }

    @Test
    @DisplayName("Test 8: GET /notifications/audit-logs returns empty list when no logs exist")
    void testGetAuditLogsEmptyResult() throws Exception {
        // Arrange
        List<AuditLog> emptyList = new ArrayList<>();
        when(notificationAuditService.getAuditLogs(eq(testProjectId), any(), any(), anyString()))
                .thenReturn(emptyList);

        // Act & Assert
        mockMvc.perform(get("/notifications/audit-logs")
                .param("projectId", testProjectId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Test 9: GET /notifications/audit-logs returns 400 when projectId is missing")
    void testGetAuditLogsMissingProjectId() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/notifications/audit-logs"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Test 10: Notification response contains all required fields")
    void testNotificationResponseStructure() throws Exception {
        // Arrange
        when(notificationAuditService.createNotification(eq(testProjectId), anyString()))
                .thenReturn(testNotification);

        NotificationAuditController.NotificationRequest request = new NotificationAuditController.NotificationRequest();
        request.setProjectId(testProjectId);
        request.setMessage("Test message");

        // Act & Assert
        mockMvc.perform(post("/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.projectId").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
