# ADR 0003: Prometheus metrics and OpenTelemetry traces

- Status: accepted
- Date: 2026-10-07

## Context

The asynchronous workflow needs measurements across the HTTP request, outbox publisher, broker, and worker. Performance claims must come from repeatable measurements.

## Decision

Expose Spring Boot Actuator metrics on a management port for Prometheus scraping. Export sampled traces over OTLP through an OpenTelemetry Collector to Jaeger. Emit structured JSON logs. Include a k6 scenario for the transaction submission endpoint.

## Consequences

Metrics use bounded labels such as message outcome; transaction IDs and user IDs are never metric labels. The local stack can be started with Docker Compose. No throughput or latency baseline is committed until k6 is actually run against a controlled environment.
