# Step 12: Monitoring, Distributed Tracing & Logging — Content Plan

---

## The Central Thread Running Through Everything

Every chapter will answer one of these three questions:

- **What problem are we solving?** (first principles)
- **How does this tool solve it?** (mechanics + Java comparison)
- **How did we actually use it at FinVerse?** (real world, Core Product Service)

And wherever a Java/Spring Boot concept maps directly to a Node.js/NestJS concept, I'll call it out explicitly with a side-by-side comparison — because you already have strong mental models from your Spring Boot notes, and the fastest way to learn the NestJS equivalent is to map it to what you already know.

---

## Chapter 1 — The Problem Space: Why Observability Exists

**Goal:** Understand *why* observability is a category of problem before touching any tool.

Topics:
- The three pillars of observability — Metrics, Logs, Traces — and what each one actually answers
- Why a single monolith doesn't need distributed tracing but a microservices system does
- The "five nines" story — why 99.999% uptime requires you to detect problems before users do
- The "improved performance by X%" interview trap — what interviewers are actually asking and what you need to have measured
- How Java teams approach this (Spring Boot Actuator + Micrometer + Datadog) vs how Node.js teams approach it — side-by-side before we go deep on either
- FinVerse's observability stack decision: OpenTelemetry → Datadog — why managed SaaS over self-hosted (Prometheus + Grafana + Jaeger)

---

## Chapter 2 — Metrics: Measuring What's Happening Right Now

**Goal:** Understand what metrics are, how they're collected, and how FinVerse uses them to answer "is the system healthy?"

Topics:
- What a metric actually is — counters, gauges, histograms, summaries — with concrete FinVerse examples for each
- Java anchor: Spring Boot Actuator `/metrics` endpoint, Micrometer as the facade, `micrometer-registry-datadog` push model vs Prometheus pull model
- Node.js equivalent: how NestJS exposes metrics — `@willsoto/nestjs-prometheus` or direct Datadog StatsD client — the honest production picture
- The metrics that actually matter in production: HTTP request rate, error rate, latency (p50/p95/p99), queue depth, DB connection pool, memory/CPU
- Custom business metrics at FinVerse: jobs processed per minute, sync success rate, GoCardless error rate, budget alerts fired
- How Datadog dashboards are built on top of these metrics
- **The interview answer**: "how did you measure that 60% improvement" — walking through exactly what metrics you'd look at for a latency improvement in the transaction sync flow

---

## Chapter 3 — Distributed Tracing: Following a Request Across Services

**Goal:** Deeply understand distributed tracing — the concept most engineers claim to know but can't explain precisely.

Topics:
- The core problem: a request touches API Gateway → Core Product → Market Data Service (Go) → Redis → PostgreSQL — which one is slow?
- What a Trace is, what a Span is, what Parent Span ID is — no buzzwords, just the mechanics
- Java anchor: your Spring Boot notes on Micrometer + OpenTelemetry bridge + OTLP exporter + Jaeger — you already have this mental model
- The W3C `traceparent` header — how trace context propagates across service boundaries (HTTP header, exactly like the Correlation ID filter you know)
- Node.js/NestJS equivalent: `@opentelemetry/sdk-node`, auto-instrumentation for HTTP, NestJS interceptors, Prisma tracing
- How the Go Market Data Service participates in the same trace — OpenTelemetry is language-agnostic
- Sampling — why you don't trace 100% of requests in production and how to decide the right rate
- **The FinVerse trace**: a complete end-to-end trace for `POST /v1/accounts/connect` — from mobile app hit to BullMQ job enqueue — every span visible in Datadog APM

---

## Chapter 4 — Logging: The Detail Layer

**Goal:** Understand production-grade structured logging in NestJS — mapped to your existing Logback/SLF4J knowledge.

Topics:
- Java anchor: SLF4J → Logback → LogstashEncoder → structured JSON → MDC for Correlation ID — you know all of this
- Node.js equivalent: the logging library landscape (Winston, Pino, Bunyan) — which one and why (Pino is the production answer)
- Why Pino over Winston — performance numbers, async transport, JSON by default
- Structured logging: what it means, why plain text logs don't work at scale, how Datadog Log Management ingests and indexes structured logs
- Log levels in production — what goes at DEBUG, INFO, WARN, ERROR and what the team actually ships to production
- The Correlation ID / Trace ID pattern in NestJS: `AsyncLocalStorage` as the Node.js equivalent of Java's `ThreadLocal` + MDC
- Sensitive data: never log PII — GoCardless tokens, IBANs, transaction amounts — how FinVerse enforces this
- **Practical NestJS setup**: complete Pino configuration for Core Product Service with Datadog integration

---

## Chapter 5 — OpenTelemetry: The Glue Layer

**Goal:** Understand what OpenTelemetry actually is, why it exists, and how it connects metrics + traces + logs into one pipeline.

Topics:
- The problem before OpenTelemetry: vendor lock-in — switching from Jaeger to Datadog meant rewriting instrumentation code
- What OpenTelemetry actually is: a vendor-neutral instrumentation standard (not a backend, not a UI, just an API + SDK + Collector)
- The three signals: Traces, Metrics, Logs — all flowing through one OTEL Collector → Datadog exporter
- Java anchor: your notes on `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp` — exact same concept in Node.js
- NestJS auto-instrumentation: what `@opentelemetry/auto-instrumentations-node` captures automatically vs what you must instrument manually
- The OTEL Collector: what it is, why FinVerse runs it as a sidecar vs why some teams skip it and export directly
- Manual span creation in NestJS — when the auto-instrumentation isn't enough (BullMQ job spans, GoCardless API call spans)
- How the Go Market Data Service connects to the same OTEL pipeline — language agnostic, same Collector

---

## Chapter 6 — Alerting & Datadog: Turning Signals Into Action

**Goal:** Understand how raw metrics and logs become actionable alerts — the "how do you know something is wrong before users tell you" answer.

Topics:
- The on-call engineer's toolkit: Datadog APM, Log Explorer, Dashboard, Monitor
- What a Datadog Monitor is — threshold alerts, anomaly detection, composite monitors
- FinVerse's critical alerts: sync failure rate, queue depth spike, GoCardless error rate, payment failure rate, Redis memory pressure
- SLOs (Service Level Objectives) — what they are, how FinVerse defines them for Core Product (99.5% of syncs complete within 60 seconds)
- Alert fatigue — why you don't alert on everything and how the team manages signal vs noise
- The on-call runbook pattern: when alert X fires, you do Y — specific examples from BullMQ Chapter 7's incident response flow
- **The interview answer**: "you said you improved sync reliability — how did you know it improved?" — walking through exactly which Datadog monitors changed before vs after

---

## Chapter 7 — Observability at FinVerse: End-to-End Setup

**Goal:** Put everything together into one coherent picture of how observability actually works in Core Product Service.

Topics:
- Complete NestJS setup: OpenTelemetry SDK initialisation (must happen before any imports — the common gotcha), Pino logger, Datadog exporter
- The request lifecycle with full observability: incoming HTTP → Trace started → logs attached to trace → DB queries as child spans → BullMQ job as async span → response
- AsyncLocalStorage for Correlation ID propagation across async operations — why `req.id` doesn't work in async contexts and what does
- BullMQ worker observability: how a BullMQ job span connects to the HTTP span that enqueued it — the parent-child span relationship across async boundaries
- The complete Datadog view: APM trace → correlated logs → infrastructure metrics — all linked by Trace ID
- **The "improved performance by X%" story**: a concrete STAR-format walkthrough of identifying, measuring, fixing, and verifying a real performance problem in Core Product Service using Datadog

---

## Execution Plan

Chapter by chapter, in order. Each chapter complete before moving to the next.

**Suggested order:** Chapters 1–3 build the mental model (problem space, metrics, traces). Chapter 4 covers logging. Chapter 5 covers OpenTelemetry as the connective layer. Chapter 6 covers alerting. Chapter 7 applies everything to FinVerse end-to-end.

---
