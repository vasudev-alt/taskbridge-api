package org.example.notifications;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * NotificationService manages notification records for project events.
 * Handles creation, retrieval, and status updates of notifications.
 */
@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    /**
 * Creates and sends a notification to a user.
 * @param recipientUserId the user receiving the notification
 * @param eventType the type of event
 * @param projectId the project ID
 * @param message the notification message
 * @return the created Notification
 */
    public Notification createNotification(
            UUID recipientUserId,
            NotificationEventType eventType,
            UUID projectId,
            String message
    ) {
        Notification notification = new Notification();
        notification.setRecipientUserId(recipientUserId);
        notification.setEventType(eventType);
        notification.setProjectId(projectId);
        notification.setMessage(message);
        notification.setReadStatus(false);

        return notificationRepository.save(notification);
    }

    /**
 * Retrieves all unread notifications for a user.
 * @param userId the user ID
 * @return list of unread notifications ordered by creation date (newest first)
 */
    public List<Notification> getUnreadNotifications(UUID userId) {
        return notificationRepository.findUnreadNotifications(userId);
    }

    /**
 * Retrieves all notifications for a user (read and unread).
 * @param userId the user ID
 * @return list of all notifications ordered by creation date (newest first)
 */
    public List<Notification> getAllNotifications(UUID userId) {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId);
    }

    /**
 * Marks a notification as read.
 * @param notificationId the notification ID
 * @return the updated Notification, or Optional.empty() if not found
 */
    public Optional<Notification> markAsRead(UUID notificationId) {
        Optional<Notification> notification = notificationRepository.findById(notificationId);
        notification.ifPresent(n -> {
            n.setReadStatus(true);
            n.setReadAt(LocalDateTime.now());
            notificationRepository.save(n);
        });
        return notification;
    }

    /**
 * Retrieves notifications for a specific project.
 * @param projectId the project ID
 * @return list of notifications for the project
 */
    public List<Notification> getNotificationsByProjectId(UUID projectId) {
        return notificationRepository.findByProjectId(projectId);
    }

    /**
 * Retrieves a specific notification by ID.
 * @param notificationId the notification ID
 * @return the Notification, or Optional.empty() if not found
 */
    public Optional<Notification> getNotificationById(UUID notificationId) {
        return notificationRepository.findById(notificationId);
    }
}
