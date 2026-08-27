package org.example.notifications;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("NotificationService Tests")
public class NotificationServiceTest {

    @Autowired
    private NotificationRepository notificationRepository;

    private UUID testProjectId;
    private LocalDateTime beforeTest;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        testProjectId = UUID.randomUUID();
        beforeTest = LocalDateTime.now();
    }

    @Test
    @DisplayName("Test 1: Notification is created successfully with valid data")
    void testNotificationCreationSuccess() {
        // Arrange
        Notification notification = new Notification();
        notification.setProjectId(testProjectId);
        notification.setMessage("Project milestone updated");
        notification.setTimestamp(LocalDateTime.now());

        // Act
        Notification savedNotification = notificationRepository.save(notification);

        // Assert
        assertNotNull(savedNotification.getId());
        assertEquals(testProjectId, savedNotification.getProjectId());
        assertEquals("Project milestone updated", savedNotification.getMessage());
        assertNotNull(savedNotification.getTimestamp());
    }

    @Test
    @DisplayName("Test 2: Notification can be retrieved by ID from repository")
    void testNotificationRetrievalById() {
        // Arrange
        Notification notification = new Notification();
        notification.setProjectId(testProjectId);
        notification.setMessage("Task assigned to user");
        notification.setTimestamp(LocalDateTime.now());
        Notification savedNotification = notificationRepository.save(notification);
        UUID notificationId = savedNotification.getId();

        // Act
        Optional<Notification> retrievedNotification = notificationRepository.findById(notificationId);

        // Assert
        assertTrue(retrievedNotification.isPresent());
        assertEquals(notificationId, retrievedNotification.get().getId());
        assertEquals("Task assigned to user", retrievedNotification.get().getMessage());
    }

    @Test
    @DisplayName("Test 3: Multiple notifications can be created for the same project")
    void testMultipleNotificationsForSameProject() {
        // Arrange
        Notification notification1 = new Notification();
        notification1.setProjectId(testProjectId);
        notification1.setMessage("First notification");
        notification1.setTimestamp(LocalDateTime.now());

        Notification notification2 = new Notification();
        notification2.setProjectId(testProjectId);
        notification2.setMessage("Second notification");
        notification2.setTimestamp(LocalDateTime.now().plusMinutes(1));

        // Act
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);

        // Assert
        assertEquals(2, notificationRepository.count());
    }

    @Test
    @DisplayName("Test 4: Notification message field handles long text correctly")
    void testNotificationWithLongMessage() {
        // Arrange
        String longMessage = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(5);
        Notification notification = new Notification();
        notification.setProjectId(testProjectId);
        notification.setMessage(longMessage);
        notification.setTimestamp(LocalDateTime.now());

        // Act
        Notification savedNotification = notificationRepository.save(notification);

        // Assert
        assertEquals(longMessage, savedNotification.getMessage());
    }

    @Test
    @DisplayName("Test 5: Notification timestamp is set correctly")
    void testNotificationTimestampAccuracy() {
        // Arrange
        LocalDateTime beforeCreation = LocalDateTime.now();
        Notification notification = new Notification();
        notification.setProjectId(testProjectId);
        notification.setMessage("Timestamp test");
        notification.setTimestamp(beforeCreation);

        // Act
        Notification savedNotification = notificationRepository.save(notification);
        LocalDateTime afterCreation = LocalDateTime.now();

        // Assert
        assertNotNull(savedNotification.getTimestamp());
        assertFalse(savedNotification.getTimestamp().isBefore(beforeCreation));
        assertFalse(savedNotification.getTimestamp().isAfter(afterCreation));
    }

    @Test
    @DisplayName("Test 6: Notification can be updated")
    void testNotificationUpdate() {
        // Arrange
        Notification notification = new Notification();
        notification.setProjectId(testProjectId);
        notification.setMessage("Original message");
        notification.setTimestamp(LocalDateTime.now());
        Notification savedNotification = notificationRepository.save(notification);

        // Act
        savedNotification.setMessage("Updated message");
        Notification updatedNotification = notificationRepository.save(savedNotification);

        // Assert
        assertEquals("Updated message", updatedNotification.getMessage());
    }

    @Test
    @DisplayName("Test 7: Notification can be deleted from repository")
    void testNotificationDeletion() {
        // Arrange
        Notification notification = new Notification();
        notification.setProjectId(testProjectId);
        notification.setMessage("To be deleted");
        notification.setTimestamp(LocalDateTime.now());
        Notification savedNotification = notificationRepository.save(notification);
        UUID notificationId = savedNotification.getId();

        // Act
        notificationRepository.deleteById(notificationId);

        // Assert
        assertFalse(notificationRepository.existsById(notificationId));
    }
}
