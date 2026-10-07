# ADR 0002: PostgreSQL as the source of truth, Redis for IP lookup

- Status: accepted
- Date: 2026-10-07

## Context

The system needs idempotency, durable transaction state, and a fast lookup for a manually maintained suspicious-IP set.

## Decision

Store transaction state, analysis results, and outbox rows in PostgreSQL. Enforce unique transaction and idempotency keys in the database. Use a row lock while a worker analyzes a transaction so concurrent deliveries cannot apply the result twice. Store the suspicious-IP set in Redis and use it as a lookup cache.

## Consequences

PostgreSQL provides the unique constraints, transactional outbox, and row-level locking needed for this workflow. Redis is not authoritative; if it is unavailable, the worker continues with the other rules and increments a failure metric. This fail-open policy keeps analysis available but can miss an IP signal.

The API and worker currently share a database schema to keep the demo small. This is a deliberate first-stage coupling. A production split should give each service data ownership and publish completed analysis as a separate event. A document database is deferred because the current access patterns are keyed lookups and transactional state transitions, which fit PostgreSQL better; no historical query workload currently justifies a second database.
