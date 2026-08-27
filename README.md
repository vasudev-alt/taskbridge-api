# TaskBridge: Notification & Audit Service

## Overview
This project is part of the TaskBridge platform, designed to handle real-time notifications for project milestone changes and maintain an immutable audit log for compliance purposes.

## Technology Stack
- **Backend**: Java with Spring Boot
- **Database**: PostgreSQL
- **Build Tool**: Gradle (Kotlin DSL)
- **Testing**: JUnit 5

## Features
1. **Notifications**:
   - Emit notifications for project milestone changes.
   - API to create and retrieve notifications.

2. **Audit Logs**:
   - Maintain an immutable log of project state changes.
   - API to query logs by project, date range, and event type.

## How to Run
1. Ensure PostgreSQL is running and update `application.properties` with your database credentials.
2. Build the project:
   ```bash
   ./gradlew build
   ```
3. Run the application:
   ```bash
   ./gradlew bootRun
   ```

## Testing
Run the tests using:
```bash
./gradlew test
```
