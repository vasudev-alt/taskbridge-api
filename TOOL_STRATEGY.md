# TOOL_STRATEGY.md

This document records how I used GitHub Copilot across the Notification & Audit Service case study, the features I would pick for several realistic scenarios, and concrete limitations I encountered (and how I fixed them).

Feature Usage Log (6+ entries, 4+ Copilot features)
---------------------------------------------------
1) Copilot Chat — High-level architecture and requirements
- What I used: Copilot Chat to draft the overall Notification & Audit Service architecture (responsibilities, components, sequence for task.updated, failure modes).
- Why this feature: Chat allows a conversational, role-based framing ("You are an experienced backend architect...") and iterative refinement; it is better than inline suggestions for multi-paragraph design explanations.
- What happened: Copilot produced a structured, opinionated design that I used as the baseline; I then refined it to prefer an outbox pattern and added compliance controls.

2) Copilot inline/code suggestions �� Project skeleton & DTOs
- What I used: In-editor inline completion to generate package skeletons, controllers, DTOs (Java records) and a compact pom.xml draft.
- Why this feature: Inline suggestions are fast for single-file scaffolding and boilerplate; they are more convenient than Chat for copy-paste code generation when you want file-by-file completions.
- What happened: The skeletons sped up scaffolding, but I had to update the pom.xml to a modern Spring Boot 3.x / Java 17 combo and change package groupId.

3) Copilot Pull Request Assistant (PR review suggestions)
- What I used: The PR assistant / review-oriented feature to draft reviewer comments and peer-review simulation items (specific, file-linked comments about EventController header validation, DB indexes, and governance checks).
- Why this feature: The PR-focused workflow is oriented to code review style feedback, linking comments to files/lines; it beats free-form Chat when you need reviewer-formatted comments.
- What happened: I produced the three peer review comments included in PR_DESCRIPTION.md and linked them to specific files and responsibilities.

4) Copilot CLI / Batch generation (command-like multi-file generation)
- What I used: A command-style prompt to generate multiple files at once (e.g., request: "Output file-by-file" for skeleton classes and SQL migrations) and then paste into the repository.
- Why this feature: When creating many small files at once, CLI-style or batch prompts let me iterate quickly and keep consistent naming across files; inline suggestions don't scale as well for multi-file outputs.
- What happened: I generated compact content for many files and then edited the outputs to match naming and production requirements.

5) Copilot Chat — Iterative refinement for idempotency & outbox pseudocode
- What I used: Copilot Chat to extend the design to idempotency handling, an outbox table, and a pseudocode transactional flow using Spring @Transactional.
- Why this feature: Chat's iterative back-and-forth helps to evolve design decisions, add constraints (no Kafka inside DB transaction), and produce explanatory pseudocode alongside prose.
- What happened: Copilot suggested a pattern that initially published to Kafka directly inside the DB transaction; I corrected it to write an outbox row and described a dispatcher worker.

6) Copilot inline suggestions for OpenAPI fragments and curl examples
- What I used: Inline/code suggestions to create OpenAPI YAML fragments for the three endpoints and a set of curl examples and JUnit test outlines.
- Why this feature: Inline suggestions provide syntactically-correct YAML and short code snippets that can be pasted into files; ideal for API contracts and example clients.
- What happened: I accepted the structure but changed the POST /v1/events response from 201 to 202 and added Idempotency-Key header and error semantics.

Scenario Responses (feature recommendation + brief rationale)
-----------------------------------------------------------
1) Understanding a complex 600-line legacy service in an unfamiliar codebase before wiring a new service to it
- Feature: Copilot Chat combined with semantic-code-search (repo-aware code search).
- Why: Use Chat to ask high-level questions and get explanations of the service's modules and data flow, but pair it with semantic-code-search to find relevant functions/classes across the repo. Chat provides narrative context while semantic search finds the 10–20 most relevant locations to inspect.

2) Generating consistent, standards-compliant request-validation middleware across 10 existing route handlers
- Feature: Copilot inline/code suggestions with batch file generation (or Copilot CLI if available).
- Why: Inline suggestions can scaffold per-route validators quickly and offer consistent code style. Use a batch-generation prompt to produce 10 handler-specific validator classes and small unit tests so the output is consistent across handlers and ready for minor edits.

3) Quickly verifying whether a JWT verification implementation correctly handles token expiry and signature tampering
- Feature: Copilot Chat for threat-model guidance plus inline suggestions for testcases; semantic-code-search to find JWT verification code.
- Why: Chat can propose targeted test scenarios and edge cases (expired tokens, clock skew, invalid signature, malformed headers). Then use inline suggestions to scaffold unit tests and semantic search to locate the function to test.

4) Enforcing that all commits to main pass linting and test coverage thresholds automatically, with no human intervention
- Feature: Copilot Chat to design CI workflow spec, then use inline suggestions to author the GitHub Actions YAML files.
- Why: Chat helps define the CI policy and rationale (linting rules, coverage thresholds, gating steps). Inline suggestions are better to emit correct GitHub Actions YAML and shell snippets to run the checks and enforce failure conditions.

5) Reviewing a contractor's AI-generated service module for security vulnerabilities before it reaches staging
- Feature: Copilot Pull Request Assistant + Copilot Chat for security checklist generation.
- Why: The PR assistant can annotate specific files/lines with review comments and flag common anti-patterns. Chat complements it by generating a focused security checklist (JWT misuse, sensitive logging, SQL injection, improper access control) tailored to the code in the PR.

6) Ensuring Copilot follows multi-tenant data isolation rules consistently across all developers and sessions
- Feature: Copilot Chat to craft strict prompting guidelines and a repository-level policy snippet, combined with inline/code suggestion guardrails (templates and code snippets that include tenant checks).
- Why: Use Chat to create a canonical policy and examples; then enforce by seeding repository templates and code snippets so inline completions default to safe patterns (e.g., require tenant_id in queries, use WHERE tenant_id = :tenantId). This combination provides both policy and practical templates.

Limitations Encountered (3 real situations)
-------------------------------------------
Note: I deliberately did not claim zero limitations — below are three real issues from this case study.

1) In-memory retry suggestion (design-level error)
- What I prompted: "Design a Notification & Audit Service, include failure modes and retry strategies." (initial architectural prompt to Copilot Chat)
- What went wrong: Copilot suggested in-memory queues for retry and temporary buffering in a paragraph of the architecture draft. That is unsafe for production because in-memory state is lost on process restart and doesn't survive crashes.
- How I detected it: During review I flagged "in-memory" as a reliability smell and cross-checked durability requirements; I noticed the absence of durable persistence like an outbox table.
- How I fixed it: Replaced the in-memory approach with the transactional outbox pattern in the SPEC.md and documented a dispatcher worker with retries and a dead-letter queue.
- What I'd do differently: Prompt Copilot explicitly for durable, production-ready retry strategies ("avoid in-memory buffers; prefer persistent outbox or external queues"), and ask for trade-offs up-front.

2) pom.xml / Java version & plugin mistakes (configuration-level error)
- What I prompted: "Generate a Maven pom.xml for a Spring Boot Notification & Audit service using Java 17." (inline/code suggestion)
- What went wrong: Copilot produced a pom referencing an older Spring Boot version and omitted the spring-boot-maven-plugin configuration for Java 17, risking build failures or non-ideal dependency versions.
- How I detected it: Local build/test cycle (or review) exposed the outdated Spring Boot parent and missing plugin settings.
- How I fixed it: Updated the pom to Spring Boot 3.x coordinates, added the spring-boot-maven-plugin, set Maven compiler target to 17, and adjusted groupId/artifactId.
- What I'd do differently: Include version pins in the prompt ("use Spring Boot 3.2.x and Java 17"), and request a minimal working mvn package command in the generated output as a smoke-test.

3) DB schema typing and API semantics (semantic/data-model error)
- What I prompted: "From these examples, produce Java DTOs and a Postgres CREATE TABLE statement for audit_entries." (few-shot prompt to Copilot Chat)
- What went wrong: Copilot used plain text/varchar for meta or used text where jsonb is appropriate; it also suggested 201 Created for event ingestion rather than 202 Accepted for async ingestion. Several fields were sized with VARCHAR(255) that could truncate identifiers.
- How I detected it: Reviewing generated SQL and API fragments, I noticed data types and status codes were not aligned with async, production-grade expectations.
- How I fixed it: Changed meta column to JSONB, used uuid and timestamptz types, adjusted varchar/text usage to TEXT for flexibility, added GIN index on meta, and set POST /v1/events to 202 Accepted with Idempotency-Key semantics.
- What I'd do differently: Add explicit constraints in prompts ("use jsonb for meta, use uuid for id, use timestamptz, return 202 for async ingestion") and request index suggestions for common query patterns.

Closing narrative
-----------------
I created TOOL_STRATEGY.md on branch refactor/move-audit-to-notifications and captured concrete Copilot feature usage, scenario decisioning, and documented limitations discovered during the work. If you'd like, I can now:

- Open a PR with these docs and request reviewers, or
- Generate the initial Flyway migration files and a skeleton Java module on this branch, or
- Produce a short training note that teaches team members how to prompt Copilot safely for this repository.

Which of those should I do next?