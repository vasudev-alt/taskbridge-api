# SPEC.md

Notification & Audit Service — Specification
===========================================

Purpose
-------
The Notification & Audit Service centralizes two related responsibilities for Taskbridge:

1. Notification routing: accept ad-hoc notifications and internal events, transform them into notification messages (email, webhooks, in-app), and publish them to a delivery system or channel-specific workers.
2. Audit logging: persist authoritative audit records for important system and user events (task.created, task.updated, comment.added, task.deleted, etc.) and provide a query API for authorized consumers.

Design goals
------------
- Strong reliability: ensure events are durably recorded and notifications are reliably delivered.
- Clear auditability: immutable audit entries, retention policy, and restricted read access.
- Simple deployability: Spring Boot app, PostgreSQL for durable storage, Kafka for message distribution.
- Operational visibility: metrics, structured logs, and retriable delivery.

Components
----------
- REST API (Spring Boot)
  - /v1/events — ingest internal events (async ingestion, idempotent)
  - /v1/notifications — send ad-hoc notifications (sync request creates notification record + publishes delivery event)
  - /v1/audit — query audit entries (requires AUDIT_READ role)
- Database: PostgreSQL
  - Tables: audit_entries, notifications, outbox (reliable delivery)
- Message bus: Kafka
  - Topics: notifications.deliver, notifications.audit-events (naming configurable)
- Background workers
  - Outbox dispatcher — reliably publishes newly inserted outbox rows to Kafka and marks them sent
  - Delivery workers — channel-specific workers (email, webhook) subscribe to notifications.deliver
- Observability
  - Metrics: request rates, error rates, outbox queue size, delivery success/failure counts
  - Tracing: distributed tracing headers propagated from incoming events

API Contracts
-------------
Note: All endpoints require TLS. Most endpoints require Authorization: Bearer <JWT>. Role-based checks documented below.

1) POST /v1/events
- Purpose: Accept internal events emitted by other Taskbridge services.
- Headers:
  - Content-Type: application/json
  - Authorization: Bearer <token>
  - Idempotency-Key: <opaque-string> (optional but recommended for retry safety)
- Body (example):
  {
    "id": "uuid-...",
    "eventType": "task.updated",
    "source": "task-service",
    "timestamp": "2026-08-01T12:00:00Z",
    "payload": { ... }
  }
- Responses:
  - 202 Accepted — event accepted for processing
  - 400 Bad Request — validation error
  - 401 Unauthorized — auth missing/invalid
  - 409 Conflict — idempotency key already used for a different event
- Behavior:
  - Validate payload and required fields.
  - If Idempotency-Key provided: enforce uniqueness per source and key (db constraint + idempotency table)
  - Persist an audit entry and an outbox row in a single transaction (write to audit_entries and outbox) such that the outbox dispatcher will publish to Kafka.

2) POST /v1/notifications
- Purpose: Create an ad-hoc notification (e.g., user-facing alert or system notification)
- Headers: Authorization: Bearer <token>
- Body (example):
  {
    "recipient": "user:42",
    "channel": "email",
    "template": "task-assigned",
    "payload": { "taskId": 1001 }
  }
- Responses:
  - 201 Created — notification created and delivery requested
  - 400 Bad Request
  - 401 Unauthorized
- Behavior:
  - Persist a `notifications` row and emit a message to notifications.deliver via outbox.

3) GET /v1/audit
- Purpose: Query audit entries (read-only)
- Headers: Authorization: Bearer <token>
- Query params: action, resource, actor, from (ISO), to (ISO), limit, offset
- Responses:
  - 200 OK — returns list of audit entries
  - 401 Unauthorized — missing credentials
  - 403 Forbidden — lacks AUDIT_READ role
- Behavior:
  - Enforce role check: only principals with AUDIT_READ may query
  - Log queries to an `audit_access` log for compliance (who queried, filters used, timestamp)

Data model
----------
1) audit_entries (Postgres)
- Columns:
  - id UUID PRIMARY KEY
  - actor TEXT NOT NULL
  - action TEXT NOT NULL
  - resource TEXT NOT NULL
  - timestamp TIMESTAMPTZ NOT NULL
  - meta JSONB NULL
  - created_at TIMESTAMPTZ DEFAULT now()

- Indexes:
  - CREATE INDEX idx_audit_action ON audit_entries(action);
  - CREATE INDEX idx_audit_resource ON audit_entries(resource);
  - CREATE INDEX idx_audit_timestamp ON audit_entries(timestamp);
  - CREATE INDEX idx_audit_meta_gin ON audit_entries USING GIN (meta);

2) notifications
- Columns:
  - id UUID PRIMARY KEY
  - recipient TEXT NOT NULL
  - channel TEXT NOT NULL
  - template TEXT NOT NULL
  - payload JSONB
  - status TEXT NOT NULL DEFAULT 'PENDING' -- PENDING, SENT, FAILED
  - attempts INT DEFAULT 0
  - created_at TIMESTAMPTZ DEFAULT now()

3) outbox
- Columns:
  - id BIGSERIAL PRIMARY KEY
  - aggregate_type TEXT NOT NULL -- e.g., "audit_entry", "notification"
  - aggregate_id UUID NOT NULL
  - payload JSONB NOT NULL
  - topic TEXT NOT NULL
  - partition_key TEXT NULL
  - status TEXT NOT NULL DEFAULT 'PENDING' -- PENDING, SENT, FAILED
  - retries INT DEFAULT 0
  - last_error TEXT NULL
  - created_at TIMESTAMPTZ DEFAULT now()

Operational patterns
--------------------
- Idempotency: Support an Idempotency-Key for POST /v1/events. Implement a db uniqueness constraint on (source, idempotency_key) and return 409 for conflicting reuses.
- Outbox pattern: Write audit_entries and outbox rows in the same DB transaction. The outbox dispatcher reads PENDING outbox rows, publishes them to Kafka, and marks them SENT in the DB. Use at-least-once publication with deduping consumers or include message IDs for de-duplication.
- Delivery worker retries: Use exponential backoff with capped retries. Failed notifications move to a dead-letter topic after N attempts.
- Retention: Keep audit_entries for a configurable retention period (e.g., 3 years) and then archive or delete according to compliance. Provide scheduled job to move old entries to an archive table or cold storage.

Transactions & consistency
------------------------
- Use local DB transaction to persist audit entries and outbox rows. Avoid synchronous blocking Kafka calls inside DB transaction. Use an outbox dispatcher to bridge DB→Kafka.
- Preserve ordering per aggregate by using partition_key derived from resource id where necessary.

Security
--------
- Authentication: JWT bearer tokens issued by central auth service. Validate tokens and extract principal and roles.
- Authorization: Role-based check for audit read (role AUDIT_READ). Only admins or auditing services get long-lived credentials.
- Encryption: TLS in transit. At-rest encryption managed by DB infrastructure (cloud provider-managed or PGP if self-hosting). Mask sensitive fields in audit meta if necessary.
- Audit access: Any query to /v1/audit must be recorded in audit_access log (who, why, filters).

Observability
-------------
- Metrics: expose Prometheus metrics: http_request_duration_seconds, outbox_pending_count, outbox_send_errors_total, notifications_sent_total, notifications_failed_total.
- Logging: structured JSON logs with event ids and correlation ids. Log outbox dispatcher errors and dead-letter events.
- Tracing: propagate traceparent and instrument important spans: request handler, DB write, outbox publish.

Backwards compatibility & Migration
----------------------------------
- Add new columns in backward-compatible steps: add nullable columns, backfill, then set NOT NULL.
- Deploy outbox dispatcher before switching producers to write outbox-only transactions.

Testing
-------
- Unit tests for controllers, services, and repositories.
- Integration tests using Testcontainers for Postgres and Kafka verifying the transactional outbox flow and idempotency handling.
- Contract tests for OpenAPI endpoints.

Performance & scaling
---------------------
- Horizontal scale: multiple instances behind a load balancer.
- Database scaling: primary/replica setup for reads; partitioning/archiving for very large audit tables.
- Kafka topics sized by partition count based on throughput; use partitioning by resource id for ordering.

Configuration
-------------
- Environment: DB URL, Kafka bootstrap servers, JWT public keys, outbox dispatcher concurrency, retention window.
- Feature flags: enable/disable audit writes, toggle outbox dispatcher.

Admin & maintenance
-------------------
- Endpoints or CLI to requeue outbox rows and inspect dead-letter entries.
- Database migrations tracked with Flyway or Liquibase; run migrations during deploy.

Open questions / future work
---------------------------
- Which fields in `meta` are PII and need redaction? (Policy decision.)
- Retention SLA and legal hold features.
- Support for more channels (SMS, push) and templating service for localized messages.

Appendix: Example SQL
---------------------
CREATE TABLE audit_entries (
  id UUID PRIMARY KEY,
  actor TEXT NOT NULL,
  action TEXT NOT NULL,
  resource TEXT NOT NULL,
  timestamp TIMESTAMPTZ NOT NULL,
  meta JSONB,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_audit_action ON audit_entries(action);
CREATE INDEX idx_audit_resource ON audit_entries(resource);
CREATE INDEX idx_audit_timestamp ON audit_entries(timestamp);
CREATE INDEX idx_audit_meta_gin ON audit_entries USING GIN (meta);

-- outbox table
CREATE TABLE outbox (
  id BIGSERIAL PRIMARY KEY,
  aggregate_type TEXT NOT NULL,
  aggregate_id UUID NOT NULL,
  payload JSONB NOT NULL,
  topic TEXT NOT NULL,
  partition_key TEXT,
  status TEXT NOT NULL DEFAULT 'PENDING',
  retries INT DEFAULT 0,
  last_error TEXT,
  created_at TIMESTAMPTZ DEFAULT now()
);


