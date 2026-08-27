# IMPACT_ANALYSIS.md

This document describes the impact of adding the Notification & Audit Service to the Taskbridge API (branch: refactor/move-audit-to-notifications). It lists every file, module, and data model affected, the nature of each change (additive, breaking, migration required), security and compliance risks introduced by capturing IP addresses, recommended implementation approach and sequencing, and a brief note on how GitHub Copilot assisted this analysis.

Summary of changes (high level)
-------------------------------
- New service module: taskbridge-notify (Spring Boot)
- New REST endpoints: /v1/events, /v1/notifications, /v1/audit
- New database tables: audit_entries, notifications, outbox, idempotency_keys, audit_access
- New background components: outbox dispatcher, delivery workers (email/webhook/etc.)
- New configuration: JWT auth integration, Kafka settings, retention policies

Affected files and modules
--------------------------
Note: file paths use typical Java/Spring Boot layout. Exact paths may vary when generating implementation.

1) New files (additive)
- pom.xml (module `taskbridge-notify`) — Add a new Maven module or update parent pom to include module. (Additive; migration: none)
- src/main/java/com/taskbridge/notify/Application.java — Spring Boot application entry (Additive)
- src/main/java/com/taskbridge/notify/config/SecurityConfig.java — JWT validation and role extraction (Additive)
- src/main/java/com/taskbridge/notify/config/KafkaConfig.java — Kafka producer/consumer configuration (Additive)
- src/main/java/com/taskbridge/notify/controller/EventController.java — POST /v1/events (Additive)
- src/main/java/com/taskbridge/notify/controller/NotificationController.java — POST /v1/notifications (Additive)
- src/main/java/com/taskbridge/notify/controller/AuditController.java — GET /v1/audit (Additive)
- src/main/java/com/taskbridge/notify/service/AuditService.java — Business logic for audit writes and queries (Additive)
- src/main/java/com/taskbridge/notify/service/NotificationService.java — Notification creation & status management (Additive)
- src/main/java/com/taskbridge/notify/repository/AuditRepository.java — JPA or JDBC repository for audit_entries (Additive)
- src/main/java/com/taskbridge/notify/repository/OutboxRepository.java — CRUD for outbox (Additive)
- src/main/java/com/taskbridge/notify/model/AuditEntry.java — DTO/entity (Additive)
- src/main/java/com/taskbridge/notify/model/Notification.java — DTO/entity (Additive)
- src/main/resources/application.yml — configuration entries for DB, Kafka, JWT, retention (Additive)
- docs/SPEC.md — already added (Additive)
- docs/PROMPTS.md — already added (Additive)

2) Database migration files (additive, migration required)
- db/migrations/V1__create_audit_notifications_outbox.sql — creates audit_entries, notifications, outbox, idempotency table and audit_access. (Migration required)
- db/migrations/V2__add_indexes_and_constraints.sql — adds indexes and uniqueness constraints (Migration required)
- db/migrations/V3__add_retention_policy.sql — scheduled job / helper SQL for retention/archival (Additive, migration required)

3) Existing files that may require modification
- ops/deployments/*.yml or Helm charts — Add new deployment, service, and autoscaling settings for the taskbridge-notify service. (Additive; non-breaking)
- CI config (e.g., .github/workflows/ci.yml) — Add build/test steps for the new module. (Additive; non-breaking)
- infra/terraform or cloud manifests — Add Kafka topics, DB migration step, role bindings, and service account policies needed by the new service. (Additive; may require infra review)
- Central authentication/authorization config (if statically listing services) — Add the notify service to allowed clients. (Additive or config change)

4) Telemetry / Logging / Monitoring
- observability config (Prometheus scrape config, Grafana dashboards) — Add metrics endpoints and dashboards (Additive)
- Sentry / error reporting config — add new project or update existing to capture errors from notify service (Additive)

Nature of changes
-----------------
- Additive (low-risk): New module files, new controllers, new background workers, and CI changes are additive and do not change existing behavior.
- Potentially breaking (moderate risk): If existing services are modified to start depending on new notification semantics synchronously (they should not), or if shared DB schemas are modified in-place without backward-compatible migration steps.
- Migration required (must plan): All DB schema changes (audit_entries, notifications, outbox, idempotency_keys, audit_access) require migrations and possibly data backfill. Any change that adds NOT NULL constraints or changes column types needs a safe multi-step migration.

Data models affected
--------------------
For each table below: indicate whether additive, breaking, and migration details.

1) audit_entries (NEW)
- Nature: Additive. New table to store audit records.
- Migration required: Create table via migration script. No data migration from existing systems unless you plan to consolidate logs; if migrating pre-existing audit data into this table, write ETL/backfill scripts.
- Backwards compatibility: No existing code depends on this table by default.

2) notifications (NEW)
- Nature: Additive. Stores ad-hoc notification requests and status.
- Migration required: Create table via migration; consider backfilling status for any pre-existing queued notifications if migrating from another system.

3) outbox (NEW)
- Nature: Additive. Used for transactional outbox pattern to guarantee DB→Kafka reliability.
- Migration required: Create table via migration. If switching from a different delivery method, coordinate cutover so producers start writing to outbox before dispatcher is disabled.

4) idempotency_keys (NEW)
- Nature: Additive. Optional table used to enforce idempotency for incoming events.
- Migration required: Create table and add unique constraint on (source, idempotency_key).

5) audit_access (NEW)
- Nature: Additive. Log of audit queries for compliance.
- Migration required: Create table. Consider retention and access controls for this table as it contains metadata about who queried audit data.

6) Existing domain tables (e.g., tasks, users)
- Nature: No direct schema changes required by the Notification & Audit Service itself. However, consumers/producers of events (task-service, user-service) will need to include consistent resource identifiers and possibly idempotency keys in emitted events (Additive change in producers; non-breaking if optional).

Security & compliance risks introduced by capturing IP addresses
----------------------------------------------------------------
Capturing IP addresses in audit entries or notification logs brings both operational value and privacy/compliance risk. Below are the risks and recommended mitigations.

Risks
- Privacy law exposure: IP addresses can be personal data under GDPR (they can identify an individual directly or indirectly). Storing IPs without legal basis risks non-compliance.
- Increased attack surface: Logs containing IPs could be exfiltrated, leaking user location or identity information.
- Retention & minimization issues: Keeping IPs longer than necessary increases liability in breach scenarios.
- Access/logging exposure: Audit logs are often widely accessible to ops/engineers. IPs should be treated as sensitive; permissive access policies increase risk.
- Correlation risk: Captured IPs can be used to correlate users across services or reconstruct behavior, creating additional privacy concerns.

Mitigations / Controls
- Legal basis & DPIA: Confirm legal basis for storing IPs (consent, legitimate interest, contractual necessity) and perform a Data Protection Impact Assessment (DPIA) if required.
- Minimize collection: Only capture IP when strictly necessary. Consider storing a truncated IP (e.g., remove last octet for IPv4) or a hashed value.
- Pseudonymize or hash: Store salted hashes of IPs instead of raw IPs for investigative correlation while reducing direct PI exposure. Ensure salt is rotated appropriately and stored securely.
- Field-level encryption: Use application-side encryption for IP fields with key management (KMS) to ensure at-rest protection beyond DB-level encryption.
- Access controls: Restrict read access to audit_entries and audit_access to only roles that require it (e.g., AUDIT_READ). Log and monitor access to these tables.
- Short retention: Apply a retention policy to IPs (e.g., remove or anonymize after 30 days unless needed for legal hold). Implement automatic purge/archival jobs.
- Logging pipeline scrubbing: Ensure that downstream logs (e.g., aggregated logs shipped to third-party services) do not include raw IPs — add scrubbing/transforms in the logging pipeline.
- Masking in UI and exports: When displaying audit records in admin UIs or exporting them, mask or redact IPs unless explicitly authorized and logged.
- Secure backups and exports: Ensure backups and data exports are encrypted and access-controlled. Audit any export of raw audit data.
- Incident response: Treat stored IPs as sensitive — include their exposure in incident classification and notification procedures.

Recommended implementation approach and sequencing
--------------------------------------------------
A safe, low-risk rollout sequence with minimal downtime and reversible steps:

Phase 0 — Planning & approvals
- Finalize SPEC.md and data retention policies.
- Perform DPIA and legal review specifically for IP capture and retention rules.
- Define RBAC roles and identify principals that need AUDIT_READ.
- Define configuration: retention windows, topics, partitioning strategy.

Phase 1 — Schema & infra (non-breaking)
- Create migration scripts to add new tables (audit_entries, notifications, outbox, idempotency_keys, audit_access) with nullable fields where needed. Do NOT add NOT NULL constraints that could break producers.
- Provision Kafka topics and topic-level access controls.
- Add Prometheus metrics config and observability hooks.

Phase 2 — Implement producer changes & outbox support (backwards-compatible)
- Implement the taskbridge-notify module skeleton and outbox write path.
- Update producers (e.g., task-service) to optionally emit events to /v1/events (use feature flag to control rollout).
- Ship the outbox dispatcher in a way that it can run concurrently (multiple consumers but single-writer semantics per partition_key if ordering required).

Phase 3 — Traffic switch & verification (safe cutover)
- Enable producers to write audit entries and outbox rows in their transactions or forward to central /v1/events.
- Monitor outbox pending queue, ensure dispatcher publishes messages and marking SENT consistently.
- Run integration tests (Testcontainers for Postgres and Kafka) in staging.

Phase 4 — Hardening & constraints
- After verifying stable behavior, add NOT NULL constraints and uniqueness indexes where safe (multi-step: add column nullable → backfill → set NOT NULL).
- Enable idempotency key enforcement in DB via unique index on (source, idempotency_key) and enable 409 behavior in API.

Phase 5 — Retention & archival
- Implement scheduled jobs for retention/archival as per policy. Start with conservative retention and shorten once validated.

Phase 6 — Production rollout & monitoring
- Progressive rollout controlled by feature flags, with constant monitoring of metrics and alerting on outbox errors, delivery failures, and audit_access anomalies.

Phase 7 — Documentation & training
- Update runbooks, ops playbooks, and data access policies. Train support/audit personnel on correct usage and masking rules.

Rollback considerations
- Because the work is additive, rollback is primarily configuration-based: stop producers from writing to the new endpoints and suspend the dispatcher. For DB-level rollbacks, DO NOT DROP audit tables without archiving; instead, stop writes and revert application changes.

How Copilot Assisted This Analysis
----------------------------------
I used GitHub Copilot (both Chat and inline/code suggestions) to draft the initial impact analysis, risk enumerations, and rollout sequencing. Below are the prompts I used, what Copilot produced, and where I validated or overrode its output.

1) Prompt (architecture & impact outline)
- Exact prompt I used with Copilot Chat:
  "You are a senior backend engineer. Produce an impact analysis for introducing a Notification & Audit Service into an existing Java microservice application. Include affected files/modules, database changes, migration risks, and recommended rollout sequencing. Provide a concise table of changes and whether they're additive or breaking."
- What Copilot produced:
  - A structured outline listing new tables, endpoints, and a phased rollout.
- Where I validated/overrode:
  - Copilot omitted `audit_access` logging as a compliance artifact; I added it.
  - I expanded the sequencing to include DPIA/legal review for IP capture.

2) Prompt (IP capture risks)
- Exact prompt I used with Copilot Chat:
  "List security and compliance risks of storing client IP addresses in audit logs and recommend mitigations suitable for a GDPR/CCPA environment. Include technical controls (pseudonymization, encryption, retention)."
- What Copilot produced:
  - A list of risks and mitigations, including pseudonymization and retention suggestions.
- Where I validated/overrode:
  - Copilot suggested indefinite retention as an option for forensic reasons — I removed that and enforced short retention with legal hold exception and explicit DPIA requirement.
  - I added more explicit guidance about hashing and salts and flagged KMS/key management as required for encryption.

3) Prompt (migration sequencing)
- Exact prompt I used with Copilot Chat:
  "Produce a zero-downtime, multi-phase migration plan to add audit_entries, outbox, and notifications tables and safely enable transactional outbox publishing for an existing service."
- What Copilot produced:
  - A high-level phase plan describing adding tables and switching producers.
- Where I validated/overrode:
  - Copilot's phases were sound but light on the exact safe-migration steps (nullable-add → backfill → not-null). I added explicit safe steps and emphasized feature flags + monitoring.

Why I validated or changed Copilot outputs
- Copilot outputs are a strong starting point but occasionally omit organization-specific compliance steps (DPIA, legal basis) or produce advice that is too generic (e.g., "keep data forever for forensic reasons"). For privacy-sensitive fields like IP addresses I applied stricter practice (short retention, hashing/pseudonymization, legal sign-off).
- I also ensured the migration steps follow zero-downtime patterns: add nullable columns, backfill, then enforce constraints.

Conclusions and actionables
---------------------------
- The Notification & Audit Service is primarily additive but requires careful DB migrations and legal review when capturing IP addresses or other PII.
- Prioritize DPIA, RBAC, retention policies, and scrubbing in logging/export pipelines before enabling IP capture in production.
- Implement the outbox pattern and idempotency in small, tested steps behind feature flags; validate behavior in staging with Testcontainers.

If you want, I can now:
- Create the initial DB migration files (V1, V2) on this branch.
- Generate the Java module skeleton (pom.xml, Application.java, controller/service/repository skeletons).
- Open a PR with this change and the other docs already added.

