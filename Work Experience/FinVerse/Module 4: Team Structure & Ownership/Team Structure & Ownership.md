You're completely right. I asked the questions *after* writing the whole thing — same mistake as Step 2. That's on me.

Let me ask everything I need **before** writing a single line this time.User's selections:
- Seniority: Junior-Mid SDE
- Module ownership: Accounts & Open Banking
- Location: India (remote via Turing, IST timezone)

Good. Everything I need. Writing Step 4 now — fresh, clean, questions-first.

---

# FinVerse — Deep Dive: Team Structure & Ownership

## 1. Company-Wide Engineering Organisation

```
CEO
 │
 ├── CTO
 │    │
 │    ├── VP of Engineering
 │    │    │
 │    │    ├── Core Product Team        (6 engineers + 1 Tech Lead)
 │    │    ├── Payment Team             (3 engineers)
 │    │    ├── Notifications Team       (2 engineers)
 │    │    ├── Market Data Team         (3 engineers)
 │    │    └── Platform & DevOps        (2 engineers)
 │    │
 │    └── VP of Product
 │         ├── Product Managers (3)
 │         └── UX Designers (3)
 │
 ├── Head of Mobile                     (4 React Native engineers)
 ├── Head of Compliance & Regulatory
 ├── Head of Finance
 └── Head of Operations
```

**Engineering headcount breakdown:**

| Function | Headcount |
|---|---|
| Backend engineering (all teams) | 16 |
| Mobile engineering (React Native) | 4 |
| Frontend web (React dashboard) | 3 |
| QA engineering | 3 |
| Platform & DevOps | 2 |
| Data & Analytics | 2 |
| Engineering management | 3 |
| **Total engineering & product** | **~33** |

The remaining ~52 people across the 85-person company are in operations, customer support, compliance, legal, finance, and business/marketing.

---

## 2. Engineering Leadership

**CTO — Markus Bauer** *(German, Berlin)*
Co-founder. Previously Staff Engineer at N26. Sets overall architecture direction, owns major technical decisions — service boundaries, database choices, observability stack. Still reviews critical PRs occasionally. Has final say on anything that crosses team boundaries or involves significant infrastructure change.

**VP of Engineering — Sophie Renard** *(French, Paris)*
Joined 8 months before you. Previously Engineering Manager at Revolut. Owns the engineering org — team structure, hiring, cross-team coordination, sprint health. Technically sharp but does not write production code. Your team's Tech Lead reports to her. She runs a monthly all-engineering meeting and a weekly sync with all Tech Leads.

---

## 3. Team Ownership Map

| Team | Service Owned | Primary Tech |
|---|---|---|
| **Core Product Team** | Core Product Service — all domain modules | NestJS, PostgreSQL, Prisma, Redis, BullMQ, RabbitMQ (producer) |
| **Payment Team** | Payment Service — Stripe, billing, investment orders, ledger | NestJS, PostgreSQL, Prisma, Stripe, BullMQ |
| **Notifications Team** | Notification Service — push, email, SMS, preferences | NestJS, PostgreSQL, Redis, RabbitMQ (consumer) |
| **Market Data Team** | Market Data Service — ETF pricing, portfolio valuation | Go, Redis, PostgreSQL (read-only) |
| **Platform & DevOps** | AWS infrastructure, CI/CD pipelines, Datadog, IaC | AWS, Docker, GitHub Actions, Terraform, Datadog |

**Cross-team contract rule:** When Core Product needs data from another service, it calls that service's documented internal REST API. No direct cross-schema database queries. No touching another team's Redis keys directly. These contracts are versioned and treated with the same discipline as external APIs.

---

## 4. Core Product Team — Full Deep Dive

### 4.1 Team Roster

**Team size:** 6 engineers + 1 Tech Lead + 1 Engineering Manager (shared with Payment Team)

---

**Lucas Hoffmann — Tech Lead** *(German, Munich — remote)*
*8 years experience. Senior–Staff level.*

One of FinVerse's earliest engineering hires — joined 14 months before you. Previously Senior Engineer at Commerzbank's digital unit, then at a Berlin fintech. Designed the Core Product Service's modular monolith architecture. Owns the overall technical direction of the service. Reviews all non-trivial PRs and sets the coding standards the team follows. Deep expertise in NestJS, PostgreSQL, financial domain modelling, and system design. Approachable but precise in reviews — he consistently asks "why" before accepting "what." Your primary technical anchor during onboarding and throughout the contract.

---

**Elena Vasquez — Senior Backend Engineer** *(Spanish, Madrid — remote)*
*6 years experience.*

Owns the Investing and Retirement Planning modules. Previously at a Spanish robo-advisor startup — brings deep domain knowledge of ETF mechanics, portfolio rebalancing, and MiFID II compliance requirements. The team's bridge between financial domain expertise and engineering. If anything touches investment logic or regulatory compliance, she is involved. Thorough in code review, especially around financial calculation correctness. Joined FinVerse 11 months before you.

---

**Tomasz Wiśniewski — Mid-Level Backend Engineer** *(Polish, Warsaw — remote)*
*4 years experience.*

Owns the Transactions, Categorisation, and Budgeting modules. Previously worked on Open Banking integrations at a Polish fintech — which makes him the team's deepest expert on GoCardless, PSD2 edge cases, and transaction data quality issues. Joined 10 months before you. Your primary day-to-day collaborator — your Accounts & Open Banking module feeds directly into his Transactions module, so you work together constantly. Pragmatic, approachable, fast to respond on Slack.

---

**Isabelle Moreau — Mid-Level Backend Engineer** *(French, Lyon — remote)*
*3.5 years experience.*

Owns the Tax & Reporting module and the Education Hub. Background at a French accounting software company — brings strong familiarity with EU tax regimes across multiple jurisdictions. The Tax module is the most country-specific, complex module in the service — 8 countries, 8 different tax rules. Joined 6 months before you. Pairs with you occasionally when savings goal data needs to feed into tax calculations.

---

**Arjun Mehta — Junior-Mid Backend Engineer** *(Indian, Amsterdam — remote via Turing)*
*2 years experience. Joined 4 months before you.*

Owns the Goals & Savings module. Self-taught background similar to yours — joined via a contractor channel, strong NestJS knowledge. Your closest peer on the team — similar seniority, similar background, you review each other's PRs regularly and often debug together on Slack. Seeing how he navigates technical and process challenges gives you useful reference points during your own ramp-up.

---

**You — Junior-Mid Backend Engineer** *(India — remote via Turing.com)*
*Joined August 2023.*

Primary ownership: **Accounts & Open Banking module** (from month 2 onwards). First professional engineering role. Ownership and contribution arc detailed in Section 6.

---

**Priya Nair — Junior Backend Engineer** *(Indian, Bangalore — remote via Turing)*
*1.5 years experience. Joined 2 months after you.*

Works on the Education Hub and supporting tasks across Goals. The most junior engineer on the team. By month 8–9 of your contract, you naturally become an informal peer mentor to her — reviewing her PRs, answering questions on Slack, explaining patterns you learned from Lucas. This is the first time you are on the giving end of mentorship, which is part of your growth story.

---

**Daniel Brandt — Engineering Manager** *(German, Hamburg — remote)*
*Shared EM across Core Product and Payment Teams.*

Manages people, not code. Handles 1:1s, performance feedback, sprint health, cross-team dependencies, and contractor relations. Does not do code review. Your direct manager for all people and process matters. Holds monthly 1:1s with every engineer including contractors. Champions giving contractors real ownership — he is the one who advocates for assigning you the Accounts & Open Banking module after your first month.

---

### 4.2 Module Ownership Map

| Module | Owner |
|---|---|
| Users & Auth | Lucas (Tech Lead) |
| **Accounts & Open Banking** | **You** (from month 2) |
| Transactions & Categorisation | Tomasz |
| Budgeting | Tomasz |
| Goals & Savings | Arjun |
| Investing | Elena |
| Retirement Planning | Elena |
| Tax & Reporting | Isabelle |
| Education Hub | Isabelle + Priya (supporting) |

---

## 5. Team Working Style & Processes

### 5.1 Sprint Structure — 2-Week Sprints

| Day & Time (CET) | Event |
|---|---|
| Monday Week 1 — 10:00 | Sprint Planning (60–90 min) |
| Wednesday Week 1 — 10:00 | Mid-sprint sync (15 min) |
| Monday Week 2 — 10:00 | Mid-sprint standup (15 min) |
| Thursday Week 2 — 10:00 | Sprint Review — demo to PM and stakeholders (30 min) |
| Thursday Week 2 — 11:00 | Sprint Retrospective — team only (45 min) |
| Friday Week 2 | Buffer day — reviews, documentation, tech debt only. No new tickets. |

**Sprint Planning process:**
Product Manager Céline (French, Paris) brings a prioritised backlog into planning. Lucas reviews technical feasibility and flags dependency risks. The team estimates using story points — Fibonacci scale (1, 2, 3, 5, 8). Any ticket above 8 points is broken down before it enters the sprint. Engineers pick up tickets aligned to their module ownership. Cross-module work is explicitly discussed and assigned during planning — it does not get discovered mid-sprint.

**Ticket lifecycle:**
```
Backlog → Refined → To Do → In Progress → In Review → QA → Done
```

---

### 5.2 Your Daily Workflow

FinVerse core hours are 10:00–18:00 CET. From India (IST = CET + 4:30h), this maps to 14:30–22:30 IST. As a Turing contractor you maintain a minimum 4-hour overlap with European core hours.

```
13:00 IST — Start day
             Check Slack — European colleagues have been
             working since ~09:00 CET (13:30 IST)

13:30 IST — Async standup post in #core-product-team Slack:
             "Yesterday: [completed / in progress]
              Today: [planned work]
              Blockers: [anything blocking]"
             No daily video standup — team runs async by default

14:00 IST — Deep work block
             Primary coding, feature implementation,
             writing tests, updating Prisma migrations

17:30 IST — European afternoon overlap begins
             (13:00–18:00 CET = 17:30–22:30 IST)
             Most active Slack period — PR reviews,
             comments, design discussions, pair debugging

19:00 IST — Sprint ceremonies when scheduled
             (Planning, Review, Retro all happen in this window)

21:00 IST — Wind down
             Address final PR review comments,
             update ticket status in Linear,
             write any async notes for next day

22:00 IST — End of day
```

---

### 5.3 Code Review & PR Process

**Hard rules:**
- Every PR requires minimum 1 approval before merge
- PRs touching financial logic, database migrations, or auth flows require Lucas's approval specifically
- PR description must cover: what changed, why it changed, how to test locally, any migration steps required
- Target PR size: under 400 lines of diff. Larger PRs are flagged and asked to be split
- No merges on Friday afternoons — unwritten rule, universally followed, avoids weekend incidents

**Review culture in practice:**
Lucas's reviews are thorough and educational — especially in your first months. He does not just flag what is wrong, he explains the underlying reason. Tomasz reviews pragmatically and quickly. You and Arjun review each other's PRs freely — peer reviews count toward the 1-approval minimum.

**Branch strategy:**
```
main (production)
 └── develop (integration branch)
       └── feature/FINV-{ticketId}-{short-description}
       └── fix/FINV-{ticketId}-{short-description}
       └── chore/FINV-{ticketId}-{short-description}
```

Feature branches cut from `develop`. PRs merge to `develop`. `develop` merges to `main` on weekly release — every Thursday after Sprint Review.

---

### 5.4 Communication Channels

| Channel | Tool | Purpose |
|---|---|---|
| Day-to-day team communication | Slack | Primary async channel |
| Sprint ceremonies | Google Meet | Planning, Review, Retro |
| Pair debugging, design discussions | Google Meet (ad hoc) | When Slack back-and-forth gets inefficient |
| Tickets & sprint board | Linear | Ticket management, sprint tracking |
| Documentation & runbooks | Notion | Architecture docs, ADRs, onboarding guides |
| Code review & version control | GitHub | PRs, branches, code history |
| Production alerts | Datadog → Slack (`#incidents`) | Real-time incident notifications |
| Deployment notifications | GitHub Actions → Slack (`#deployments`) | Automated deploy status |

**Key Slack channels for you:**
- `#core-product-team` — your team's primary channel
- `#engineering-all` — company-wide engineering announcements
- `#incidents` — production issues, real-time coordination
- `#deployments` — automated CI/CD notifications

---

### 5.5 Your Onboarding — First Month (August 2023)

**Week 1 — Orientation and environment setup:**
Lucas runs a 2-hour recorded walkthrough of the Core Product Service codebase — module structure, folder conventions, how Prisma schema is organised, how BullMQ workers are registered, how RabbitMQ producers are wired up. You set up local dev using Docker Compose — PostgreSQL, Redis, and RabbitMQ all run locally. Core Product Service runs against this local stack.

You spend time reading Architecture Decision Records in Notion — particularly why the modular monolith was chosen over separate microservices, why Prisma over TypeORM, and why BullMQ sits alongside RabbitMQ.

First ticket: a small bug fix in the Budgeting module — intentionally scoped to walk you through making a real code change, writing a PR description, going through review, and merging. End-to-end process familiarity.

**Week 2 — First real contribution:**
Assigned a small feature in the Transactions module under Tomasz's guidance — adding a new merchant categorisation rule for a category being consistently miscategorised. Tomasz walks you through the categorisation engine in 30 minutes, you implement independently. PR reviewed by both Tomasz and Lucas — several comments, one round of changes, merged by end of week.

**Week 3–4 — Module handover:**
Daniel and Lucas jointly decide to give you ownership of the Accounts & Open Banking module. Elena, who had been informally maintaining it alongside her Investing work, runs a dedicated handover session — covering the GoCardless integration in detail, the requisition and consent flow, how BullMQ sync jobs are structured, and known edge cases: banks that return malformed transaction data, GoCardless rate limits, accounts that fail to reconnect after token expiry.

You shadow Elena on the module for one week before taking full ownership.

**End of month 1:**
You own the Accounts & Open Banking module. Four PRs merged. Comfortable enough with the codebase to work independently on your module, with Lucas available for design questions.

---

### 5.6 Mentorship & Growth Arc

**Month 1–3 — High guidance:**
Lucas reviews all your PRs in detail. Comments are educational — not just "fix this" but "here is why this pattern is problematic." You ask questions freely, Tomasz is your main collaborative point for day-to-day questions since your module feeds into his.

**Month 4–6 — Growing independence:**
You start making module-level design decisions. When a new edge case in GoCardless's API response requires a schema change or a new sync strategy, you draft the approach and bring it to Lucas for review rather than asking for direction. Lucas approves with minor adjustments. You start contributing PR reviews to Arjun's Goals module work.

**Month 7–9 — Peer-level on your module:**
Lucas trusts your judgment on Accounts & Open Banking decisions. You are pulled into broader design discussions — not just your module. You contribute meaningfully to a cross-team discussion about how transaction sync timing affects the Notification Service's budget alert accuracy.

**Month 10–12 — Giving back:**
Priya joins and you naturally become her first point of contact for questions. You review her PRs, explain patterns, catch the same kinds of mistakes Lucas caught in yours early on. In your final 1:1 with Daniel, the feedback is: *"solid growth trajectory — production-ready engineer by end of contract."*

---

### 5.7 Decision Making & Hierarchy

| Decision Type | Who Decides |
|---|---|
| How to implement a feature within your module | You |
| Schema changes or new patterns within your module | You — Lucas reviews the PR |
| Cross-module API contracts or shared utility changes | You + Lucas + affected module owner |
| Cross-service API contracts | Lucas + relevant Tech Lead from other team |
| New service or major architectural change | Lucas + CTO Markus |
| Infrastructure changes | Platform & DevOps team |

The practical rule: if the blast radius of a decision is contained within your module, it is yours to make. The moment it touches another module's data or another service's API contract, Lucas is in the loop before implementation starts — not after.

---

## 6. Your Ownership & Contribution Arc — 12 Months

| Period | Focus | Nature of Work |
|---|---|---|
| Month 1 | Onboarding, Transactions support | Bug fixes, small features, learning codebase and team process |
| Month 2–3 | Accounts & Open Banking — taking ownership | Stabilising GoCardless integration, fixing known edge cases, improving sync reliability |
| Month 4–6 | Accounts & Open Banking — deep ownership | New features: multi-account improvements, error handling, sync performance |
| Month 7–9 | Accounts & Open Banking + cross-module contributions | Contributing to Goals and Budgeting work, cross-team design discussions |
| Month 10–12 | Accounts & Open Banking + informal mentorship | Mentoring Priya, Series B prep features, performance and observability improvements |

---

Step 4 is written clean.

**Ready for Step 5: Database Selection & Schema Design?**