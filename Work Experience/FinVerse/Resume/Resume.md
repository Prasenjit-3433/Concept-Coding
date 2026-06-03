**Node.js Backend Engineer** | FinVerse (EU Fintech, via Turing.com) | Aug 2023 – Aug 2024 | Remote

- Reduced transaction sync p95 latency by 61% from 28.4s to 11.2s by refactoring sequential GoCardless API calls to concurrent async processing with BullMQ lock tuning, verified via Datadog APM histograms
- Eliminated a 12% stall rate for multi-account users by implementing per-account timeouts using Promise.race and extending BullMQ lock duration, dropping stalled jobs from 34 per day to near zero
- Resolved a live 8,000-job GoCardless rate-limit storm by adding worker-level rate limiting and exponential backoff with jitter, draining the full backlog at under 0.1% error rate
- Built the end-to-end consent expiry notification pipeline using BullMQ repeatable jobs, Redis deduplication, and the Outbox pattern, fixing a race condition where notifications arrived before transactions appeared in-app
- Owned the Accounts and Open Banking module delivering 8 REST endpoints for PSD2 bank connections and sync orchestration across 450K users in 8 EU markets via GoCardless integration