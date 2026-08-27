package org.example.notifications;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(NotificationService.class)
@DisplayName("NotificationService Tests")
public class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    private UUID testUserId;
    private UUID testProjectId;
    private UUID testTeamMemberId1;
    private UUID testTeamMemberId2;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testProjectId = UUID.randomUUID();
        testTeamMemberId1 = UUID.randomUUID();
        testTeamMemberId2 = UUID.randomUUID();
        notificationRepository.deleteAll();
    }

    @Test
    @DisplayName("Test 1: Equal notification dispatch to all team members on a project state change")
    void testEqualNotificationDispatchToTeamMembers() {
        // Arrange & Act
        Notification notif1 = notificationService.createNotification(
                testTeamMemberId1,
                NotificationEventType.PROJECT_STATUS_UPDATED,
                testProjectId,
                "Project status changed to In Progress"
        );

        Notification notif2 = notificationService.createNotification(
                testTeamMemberId2,
                NotificationEventType.PROJECT_STATUS_UPDATED,
                testProjectId,
                "Project status changed to In Progress"
        );

        // Assert - Both team members received the same notification
        assertNotNull(notif1.getId());
        assertNotNull(notif2.getId());
        assertEquals(notif1.getMessage(), notif2.getMessage());
        assertEquals(notif1.getEventType(), notif2.getEventType());
        assertEquals(notif1.getProjectId(), notif2.getProjectId());
        assertFalse(notif1.getReadStatus());
        assertFalse(notif2.getReadStatus());
    }

    @Test
    @DisplayName("Test 2: Unread notifications are correctly retrieved for a user")
    void testGetUnreadNotifications() {
        // Arrange
        notificationService.createNotification(
                testUserId,
                NotificationEventType.PROJECT_CREATED,
                testProjectId,
                "New project created"
        );

        Notification notification2 = notificationService.createNotification(
                testUserId,
                NotificationEventType.PROJECT_STATUS_UPDATED,
                testProjectId,
                "Project status updated"
        );

        // Mark one as read
        notificationService.markAsRead(notification2.getId());

        // Act
        List<Notification> unreadNotifications = notificationService.getUnreadNotifications(testUserId);

        // Assert
        assertEquals(1, unreadNotifications.size());
        assertEquals(NotificationEventType.PROJECT_CREATED, unreadNotifications.get(0).getEventType());
    }

    @Test
    @DisplayName("Test 3: Notification status can be marked as read")
    void testMarkNotificationAsRead() {
        // Arrange
        Notification notification = notificationService.createNotification(
                testUserId,
                NotificationEventType.PROJECT_CREATED,
                testProjectId,
                "New project"
        );
        assertFalse(notification.getReadStatus());
        assertNull(notification.getReadAt());

        // Act
        Optional<Notification> updatedNotification = notificationService.markAsRead(notification.getId());

        // Assert
        assertTrue(updatedNotification.isPresent());
        assertTrue(updatedNotification.get().getReadStatus());
        assertNotNull(updatedNotification.get().getReadAt());
    }

    @Test
    @DisplayName("Test 4: Notifications for non-existent ID return empty Optional")
    void testMarkNonExistentNotificationAsRead() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        Optional<Notification> result = notificationService.markAsRead(nonExistentId);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test 5: All notifications (read and unread) are retrievable for a user")
    void testGetAllNotifications() {
        // Arrange
        Notification notif1 = notificationService.createNotification(
                testUserId,
                NotificationEventType.PROJECT_CREATED,
                testProjectId,
                "Project created"
        );

        Notification notif2 = notificationService.createNotification(
                testUserId,
                NotificationEventType.PROJECT_STATUS_UPDATED,
                testProjectId,
                "Project updated"
        );

        notificationService.markAsRead(notif1.getId());

        // Act
        List<Notification> allNotifications = notificationService.getAllNotifications(testUserId);

        // Assert
        assertEquals(2, allNotifications.size());
        assertTrue(allNotifications.stream().anyMatch(Notification::getReadStatus));
        assertTrue(allNotifications.stream().anyMatch(n -> !n.getReadStatus()));
    }

    @Test
    @DisplayName("Test 6: Notifications are ordered by creation date (newest first)")
    void testNotificationsOrderedByCreationDate() {
        // Arrange
        Notification notif1 = notificationService.createNotification(
                testUserId,
                NotificationEventType.PROJECT_CREATED,
                testProjectId,
                "First notification"
        );

        Notification notif2 = notificationService.createNotification(
                testUserId,
                NotificationEventType.PROJECT_STATUS_UPDATED,
                testProjectId,
                "Second notification"
        );

        Notification notif3 = notificationService.createNotification(
                testUserId,
                NotificationEventType.PROJECT_DELETED,
                testProjectId,
                "Third notification"
        );

        // Act
        List<Notification> allNotifications = notificationService.getAllNotifications(testUserId);

        // Assert
        assertEquals(3, allNotifications.size());
        assertEquals(notif3.getId(), allNotifications.get(0).getId());
        assertEquals(notif2.getId(), allNotifications.get(1).getId());
        assertEquals(notif1.getId(), allNotifications.get(2).getId());
    }
}
