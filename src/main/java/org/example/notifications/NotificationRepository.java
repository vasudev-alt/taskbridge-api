package org.example.notifications;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId);

    List<Notification> findByRecipientUserIdAndReadStatusOrderByCreatedAtDesc(UUID recipientUserId, Boolean readStatus);

    @Query("SELECT n FROM Notification n WHERE n.recipientUserId = :recipientUserId AND n.readStatus = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadNotifications(@Param("recipientUserId") UUID recipientUserId);

    List<Notification> findByProjectId(UUID projectId);

    List<Notification> findByRecipientUserIdAndProjectId(UUID recipientUserId, UUID projectId);
}
