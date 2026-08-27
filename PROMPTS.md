# PROMPTS.md

This document records the exact prompt chain I used with GitHub Copilot while building the Notification & Audit Service for the Taskbridge API, which lives on branch `refactor/move-audit-to-notifications`.

Overview
--------
- Goal: Design and implement a Java-based Notification & Audit Service that centralizes notifications and audit logging for Taskbridge.
- Tools / Copilot features used: Copilot Chat, Copilot inline/code suggestions (IDE inline suggestions).
- Prompting techniques demonstrated: role-based, specificity, decomposition, few-shot, constraint, iterative refinement.

Prompt chain (in the order executed)
-----------------------------------
1) Name: High-level architecture (role-based + specificity)
- Exact prompt:

"You are an experienced backend architect for distributed Java services. Design a Notification & Audit Service for the Taskbridge API. Requirements: single service that accepts events from other services, writes audit entries to Postgres, emits notification events to a message bus (Kafka), and exposes a small REST API for querying audit records and sending ad-hoc notifications. Include: responsibilities, components, sequence of operations for an incoming "task.updated" event, failure modes, and data retention recommendations. Prefer simple, production-ready choices for a greenfield Java microservice (Spring Boot, PostgreSQL, Kafka). Limit to ~300–600 words."

- Copilot feature chosen: Copilot Chat
- Prompting techniques applied: role-based, specificity
- Rationale: Start with a focused, role-driven architectural brief so Copilot provides a structured, opinionated design aligned to Java/Spring Boot conventions.

2) Name: Project skeleton and packages (decomposition + constraint)
- Exact prompt:

"Generate a Java package structure and an initial Maven pom.xml for a Spring Boot Notification & Audit service named `taskbridge-notify`. Packages: api, service, repository, model, config, events. Provide skeleton interfaces and classes for NotificationController, AuditController, NotificationService, AuditService, NotificationRepository, AuditRepository, EventDispatcher. Keep code small — no method bodies beyond TODO stubs. Output file-by-file in a compact form so I can paste into files."

- Copilot feature chosen: Copilot inline/code suggestions (IDE inline suggestions)
- Prompting techniques applied: decomposition, constraint
- Rationale: Ask for skeletons so I can quickly scaffold the repo. Constraining to TODO stubs keeps suggestions brief and focused.

3) Name: DTOs and database schema (few-shot + specificity)
- Exact prompt:

"Here are 3 example audit records (few-shot):

1) {"id":"uuid-1","actor":"user:42","action":"task.updated","resource":"task:1001","timestamp":"2026-08-01T12:00:00Z","meta":{"field":"status","old":"open","new":"done"}}
2) {"id":"uuid-2","actor":"system","action":"task.deleted","resource":"task:1002","timestamp":"2026-08-01T12:01:00Z","meta":{}}
3) {"id":"uuid-3","actor":"user:7","action":"comment.added","resource":"task:1001","timestamp":"2026-08-01T12:02:00Z","meta":{"commentId":"c-7"}}

From those examples, produce Java DTOs (using Java 17 records or Lombok — choose Java records) and a Postgres CREATE TABLE statement for `audit_entries` including indexes for query fields (action, resource, timestamp). Use types appropriate for UUID and timestamp."

- Copilot feature chosen: Copilot Chat (with code generation)
- Prompting techniques applied: few-shot, specificity
- Rationale: Provide concrete examples so Copilot infers fields and types, and ask for both DTOs and DB schema in one step.

4) Name: REST API OpenAPI fragment (specificity + constraint)
- Exact prompt:

"Produce an OpenAPI 3.0 YAML fragment describing these endpoints for taskbridge-notify:
- POST /v1/notifications — send an ad-hoc notification (body: {recipient, channel, template, payload})
- POST /v1/events — ingest an internal event (body: {eventType, payload, source, idempotencyKey?})
- GET /v1/audit — query audit entries with params: action, resource, actor, from, to, limit, offset

Constrain responses: responses must use JSON, provide example request and response bodies, and include proper status codes (202 for ingestion, 200 for queries, 201 for created notifications). Keep the fragment to the three paths only."

- Copilot feature chosen: Copilot inline/code suggestions
- Prompting techniques applied: specificity, constraint
- Rationale: I wanted a machine-readable contract to copy into the repository and to drive controller implementations.

5) Name: Event handling and idempotency (iterative refinement + decomposition)
- Exact prompt:

"Update the earlier event ingestion design: describe how to make `POST /v1/events` idempotent for retries, including the server-side algorithm (idempotency key handling), database uniqueness constraints, and interaction with transactional writes to both audit_entries table and message bus publish. Also provide a short pseudocode snippet (Java) showing the transactional flow using Spring @Transactional and an outbox table for reliable delivery to Kafka."

- Copilot feature chosen: Copilot Chat
- Prompting techniques applied: iterative refinement, decomposition
- Rationale: After the first design I needed reliable exactly-once semantics between DB and Kafka — iterative ask to extend the design and generate the outbox pattern code scaffold.

6) Name: Tests and curl examples (few-shot + constraint)
- Exact prompt:

"Using two example curl commands as few-shot seeds, generate a set of curl commands and a short JUnit 5 test outline for:
- Ingesting an event (POST /v1/events)
- Querying audits (GET /v1/audit?resource=task:1001)

Constraints: tests should be simple integration-style outlines (mock Kafka/DB using Testcontainers) and curl commands must include Authorization header `Authorization: Bearer <token>`.

Example curls:

curl -X POST https://api.example/v1/events -H 'Content-Type: application/json' -d '{"eventType":"task.updated","payload":{}}'

curl -X GET 'https://api.example/v1/v1/audit?resource=task:1001' -H 'Authorization: Bearer <token>'
"

- Copilot feature chosen: Copilot inline/code suggestions
- Prompting techniques applied: few-shot, constraint
- Rationale: Produce runnable examples and a testing outline to validate the design.

Post-Generation Corrections
--------------------------
This section lists every change I made to Copilot's outputs while authoring the service, what was wrong/missing in each output, and how I fixed it.

1) After architecture draft (Prompt 1)
- Problem: Copilot suggested using an in-memory queue for retries in one paragraph — unsafe for production.
- Fix: Replaced in-memory retry suggestion with a persistent outbox pattern and durable Kafka delivery; added note to use Testcontainers during integration tests to validate failure/recovery.

2) Project skeleton (Prompt 2)
- Problem: Generated pom.xml had an older Spring Boot version and lacked a plugin for Java 17. Also the package names used `org.example`.
- Fix: Updated pom to Spring Boot 3.x, set Java version to 17, added spring-boot-maven-plugin, and updated groupId to `com.taskbridge.notify`.

3) DTOs and DB schema (Prompt 3)
- Problem: Copilot used `timestamp with time zone` but used a text column for `meta` with no JSONB type; it also used `varchar(255)` for actor and resource causing potential truncation.
- Fix: Changed `meta` to `jsonb`, set `actor` and `resource` to `text`, used `uuid` type for id and `timestamptz` for timestamp. Added indexes on (resource), (action), and a GIN index on (meta) for some search use-cases.

4) OpenAPI fragment (Prompt 4)
- Problem: The generated OpenAPI fragment had `201` for POST /v1/events but the intended contract is async ingestion (202 Accepted).
- Fix: Adjusted status code to 202. Also added `Idempotency-Key` header to the spec for POST /v1/events and documented 409 conflict when duplicate idempotency key is detected.

5) Outbox pseudocode (Prompt 5)
- Problem: Generated pseudocode published to Kafka directly inside the DB transaction thread (blocking), without describing background delivery worker.
- Fix: Reworked design so the DB transaction writes an outbox row; a separate background thread/process reads the outbox and publishes to Kafka, marking rows as sent. Added notes about exponential backoff and dead-letter queue for failing outbox messages.

6) Tests & curl examples (Prompt 6)
- Problem: Copilot suggested real hostnames in examples and used a hard-coded JWT secret in test configuration.
- Fix: Replaced hostnames with placeholders, used Testcontainers for DB and Kafka configuration in tests, and indicated usage of generated ephemeral JWTs for test auth (not hard-coded secrets).

7) Naming and REST conventions (across prompts)
- Problem: Several endpoints used inconsistent pluralization and path nesting (e.g., /notifications/send vs /v1/notifications).
- Fix: Standardized to `/v1/notifications` and `/v1/events` and `/v1/audit`, documented the reasons (consistency, versioning).

8) Security (applied after initial outputs)
- Problem: Copilot's drafts omitted authentication and authorization details for sensitive audit data.
- Fix: Added requirement for JWT-based auth with role-based checks for audit queries (role: AUDIT_READ), require TLS, and log access to audit queries separately for compliance.

9) DB migration considerations
- Problem: Initial schema had no migration notes and would require a downtime-unfriendly migration for adding `jsonb` meta.
- Fix: Added migration steps with `ALTER TABLE` in safe incremental steps, including adding new columns nullable, backfilling, and then switching to NOT NULL with a follow-up migration.

Summary
-------
I used Copilot Chat and in-IDE inline suggestions across a sequence of prompts combining role-based framing, decomposition, few-shot examples, constraints, and iterative refinement. For each generated artifact I reviewed Copilot's outputs, corrected configuration and production hardening issues (migrations, indexes, idempotency, outbox pattern, security), and used the refined outputs to author the SPEC.md in the repository.

If you want, I can now create JIRA-style issues for the remaining implementation tasks or generate the actual Java classes and tests on this branch.
