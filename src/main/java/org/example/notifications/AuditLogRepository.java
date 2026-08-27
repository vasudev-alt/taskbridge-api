package org.example.notifications;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntry, UUID> {

    List<AuditLogEntry> findByEntityIdOrderByCreatedAtDesc(UUID entityId);

    @Query("SELECT a FROM AuditLogEntry a WHERE a.entityId = :entityId AND a.createdAt BETWEEN :fromDate AND :toDate ORDER BY a.createdAt DESC")
    List<AuditLogEntry> findByEntityIdAndDateRange(
            @Param("entityId") UUID entityId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query("SELECT a FROM AuditLogEntry a WHERE a.entityId = :entityId AND a.eventType = :eventType ORDER BY a.createdAt DESC")
    List<AuditLogEntry> findByEntityIdAndEventType(
            @Param("entityId") UUID entityId,
            @Param("eventType") AuditEventType eventType
    );

    @Query("SELECT a FROM AuditLogEntry a WHERE a.entityId = :entityId AND a.eventType = :eventType AND a.createdAt BETWEEN :fromDate AND :toDate ORDER BY a.createdAt DESC")
    List<AuditLogEntry> findByEntityIdEventTypeAndDateRange(
            @Param("entityId") UUID entityId,
            @Param("eventType") AuditEventType eventType,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    List<AuditLogEntry> findByActorOrganisation(String organisation);

    List<AuditLogEntry> findByActorUserId(UUID actorUserId);
}
