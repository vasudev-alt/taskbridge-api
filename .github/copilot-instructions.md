# GitHub Copilot Instructions

## Technology Stack
- **Backend**: Java with Spring Boot
- **Database**: PostgreSQL
- **Build Tool**: Gradle (Kotlin DSL)
- **Testing**: JUnit 5

## Architecture Conventions
- Multi-service architecture with clear separation of concerns.
- Each service should have its own data models, repositories, and controllers.
- Services communicate via REST APIs.

## Coding Standards
- Follow Java conventions (e.g., camelCase for variables, PascalCase for classes).
- Use dependency injection for service classes.
- Ensure immutability where applicable.
- Write clean, readable, and maintainable code.

## Security Rules
- Implement authentication and authorization for all endpoints.
- Validate all user inputs to prevent SQL injection and other attacks.
- Use environment variables for sensitive data (e.g., database credentials).

## Testing Expectations
- Write unit tests for all service methods.
- Write integration tests for API endpoints.
- Ensure 80% or higher code coverage.
- Mock external dependencies in tests.

## Copilot Usage
- Use Copilot to generate boilerplate code.
- Review and refine Copilot-generated code to ensure compliance with standards.
- Document where Copilot was used and any manual corrections made.
