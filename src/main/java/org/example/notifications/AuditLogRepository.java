package org.example.notifications;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("SELECT a FROM AuditLog a WHERE a.projectId = :projectId AND (:startDate IS NULL OR a.timestamp >= :startDate) AND (:endDate IS NULL OR a.timestamp <= :endDate) AND (:eventType IS NULL OR a.eventType = :eventType)")
    List<AuditLog> findByProjectIdAndFilters(@Param("projectId") UUID projectId,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate,
                                             @Param("eventType") String eventType);
}
