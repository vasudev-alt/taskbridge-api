package org.example.notifications;

import jakarta.persistence.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.type.JsonType;
import org.hibernate.annotations.Type;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private AuditEventType eventType;

    private String entityType; // e.g., "PROJECT"
    private UUID entityId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "actor_organisation")
    private String actorOrganisation;

    @Column(name = "actor_ip_address")
    private String actorIpAddress;

    @Column(columnDefinition = "jsonb")
    @Type(JsonType.class)
    private JsonNode previousState;

    @Column(columnDefinition = "jsonb")
    @Type(JsonType.class)
    private JsonNode newState;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getActorOrganisation() {
        return actorOrganisation;
    }

    public String getActorIpAddress() {
        return actorIpAddress;
    }

    public JsonNode getPreviousState() {
        return previousState;
    }

    public JsonNode getNewState() {
        return newState;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setEventType(AuditEventType eventType) {
        this.eventType = eventType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public void setActorUserId(UUID actorUserId) {
        this.actorUserId = actorUserId;
    }

    public void setActorOrganisation(String actorOrganisation) {
        this.actorOrganisation = actorOrganisation;
    }

    public void setActorIpAddress(String actorIpAddress) {
        this.actorIpAddress = actorIpAddress;
    }

    public void setPreviousState(JsonNode previousState) {
        this.previousState = previousState;
    }

    public void setNewState(JsonNode newState) {
        this.newState = newState;
    }
}
