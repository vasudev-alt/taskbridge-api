# Notification & Audit Service Specification

## Overview
The Notification & Audit Service will handle real-time notifications for project milestone changes and maintain an immutable audit log for compliance purposes.

## Data Models
### Notification
- **id**: UUID (Primary Key)
- **projectId**: UUID (Foreign Key to Project)
- **message**: String
- **timestamp**: Timestamp

### AuditLog
- **id**: UUID (Primary Key)
- **projectId**: UUID (Foreign Key to Project)
- **eventType**: Enum (e.g., CREATED, UPDATED, CLOSED)
- **changedBy**: String (User ID)
- **timestamp**: Timestamp
- **details**: String (JSON for additional metadata)

## API Contracts
### Notifications
#### POST /notifications
- **Request**:
  ```json
  {
    "projectId": "UUID",
    "message": "String"
  }
  ```
- **Response**:
  ```json
  {
    "id": "UUID",
    "timestamp": "Timestamp"
  }
  ```

### Audit Logs
#### GET /audit-logs
- **Query Parameters**:
  - `projectId`: UUID (required)
  - `startDate`: Date (optional)
  - `endDate`: Date (optional)
  - `eventType`: Enum (optional)
- **Response**:
  ```json
  [
    {
      "id": "UUID",
      "eventType": "Enum",
      "changedBy": "String",
      "timestamp": "Timestamp",
      "details": "String"
    }
  ]
  ```

## Integration Points
- **Project Service**: Consume its API to fetch project details.

## Constraints
- **Immutability**: Audit logs cannot be modified or deleted.
- **Authorization**: Only authorized users can access logs.
- **Validation**: Ensure all inputs are sanitized.

## Copilot Usage
- Copilot will assist in generating boilerplate code for models, repositories, and controllers.
- Manual adjustments will be made to ensure compliance with standards.
