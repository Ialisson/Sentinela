# ADR 0001: RabbitMQ for the asynchronous risk workflow

- Status: accepted
- Date: 2026-10-07

## Context

The transaction API must accept work without waiting for risk analysis. The local project needs retries, dead-letter handling, and a reproducible environment without an AWS account.

## Decision

Use RabbitMQ with a durable direct exchange, durable work queue, three retries with exponential backoff, and a dead-letter queue. The API writes the transaction and an outbox row in one PostgreSQL transaction. A publisher sends outbox rows and marks them published only after a broker confirmation.

## Consequences

RabbitMQ runs locally in Compose and exposes its management UI. The outbox can publish a message more than once after a crash between broker confirmation and the database commit, so the worker locks the transaction row and treats completed work as a duplicate. Delivery is at least once; exactly-once delivery is not claimed.

Amazon SQS/SNS would better match a production AWS deployment and reduce broker operations, but would make this local portfolio environment depend on LocalStack or AWS setup. The broker topology is intentionally isolated so a transport adapter can be replaced later.
