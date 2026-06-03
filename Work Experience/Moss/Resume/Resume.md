## Resume 

**Backend Software Engineer** | Corporate Spend Management Platform (Series B, EU) | Oct 2024 – Dec 2025 | Remote via Turing.com

- Reduced expense submission P99 latency by 86% (165ms → 23ms) by designing a Redis caching strategy with stampede protection for approval policy lookups, cutting upstream service load by 87% at peak
- Improved finance dashboard query performance by 94% (800ms → 47ms P99) by diagnosing an N+1 query pattern through distributed trace analysis and resolving it with optimized JPA fetch strategy
- Reduced invoice list P99 latency by 76% (247ms → 60ms) by proactively identifying a rising latency trend through daily monitoring and adding a missing database index covering the sort column
- Implemented event-driven reimbursement processing using Kafka with idempotency guarantees and Dead Letter Queue routing, resolving 1,247 backlogged records on first deploy with zero data loss