# Architecture Overview

Project Service owns tenant-scoped project lifecycle and emits canonical events (HTTP+JSON events or messages) to notify other services.

Notification & Audit Service subscribes to those events (message bus or authenticated webhook) and implements the integration contract: event schema (tenant_id, project_id, actor, action, timestamp, metadata) and exactly-once processing semantics.

Layered architecture: API Layer -> Application/Domain Layer -> Integration/Outbox -> Message Broker -> Notification & Audit Service -> Storage (notifications, audit_logs).

On inbound API request the Project Service validates tenant context, applies domain changes, writes state and an outbox record within the same DB transaction, and publishes the outbox to the broker.

The Notification & Audit Service consumes events, enriches them (actor resolution, policy checks), persists notifications (per-tenant index) and append-only audit logs, and pushes user-facing delivery (email/webhook) asynchronously.

Multi-tenant B2B rationale: tenant-aware events, strict tenant_id in all messages, per-tenant partitioning in storage and broker topics ensure isolation, scalability, and regulatory auditability.

Key decisions: use outbox pattern + broker for reliability and decoupling; canonical event schema for backwards compatibility; separate storage for notifications vs append-only audit logs for retention and query patterns.

Trade-offs: eventual consistency between project state and notifications vs simpler synchronous coupling; operational overhead of broker and schema governance vs improved scalability and loose coupling.

Security & compliance: tenant scoping, signed events, RBAC checks in consumer, and immutable audit records for compliance.

Observability: structured tracing linking request -> outbox -> consumed event -> persisted audit/notification, and DLQ for failed deliveries.

This design prioritizes reliability, tenant isolation, and auditability for a B2B SaaS product while allowing independent scaling of services.
