package org.example.notifications;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * GET /notifications/:userId - Get all unread notifications for a user.
     *
     * @param userId the user ID
     * @return list of unread notifications
     */
    @GetMapping("/{userId}")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable UUID userId) {
        List<Notification> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * GET /notifications/:userId/all - Get all notifications for a user (read and unread).
     *
     * @param userId the user ID
     * @return list of all notifications
     */
    @GetMapping("/{userId}/all")
    public ResponseEntity<List<Notification>> getAllNotifications(@PathVariable UUID userId) {
        List<Notification> notifications = notificationService.getAllNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * PATCH /notifications/:id/read - Mark a notification as read.
     *
     * @param id the notification ID
     * @return the updated Notification
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable UUID id) {
        Optional<Notification> notification = notificationService.markAsRead(id);
        return notification.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * GET /notifications/:id - Get a specific notification by ID.
     *
     * @param id the notification ID
     * @return the Notification
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<Notification> getNotificationById(@PathVariable UUID id) {
        Optional<Notification> notification = notificationService.getNotificationById(id);
        return notification.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
