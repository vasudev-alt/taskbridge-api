package org.example.notifications;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public class AuditEventRequest {
    private AuditEventType eventType;
    private String entityType;
    private UUID entityId;
    private UUID actorUserId;
    private String actorOrganisation;
    private JsonNode previousState;
    private JsonNode newState;

    // Getters and Setters
    public AuditEventType getEventType() {
        return eventType;
    }

    public void setEventType(AuditEventType eventType) {
        this.eventType = eventType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(UUID actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorOrganisation() {
        return actorOrganisation;
    }

    public void setActorOrganisation(String actorOrganisation) {
        this.actorOrganisation = actorOrganisation;
    }

    public JsonNode getPreviousState() {
        return previousState;
    }

    public void setPreviousState(JsonNode previousState) {
        this.previousState = previousState;
    }

    public JsonNode getNewState() {
        return newState;
    }

    public void setNewState(JsonNode newState) {
        this.newState = newState;
    }
}
