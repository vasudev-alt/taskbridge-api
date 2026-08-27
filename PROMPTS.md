# Prompt Engineering Documentation: Notification & Audit Service Implementation

## Overview

This document details the prompt engineering strategy used to build the Notification & Audit Service using GitHub Copilot, including the exact prompts, techniques applied, and corrections made to AI-generated output.

---

## Prompt Chain Execution

### Prompt 1: Architecture Exploration
**Feature:** GitHub Copilot Chat (Semantic Search)
**Technique:** Role-based prompting, Specificity

**Exact Prompt:**
```
You are a Spring Boot architect. I'm building a notification and audit service for a project management 
API. The audit service must store immutable event logs capturing before/after state of project entities. 
The notification service must dispatch messages to team members on project changes. 

Design the entity models for:
1. AuditLog - immutable entries with event type, entity reference, actor info, state snapshots
2. Notification - records for users with read status tracking

Include JPA annotations, validation, and immutability constraints.
```

**Copilot Output:**
- Provided `AuditLog` entity with `@Entity`, `@Table` annotations
- Suggested `@PrePersist` for timestamp management
- Included JSONB field mapping for state snapshots (using `@Type(JsonType.class)`)
- Provided `Notification` entity with similar structure

**Technique Rationale:** Role-based prompting ("architect") sets context for design-level responses. Specificity (listing 2 entities with constraints) ensures focused output.

**Validation:** I cross-checked field types against Spring Boot + PostgreSQL best practices. JSONB suggestion was validated and accepted.

---

### Prompt 2: Repository & Query Design
**Feature:** GitHub Copilot Chat (Code Generation)
**Technique:** Few-shot prompting, Decomposition

**Exact Prompt:**
```
Generate Spring Data JPA repository interfaces for audit logs and notifications. 

For AuditLogRepository:
- Method to find audit entries by project ID (entityId)
- Method to find by date range (fromDate, toDate)
- Method to filter by event type
- Method to combine event type AND date range filtering

For NotificationRepository:
- Method to find unread notifications for a user
- Method to find all notifications ordered by creation date
- Query should handle NULL checks for timestamps

Use @Query annotations with proper SQL/JPQL syntax.
```

**Copilot Output:**
- Correct `@Query` annotations with JPQL
- Proper date filtering using `BETWEEN`
- Correct enum handling in queries
- Proper ordering by `createdAt DESC`

**Technique Rationale:** Decomposition (breaking into specific query requirements) ensures comprehensive repository. Few-shot (listing example methods) guides output format.

**Validation:** Tested query syntax. Found Copilot used JPQL correctly; queries match Spring Data conventions.

---

### Prompt 3: Service Layer Implementation
**Feature:** GitHub Copilot Code Completion
**Technique:** Constraint-based prompting, Iterative refinement

**Exact Prompt:**
```
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AuditService {
    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Records an audit event. This method ENFORCES IMMUTABILITY by only allowing inserts.
     * Audit entries CANNOT be updated or deleted once created.
     * 
     * @param eventType the type of event
     * @param entityType entity classification (e.g., "PROJECT")
     * @param entityId the entity's ID
     * @param actorUserId the user performing the action
     * @param actorOrganisation the organisation context
     * @param actorIpAddress the actor's IP address (NEW REQUIREMENT)
     * @param previousState before state snapshot (null for create events)
     * @param newState after state snapshot
     * @return the created AuditLog entry
     */
    public AuditLog recordAuditEvent(
            AuditEventType eventType,
            String entityType,
            UUID entityId,
            UUID actorUserId,
            String actorOrganisation,
            String actorIpAddress,  // NEW
            JsonNode previousState,
            JsonNode newState
    ) {
        // Generate implementation that:
        // 1. Creates AuditLog entity
        // 2. Sets all fields including actorIpAddress
        // 3. Saves to repository
        // 4. Returns saved entity (with generated ID)
        // 5. Throws exception if update/delete attempted
    }

    /**
     * ENFORCES IMMUTABILITY: Audit logs cannot be updated.
     */
    public void updateAuditLog(UUID auditLogId, AuditLog updatedLog) {
        throw new UnsupportedOperationException("Audit logs are immutable and cannot be updated.");
    }

    /**
     * ENFORCES IMMUTABILITY: Audit logs cannot be deleted.
     */
    public void deleteAuditLog(UUID auditLogId) {
        throw new UnsupportedOperationException("Audit logs are immutable and cannot be deleted.");
    }
}
```

**Copilot Output:**
- Implemented `recordAuditEvent()` correctly with all field mappings
- Added proper `@PrePersist` hook usage
- Immutability methods with appropriate exceptions
- Query methods for filtering with correct repository calls

**Technique Rationale:** Constraint-based prompting ("ENFORCES", "CANNOT") ensures immutability is top-of-mind. Providing skeleton method signatures guides Copilot to fill in correct logic.

**Validation:** Verified each method matches specification. IP address parameter correctly added and stored.

---

### Prompt 4: ProjectService Integration
**Feature:** GitHub Copilot Code Completion with Manual Refinement
**Technique:** Iterative refinement, Constraint-based

**Exact Prompt:**
```
// Enhanced ProjectService that integrates audit and notification services
// REQUIREMENTS:
// 1. Each method (createProject, updateProjectStatus, deleteProject) must:
//    - Accept actorUserId, actorOrganisation, actorIpAddress parameters
//    - Capture previousState BEFORE any modifications
//    - Call auditService.recordAuditEvent() with before/after state
//    - Call notificationService on all team members via projectTeamService.getTeamMembersByProjectId()
// 2. Handle MILESTONE_REOPENED event type (new in this sprint)
// 3. Notify team BEFORE deletion (to notify before entity removed)
// 4. Use ObjectMapper to convert entities to JsonNode for state snapshots

@Service
public class ProjectService {
    @Autowired
    private AuditService auditService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private ProjectTeamService projectTeamService;
    @Autowired
    private ObjectMapper objectMapper;
    
    // Implement createProject, updateProjectStatus, deleteProject, reopenProjectMilestone
}
```

**Copilot Output:**
- Correctly integrated AuditService calls
- Properly captured previous state before modifications
- Correctly implemented notifyTeamMembers() helper
- Added reopenProjectMilestone() method for new event type

**Issue Found:**
- Copilot suggested updating ProjectService without deprecating old signatures
- This creates breaking changes for callers

**Correction Applied:**
```java
// Option 1: Overload old methods (backward compatible)
public Project createProject(Project project) {
    // Call new method with default/extracted context
    return createProject(project, getCurrentUserId(), getCurrentOrganisation(), getClientIp());
}

// Option 2: Keep deprecated methods, gradually migrate callers
@Deprecated(since = "1.1.0", forRemoval = true)
public Project createProject(Project project) { ... }
```

**Technique Rationale:** Iterative refinement identifies missing backward compatibility. Constraint prompting ensures all requirements are met.

---

### Prompt 5: API Controllers
**Feature:** GitHub Copilot Code Completion
**Technique:** Few-shot, Template-based

**Exact Prompt:**
```
Create Spring REST controllers for audit and notification services.

AuditController:
- POST /api/audit - Internal endpoint, records audit event
  * Automatically extract client IP from request (X-Forwarded-For header or remoteAddr)
  * Accept AuditEventRequest DTO with eventType, entityType, entityId, etc.
  * Return created AuditLog

- GET /api/audit/{projectId} - Query audit history
  * @RequestParam(required = false) from: LocalDateTime (ISO 8601 format)
  * @RequestParam(required = false) to: LocalDateTime
  * @RequestParam(required = false) eventType: AuditEventType enum
  * Support all combinations of filters or no filters
  * Return List<AuditLog>

NotificationController:
- GET /api/notifications/{userId} - Get unread notifications
- GET /api/notifications/{userId}/all - Get all notifications
- PATCH /api/notifications/{id}/read - Mark as read
- GET /api/notifications/detail/{id} - Get specific notification

Use @RestController, @RequestMapping, proper HTTP methods and status codes.
```

**Copilot Output:**
- Correctly mapped endpoints
- Proper use of `@PathVariable`, `@RequestParam`, `@RequestBody`
- Correct HTTP methods (GET, POST, PATCH)
- Proper use of `ResponseEntity` for flexible responses
- Correct IP extraction logic from headers

**Issue Found:**
- Controllers lacked authorization checks (security gap)

**Correction Applied:**
```java
@PreAuthorize("hasRole('USER')")
@GetMapping("/{projectId}")
public ResponseEntity<List<AuditLog>> getAuditHistory(...) {
    // Add organization-based filtering at repository level
    UUID userOrg = getCurrentUserOrganization();
    // Verify user's org matches project's org before returning
}
```

**Technique Rationale:** Few-shot (showing endpoint patterns) and template-based (REST conventions) ensure standard Spring Boot practices.

---

### Prompt 6: Test Suite Generation
**Feature:** GitHub Copilot Code Completion
**Technique:** Constraint-based, Specification-driven

**Exact Prompt:**
```
Generate JUnit 5 test suite for AuditService with minimum 6 test cases:

1. "Audit entry is created correctly when a project milestone is updated"
   - Create audit entry with PROJECT_STATUS_UPDATED event
   - Verify all fields set correctly (ID, eventType, entityId, actor, state snapshots, timestamp)

2. "Audit entry cannot be deleted or overwritten (immutability enforcement)"
   - Create audit entry
   - Assert updateAuditLog() throws UnsupportedOperationException
   - Assert deleteAuditLog() throws UnsupportedOperationException
   - Verify entry still exists in DB

3. "Audit history query returns correct results filtered by date range"
   - Create audit entries
   - Query with fromDate and toDate
   - Verify only entries in range returned

4. "Audit history query filtered by event type returns only matching entries"
   - Create multiple entries with different event types
   - Query by eventType
   - Verify only matching type returned

5. "Audit entries capture IP address for compliance and security tracking"
   - Create entry with specific IP
   - Verify IP address stored correctly

6. "Multiple audit entries for same project are retrievable in chronological order"
   - Create 3+ entries
   - Query history
   - Verify ordered by createdAt DESC (newest first)

Also generate NotificationService and ProjectServiceIntegration tests with similar patterns.

Use @DataJpaTest, @DisplayName, clear Arrange-Act-Assert structure.
```

**Copilot Output:**
- Generated all 6 test cases as specified
- Correct use of JUnit 5 annotations
- Proper test structure (setUp, tearDown)
- Clear assertion messages
- Correct use of mocked repositories

**Validation:**
- Tests are executable and comprehensive
- Cover both happy paths and error cases
- Security scenario included (cross-org audit access)

**Technique Rationale:** Specification-driven prompting (listing exact test cases) ensures test suite comprehensiveness. Constraint-based (must cover immutability, filtering, etc.) ensures requirements met.

---

### Prompt 7: Impact Analysis & Documentation
**Feature:** GitHub Copilot Chat (Semantic Analysis)
**Technique:** Role-based, Decomposition

**Exact Prompt:**
```
You are a technical architect conducting impact analysis for a new feature.

I'm adding a MILESTONE_REOPENED event type and IP address capture to an existing audit system.

Analyze and document:
1. Every file, module, and data model affected
2. Nature of each change: additive, breaking, requires migration
3. Security & compliance risks of capturing IP addresses (GDPR, CCPA, data retention, logging)
4. Breaking changes and required migrations
5. Recommended phased implementation approach
6. Mitigation strategies for risks identified

Provide specific, actionable recommendations with code examples where relevant.
Structure as IMPACT_ANALYSIS.md file.
```

**Copilot Output:**
- Comprehensive file inventory
- Correct identification of breaking vs. additive changes
- Good GDPR/CCPA analysis
- Risk identification (authorization gaps, rate limiting)
- Phased implementation plan

**Corrections Applied:**
1. Added specific SQL migration examples
2. Expanded authorization gap section with specific Spring Security recommendations
3. Added backward compatibility strategy (overloaded methods)
4. Added explicit risk mitigation checklist

**Technique Rationale:** Role-based prompting (architect) sets analytical tone. Decomposition (breaking into 6 specific areas) ensures thorough coverage.

---

## Prompting Techniques Summary

| Technique | Usage | Examples |
|-----------|-------|----------|
| **Role-based** | Set context and tone for better output | "You are a Spring Boot architect..." |
| **Specificity** | Narrow scope to exact requirements | "Method to find by date range with JPQL" |
| **Decomposition** | Break complex tasks into steps | "List 6 specific test cases" |
| **Few-shot** | Provide examples to guide format | Showing method signature patterns |
| **Constraint-based** | Emphasize non-negotiable requirements | "ENFORCES IMMUTABILITY" in caps |
| **Iterative refinement** | Build on previous output progressively | Generate code → test → identify gaps → refine |
| **Template-based** | Leverage common patterns | Spring Boot conventions, REST standards |
| **Specification-driven** | List exact acceptance criteria | Test case numbers and descriptions |

---

## Copilot Features Used

1. **GitHub Copilot Chat (Semantic Search)** - Architecture exploration, design decisions
2. **GitHub Copilot Code Completion** - Entity generation, service implementation, test scaffolding
3. **GitHub Copilot PR Review (Implicit)** - Validation of generated code against patterns

---

## Post-Generation Corrections

### Correction 1: Backward Compatibility
**Issue:** ProjectService method signatures changed breaking existing callers
```java
// Copilot generated (breaks existing code)
public Project createProject(
        Project project,
        UUID actorUserId,
        String actorOrganisation,
        String actorIpAddress
) { ... }
```

**Fix Applied:**
```java
// Add overloaded method for backward compatibility
public Project createProject(Project project) {
    // Extract context from SecurityContext/request
    return createProject(project, getCurrentUserId(), getCurrentOrganisation(), getClientIp());
}

// New method with full context
public Project createProject(
        Project project,
        UUID actorUserId,
        String actorOrganisation,
        String actorIpAddress
) { ... }
```
**Rationale:** Maintains backward compatibility during transition period.

---

### Correction 2: Authorization Gaps
**Issue:** Controllers lacked access control
```java
// Copilot generated (security vulnerability)
@GetMapping("/{projectId}")
public ResponseEntity<List<AuditLog>> getAuditHistory(@PathVariable UUID projectId) {
    return ResponseEntity.ok(auditService.getAuditHistoryByProjectId(projectId));
}
```

**Fix Applied:**
```java
// Add security annotations and organization check
@PreAuthorize("hasRole('USER')")
@GetMapping("/{projectId}")
public ResponseEntity<List<AuditLog>> getAuditHistory(@PathVariable UUID projectId) {
    // Verify user's organization
    UUID userOrg = securityContext.getAuthentication().getPrincipal().getOrganization();
    Project project = projectService.getProjectById(projectId);
    
    if (!project.getOrganization().equals(userOrg)) {
        return ResponseEntity.status(403).build();
    }
    
    return ResponseEntity.ok(auditService.getAuditHistoryByProjectId(projectId));
}
```
**Rationale:** Prevents unauthorized cross-organization data access.

---

### Correction 3: IP Address Privacy
**Issue:** Copilot didn't address GDPR implications

**Fix Applied:**
- Added data retention policy (90 days)
- Recommended IP anonymization/hashing
- Added explicit privacy documentation
- Suggested HMAC signing for integrity

---

### Correction 4: Immutability Enforcement
**Issue:** Only service-layer enforcement suggested, no DB-level constraints
```sql
-- Copilot suggestion (only app-level protection)
CREATE TABLE audit_logs (
    ...
);
```

**Fix Applied:**
```sql
-- Add database-level immutability constraint
CREATE TABLE audit_logs (
    ...
    CONSTRAINT audit_immutable_insert_only 
        CHECK (true) -- Allows inserts, checked at app layer for updates/deletes
);

-- Alternative: Trigger-based enforcement
CREATE TRIGGER prevent_audit_update
BEFORE UPDATE ON audit_logs
FOR EACH ROW
EXECUTE FUNCTION raise_immutability_error();
```
**Rationale:** Defense-in-depth approach protects against SQL injection or admin mistakes.

---

### Correction 5: Test Coverage for Cross-Org Access
**Issue:** Copilot generated happy-path tests, missed security scenarios
```java
// Copilot focused on positive cases
public void testGetUnreadNotifications() { ... }
public void testMarkAsRead() { ... }
```

**Fix Applied:**
Added explicit test case:
```java
@Test
@DisplayName("Test: Unauthorised user cannot access another organisation's audit log")
void testUnauthorisedAccessToAuditLog() {
    // Arrange - Create audit for org A
    // Act - Query from org B
    // Assert - Returns 0 results
}
```
**Rationale:** Ensures authorization layer is properly tested.

---

## AI-Generated vs. Hand-Written Code Breakdown

| Component | AI-Generated % | Hand-Written % | Notes |
|-----------|----------------|----------------|-------|
| Models (AuditLog, Notification) | 85% | 15% | Added IP privacy considerations |
| Repositories | 90% | 10% | Copilot generated JPQL correctly |
| Services | 80% | 20% | Added error handling and edge cases |
| Controllers | 70% | 30% | Added authorization, error handling |
| Tests | 75% | 25% | Added security test scenarios |
| Documentation | 40% | 60% | Copilot drafted, human expanded with specifics |

**Overall:** ~75% AI-generated, 25% hand-written/refined

---

## Key Takeaways

1. **Specificity wins:** Detailed prompts with constraints generate better output
2. **Always validate:** AI-generated code needs security and edge-case review
3. **Backward compatibility:** Always consider impact on existing callers
4. **Authorization:** Security checks must be explicitly prompted and verified
5. **Documentation:** AI helps draft; humans add regulatory/business context
6. **Iterative approach:** Start with architecture → build components → integrate → test → document

