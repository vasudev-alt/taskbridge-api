package org.example.notifications;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class NotificationAuditControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testCreateNotificationEndpoint() {
        NotificationAuditController.NotificationRequest request = new NotificationAuditController.NotificationRequest();
        request.setProjectId(UUID.randomUUID());
        request.setMessage("Test notification");

        ResponseEntity<Notification> response = restTemplate.postForEntity("/notifications", request, Notification.class);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(request.getProjectId(), response.getBody().getProjectId());
        assertEquals(request.getMessage(), response.getBody().getMessage());
    }

    @Test
    public void testGetAuditLogsEndpoint() {
        UUID projectId = UUID.randomUUID();

        ResponseEntity<AuditLog[]> response = restTemplate.getForEntity("/notifications/audit-logs?projectId=" + projectId, AuditLog[].class);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
    }
}
