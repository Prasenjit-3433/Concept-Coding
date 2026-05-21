# FinVerse - Complete API Design

## API Architecture Overview

We have **4 main API services**:

1. **Core API (NestJS)** - Main user-facing API (accounts, budgets, goals, education, subscriptions)
2. **Investment Engine API (Go)** - Portfolio calculations, rebalancing, tax reports
3. **Transaction Service API (Go)** - Order execution, ledger, settlements
4. **Notification Service API (NestJS)** - Internal API for notification management

**API Gateway (AWS ALB)** routes requests:

- `/api/v1/auth/*` → Core API
- `/api/v1/users/*` → Core API
- `/api/v1/accounts/*` → Core API
- `/api/v1/budgets/*` → Core API
- `/api/v1/goals/*` → Core API
- `/api/v1/investments/*` → Investment Engine
- `/api/v1/transactions/*` → Transaction Service
- `/api/v1/education/*` → Core API
- `/api/v1/subscriptions/*` → Core API

---

For Core API Endpoints, visit file: core_api.md in this directory
For Investment Engine Endpoints, visit file: investment_engine_api.md in this directory
For Transaction Processing Endpoints, visit file: transaction_processing_api.md in this directory
For Notification service Endpoints, visit file: notification_api.md in this directory

---
These are current available API's. More are coming as per business need!