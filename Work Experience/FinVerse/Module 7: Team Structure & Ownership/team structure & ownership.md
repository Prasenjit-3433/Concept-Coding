# FinVerse Engineering Organization Structure

## Overall Company Size (Mid 2023)

**Total Company:** ~85 people

- Engineering & Product: ~25 people
- Operations & Support: ~15 people
- Marketing & Growth: ~12 people
- Compliance & Legal: ~8 people
- Finance & Admin: ~10 people
- Leadership: ~5 people (CEO, CTO, CPO, CFO, COO)

---

## Engineering Department Structure

**Total Engineering Team: 18-22 people**

```
┌─────────────────────────────────────────────────────────────────┐
│                         CTO (Chief Technology Officer)          │
│                         • 15+ YOE                               │
│                         • Ex-N26, Klarna background             │
│                         • Manages all engineering               │
└─────────────────────────────┬───────────────────────────────────┘
                              │
          ┌──────────────┬──────────────┬────────────────┐
          │              │              │                │
          ▼              ▼              ▼                ▼
┌──────────────────┐ ┌──────────┐ ┌──────────────┐ ┌──────────────┐
│ Backend Team     │ │ Mobile   │ │ DevOps/      │ │ QA/Testing   │
│ (8-10 people)    │ │ Team     │ │ Infra Team   │ │ Team         │
│                  │ │ (4-5)    │ │ (1-2)        │ │ (2-3)        │
└──────────────────┘ └──────────┘ └──────────────┘ └──────────────┘

```

---

## Backend Team Deep Dive (Where You Work)

### **Team Size: 8-10 Backend Engineers**

**Reporting Structure:**

```
                    Engineering Manager (Backend)
                    • 10 YOE, Ex-Revolut
                    • Technical + People management
                              │
        ┌─────────────────────┼─────────────────────┬────────────────┐
        │                     │                     │                │
   Tech Lead            Team Lead            Senior Engineers    Mid/Junior Engineers
   (1 person)           (1 person)           (3-4 people)        (3-4 people)
   • 8-10 YOE           • 6-8 YOE             • 4-6 YOE           • 0-3 YOE
   • Architecture       • Feature delivery    • Owns services     • Learn & contribute
   • Code reviews       • Mentoring           • Mentors juniors   • Pair programming

```

---

## Team Composition & Background

### **1. Engineering Manager - Sarah (40, German)**

- **Experience**: 10 YOE, previously at Revolut (fintech), Amazon (payments team)
- **Role**:
    - Manages backend team (hiring, performance reviews, career development)
    - Technical decisions (architecture, tech stack choices)
    - Cross-team coordination (works with Mobile, DevOps, Product)
    - Shields team from organizational noise
- **Not coding much**: ~20% coding (code reviews, critical bugs), 80% management
- **Your interaction**: 1-on-1 meetings every 2 weeks, provides career guidance

---

### **2. Tech Lead - Marcus (35, Swedish)**

- **Experience**: 8 YOE, previously at Klarna (backend), Spotify (payments)
- **Role**:
    - System architecture design (draws diagrams, makes tech decisions)
    - Code review for critical features (database migrations, new services)
    - Performance optimization (identifies bottlenecks, scales systems)
    - Technical mentorship (teaches design patterns, best practices)
- **Coding**: ~60% coding, 40% architecture/reviews
- **Your interaction**:
    - Reviews your PRs (pull requests)
    - Pair programming on complex features
    - Explains architecture decisions (why microservices, why Go for Investment Engine)

---

### **3. Team Lead - Anna (32, French)**

- **Experience**: 6 YOE, previously at N26 (fintech backend), BlaBlaCar
- **Role**:
    - Sprint planning (breaks down features into tasks)
    - Daily standups facilitator
    - Unblocks team members (answers questions, provides context)
    - Owns Core API service (main NestJS monolith)
    - Mentors mid-level and junior engineers
- **Coding**: ~70% coding, 30% coordination
- **Your interaction**:
    - Daily standup (you report to her)
    - Assigns you tasks from sprint backlog
    - First point of contact for questions
    - Regular 1-on-1s for technical growth

---

### **4. Senior Backend Engineer - Dmitri (30, Russian)**

- **Experience**: 5 YOE, previously at Yandex (payments), small fintech startup
- **Role**:
    - Owns Transaction Service (Go) - money movement, ledger
    - Expert in financial systems, double-entry bookkeeping
    - Database expert (PostgreSQL optimization, query tuning)
    - On-call rotation leader (handles production incidents)
- **Coding**: ~85% coding, 15% mentoring
- **Your interaction**:
    - You work closely when features touch Transaction Service
    - He reviews your database-heavy code
    - Teaches you about financial transactions, ACID guarantees

---

### **5. Senior Backend Engineer - Liam (29, Irish)**

- **Experience**: 4 YOE, previously at Stripe (API team), fintech startup
- **Role**:
    - Owns Investment Engine (Go) - portfolio calculations, rebalancing
    - Strong in algorithms, mathematical computations
    - API design expert (RESTful principles, versioning)
    - Owns integration with external broker APIs
- **Coding**: ~80% coding, 20% mentoring
- **Your interaction**:
    - Collaborates when Core API needs to call Investment Engine
    - Reviews your API endpoint designs
    - Pair programs on complex business logic

---

### **6. Mid-Level Engineer - Sofia (27, Spanish)**

- **Experience**: 3 YOE, previously at small startup (backend generalist)
- **Role**:
    - Works on Core API (NestJS) - budgets, goals, education modules
    - Shares ownership of Notification Service (NestJS)
    - Writes a lot of CRUD endpoints, business logic
    - Active code reviewer (reviews your PRs)
- **Coding**: ~90% coding, 10% mentoring
- **Your interaction**:
    - You work on same codebase (Core API)
    - Pair programming on features
    - She asks you for help too (collaborative relationship)
    - You both attend NestJS guild meetings

---

### **7. Mid-Level Engineer - Raj (26, Indian, via Turing)**

- **Experience**: 2.5 YOE, previously at Turing project (edtech platform)
- **Role**:
    - Works on Core API (NestJS) - authentication, user management, subscriptions
    - Strong in TypeScript, familiar with Stripe integration
    - Good at writing tests (Jest, unit tests)
- **Coding**: ~90% coding
- **Your interaction**:
    - Fellow Turing contractor (you both relate to remote work challenges)
    - Collaborate on Core API features
    - Share knowledge about NestJS patterns

---

### **8. Junior Engineer - Emma (24, German)**

- **Experience**: 1 YOE, fresh bootcamp graduate (6-month intensive program)
- **Role**:
    - Works on simpler Core API tasks (bug fixes, small features)
    - Learning NestJS, PostgreSQL, REST APIs
    - Pair programs heavily (learns from seniors)
    - Writes lots of tests (good learning exercise)
- **Coding**: ~95% coding, 5% learning
- **Your interaction**:
    - You mentor her occasionally (teaches you to explain concepts)
    - You both pair on features sometimes
    - She asks you NestJS questions (helps solidify your knowledge)

---

### **9. YOU - Prasenjit (24, Indian, via Turing) - Contract Engineer**

- **Experience**: 0 YOE professionally, ~1 year self-learning + projects
- **Background**:
    - Completed Andrei Neagoie's React & Node.js ZTM courses
    - Built Bookify.IO (NestJS microservices project)
    - Learned DSA (Striver SDE sheet in Java)
    - Basic understanding of HLD/LLD
    - Strong fundamentals in JavaScript, TypeScript, Node.js internals
- **Initial Level**: Junior engineer (but with solid theoretical foundation)
- **Role in first 6 months (Jun-Dec 2023)**:
    - Bug fixes in Core API (NestJS)
    - Small feature additions (new API endpoints)
    - Pair programming with Sofia, Raj, Anna
    - Learning codebase, understanding system architecture
    - Writing tests, improving code coverage
- **Role in next 9 months (Jan-Aug 2024)**:
    - Independently own features (budget module, goals module)
    - Collaborate across services (Core API calls Investment Engine)
    - Participate in architecture discussions
    - Mentor Emma (newer engineer)
    - Handle on-call rotation (production support)

---

## Service Ownership Map

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         Backend Services                                  │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌───────────────────────────────────────────────────────────────────┐    │
│  │ Core API (NestJS Monolith)                                        │    │
│  │ Primary Owner: Anna (Team Lead)                                   │    │
│  │ Contributors: Sofia, Raj, Prasenjit, Emma                         │    │
│  │                                                                   │    │
│  │ Modules:                                                          │    │
│  │ • Authentication & Users ────────────── Raj (primary)             │    │
│  │ • Bank Accounts & Transactions ──────── Sofia (primary)           │    │
│  │ • Budgets ───────────────────────────── ***Prasenjit*** (primary) │    │
│  │ • Savings Goals ─────────────────────── ***Prasenjit*** (primary) │    │
│  │ • Education & Courses ───────────────── Sofia (primary)           │    │
│  │ • Subscriptions & Payments ──────────── Raj (primary)             │    │
│  │ • Dashboard & Analytics ─────────────── Anna (primary)            │    │
│  │                                                                   │    │
│  │ Everyone contributes to all modules, but has "primary"            │    │
│  │ ownership (main point of contact for that domain)                 │    │
│  └───────────────────────────────────────────────────────────────────┘    │
│                                                                           │
│  ┌───────────────────────────────────────────────────────────────────┐    │
│  │ Investment Engine (Go)                                            │    │
│  │ Owner: Liam (Senior Engineer)                                     │    │
│  │ Backup: Marcus (Tech Lead)                                        │    │
│  │                                                                   │    │
│  │ Responsibilities:                                                 │    │
│  │ • Portfolio allocation calculations                               │    │
│  │ • Rebalancing logic                                               │    │
│  │ • Performance metrics                                             │    │
│  │ • Tax optimization                                                │    │
│  │                                                                   │    │
│  │ NestJS team rarely touches this (different language)              │    │
│  └───────────────────────────────────────────────────────────────────┘    │
│                                                                           │
│  ┌───────────────────────────────────────────────────────────────────┐    │
│  │ Transaction Service (Go)                                          │    │
│  │ Owner: Dmitri (Senior Engineer)                                   │    │
│  │ Backup: Marcus (Tech Lead)                                        │    │
│  │                                                                   │    │
│  │ Responsibilities:                                                 │    │
│  │ • Order execution                                                 │    │
│  │ • Ledger management                                               │    │
│  │ • Money transfers                                                 │    │
│  │ • Broker API integration                                          │    │
│  │                                                                   │    │
│  │ Critical service - only seniors touch                             │    │
│  └───────────────────────────────────────────────────────────────────┘    │
│                                                                           │
│  ┌───────────────────────────────────────────────────────────────────┐    │
│  │ Notification Service (NestJS)                                     │    │
│  │ Primary Owner: Sofia (Mid-Level)                                  │    │
│  │ Contributors: ***Prasenjit***, Raj                                │    │
│  │                                                                   │    │
│  │ Responsibilities:                                                 │    │
│  │ • RabbitMQ consumers                                              │    │
│  │ • Template rendering                                              │    │
│  │ • Email/Push/SMS delivery                                         │    │
│  │ • BullMQ workers                                                  │    │
│  │                                                                   │    │
│  │ Good service for juniors (less critical than money services)      │    │
│  └───────────────────────────────────────────────────────────────────┘    │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘

```

---

## Mobile Team (Separate Team)

**Team Size: 4-5 Mobile Engineers**

```
Mobile Engineering Manager - Laura (8 YOE)
├─ Senior iOS Engineer - Tom (6 YOE)
├─ Senior Android Engineer - Maria (5 YOE)
├─ React Native Engineer - Alex (3 YOE)
└─ Junior Mobile Engineer - Chris (1 YOE)

```

**Their responsibility:**

- Build React Native app (main user interface)
- Consume backend APIs (your endpoints!)
- Handle offline functionality, push notifications
- App store submissions

**Your interaction with Mobile team:**

- Weekly sync meeting (backend + mobile)
- You present new API endpoints (explain request/response format)
- They give feedback ("Can you add a field for X?")
- Slack channel: #backend-mobile-sync

---

## DevOps/Infrastructure Team

**Team Size: 1-2 Engineers**

```
DevOps Lead - Klaus (German, 7 YOE)
└─ Junior DevOps Engineer - Miguel (Spanish, 1.5 YOE)

```

**Their responsibility:**

- AWS infrastructure (ECS, RDS, ElastiCache, etc.)
- CI/CD pipelines (GitHub Actions)
- Monitoring (DataDog, CloudWatch)
- Database management (backups, migrations)
- On-call escalation (when backend can't fix production issue)

**Your interaction:**

- They deploy your code to production
- You create deployment tickets in Jira
- They help when you need Redis/PostgreSQL access
- Pair on infrastructure changes (e.g., adding new queue)

---

## QA/Testing Team

**Team Size: 2-3 QA Engineers**

```
QA Lead - Ines (Portuguese, 5 YOE)
├─ QA Engineer - Fatima (Moroccan, 2 YOE)
└─ QA Engineer - David (German, 1 YOE)

```

**Their responsibility:**

- Manual testing (exploratory, regression)
- Automated E2E tests (Playwright)
- API testing (Postman, automated scripts)
- Bug reporting (Jira tickets assigned to you)

**Your interaction:**

- They test your features before production
- You fix bugs they find
- You write API documentation for them to test

---

## Cross-Functional Collaboration

### **Product Team**

```
Chief Product Officer (CPO) - Olivia (12 YOE, Ex-PayPal)
├─ Product Manager (Core Features) - Ben (5 YOE)
├─ Product Manager (Investments) - Carla (4 YOE)
└─ Product Designer - Nina (3 YOE)

```

**Your interaction:**

- Sprint planning meetings (they explain what to build, you estimate effort)
- Refinement sessions (clarify requirements before coding)
- Demo days (you show finished features)
- Slack questions ("Is this API endpoint ready?")

---

## Team Working Style & Processes

### **Daily Workflow**

```
09:00 - 09:15   Daily Standup (entire backend team)
                • What you did yesterday
                • What you'll do today
                • Any blockers?

09:15 - 12:00   Deep work (coding)
                • Work on assigned Jira tickets
                • Pair programming sessions
                • Code reviews

12:00 - 13:00   Lunch break

13:00 - 15:00   Collaboration time
                • Team meetings
                • Cross-team syncs
                • Architecture discussions

15:00 - 18:00   Deep work (coding)
                • Finish tickets
                • Write tests
                • Documentation

18:00+          Flexible (remote work, some stay online for India timezone)

```

---

### **Sprint Structure (2-week sprints)**

```
┌───────────────────────────────────────────────────────────────────┐
│                     2-Week Sprint Cycle                           │
├───────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Monday Week 1:                                                   │
│  ├─ 10:00-12:00: Sprint Planning                                  │
│  │   • Review backlog                                             │
│  │   • Estimate story points                                      │
│  │   • Assign tasks to team members                               │
│  │   • You get 2-3 tickets (20-30 story points total)             │
│  │                                                                │
│  └─ 14:00-15:00: Refinement (for next sprint)                     │
│                                                                   │
│  Daily (Week 1 & 2):                                              │
│  └─ 09:00-09:15: Standup                                          │
│                                                                   │
│  Wednesday Week 1:                                                │
│  └─ 15:00-16:00: Tech Sync                                        │
│      • Tech Lead presents architecture decisions                  │
│      • Team discusses technical challenges                        │
│                                                                   │
│  Friday Week 1:                                                   │
│  └─ 16:00-17:00: Demo Day (optional, if features ready)           │
│                                                                   │
│  Friday Week 2:                                                   │
│  ├─ 15:00-16:00: Sprint Demo                                      │
│  │   • Show completed features to stakeholders                    │
│  │   • You present your work!                                     │
│  │                                                                │
│  └─ 16:00-17:00: Retrospective                                    │
│      • What went well?                                            │
│      • What can improve?                                          │
│      • Action items for next sprint                               │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘

```

---

### **Code Review Process**

```
You write code (Feature branch)
    │
    ▼
Create Pull Request (PR) in GitHub
    │
    ├─ Title: "[CORE-123] Add budget alerts endpoint"
    ├─ Description: What changed, why, how to test
    ├─ Screenshots/videos (if UI impact)
    └─ Link to Jira ticket
    │
    ▼
Automated checks run
    ├─ Linting (ESLint)
    ├─ Unit tests (Jest)
    ├─ Integration tests
    └─ Build succeeds
    │
    ▼
Request reviews from:
    ├─ Primary: Sofia or Raj (peer review, NestJS experts)
    ├─ Secondary: Anna (Team Lead, knows entire codebase)
    └─ Optional: Marcus (if architecture change)
    │
    ▼
Reviewers leave comments
    ├─ "Nice work! Just one suggestion..."
    ├─ "Can you extract this into a separate function?"
    └─ "Don't forget to add tests for edge case X"
    │
    ▼
You address feedback (push new commits)
    │
    ▼
Reviewers approve (✅)
    │
    ▼
Merge to main branch
    │
    ▼
CI/CD pipeline deploys to staging
    │
    ▼
QA tests on staging
    │
    ▼
Deploy to production (daily at 14:00 CET)

```

**Review SLA (Service Level Agreement):**

- Peers review within 4 hours (during work day)
- Seniors review within 24 hours
- Critical bugs: Reviewed immediately

---

### **Communication Channels**

**Slack Workspaces:**

```
#engineering-all          - All engineering announcements
#backend-team            - Backend team chat (your main channel)
#backend-core-api        - Specific to Core API discussions
#backend-mobile-sync     - Coordinate with mobile team
#incidents               - Production issues (monitored 24/7)
#deployments             - Deployment notifications
#random                  - Non-work chat, memes, casual

Direct Messages:
- Anna (Team Lead) - Daily questions
- Sofia/Raj - Pair programming coordination
- Marcus - Architecture questions

```

**Meetings:**

```
Recurring:
├─ Daily Standup (15 min, entire backend team)
├─ Sprint Planning (2 hours, every 2 weeks)
├─ Sprint Retrospective (1 hour, every 2 weeks)
├─ Tech Sync (1 hour, weekly)
├─ Backend-Mobile Sync (30 min, weekly)
├─ 1-on-1 with Anna (30 min, bi-weekly)
└─ 1-on-1 with Sarah (EM) (30 min, monthly)

Ad-hoc:
├─ Pair programming sessions (as needed)
├─ Architecture design discussions (when starting new feature)
└─ Incident post-mortems (after production issues)

```

---

### **Onboarding Process (Your First Month)**

```
Week 1: Setup & Orientation
├─ Day 1:
│   ├─ IT setup (laptop, accounts, VPN)
│   ├─ Meet the team (video calls with each person)
│   ├─ Sarah (EM) explains company, mission, your role
│   └─ Marcus gives architecture overview (whiteboard session)
│
├─ Day 2-3:
│   ├─ Clone repositories (Core API, Investment Engine, etc.)
│   ├─ Run application locally (with Anna's help)
│   ├─ Read documentation (Confluence wiki)
│   └─ Watch recorded tech talks (past architecture sessions)
│
└─ Day 4-5:
    ├─ Shadow Anna (watch her work, ask questions)
    ├─ Read code (understand module structure)
    └─ First tiny task: Fix a typo in documentation (get familiar with PR process)

Week 2: First Real Tasks
├─ Assigned 2-3 simple bug fixes ("good first issues")
├─ Pair program with Sofia on one bug
├─ Submit your first real PR
├─ Attend all team meetings (observer mode, learning)
└─ Anna reviews your PRs, provides detailed feedback

Week 3-4: Independent Contributions
├─ Assigned a small feature: "Add endpoint to fetch user's active goals"
├─ Work independently (but ask questions when stuck)
├─ Write tests for your code
├─ Present your work in sprint demo
└─ By end of month: Feeling comfortable with codebase basics

```

---

### **Mentorship & Growth**

**Formal Mentorship:**

- **Primary Mentor: Anna (Team Lead)**
    - Bi-weekly 1-on-1s (30 minutes)
    - Reviews your code with educational comments
    - Assigns progressively challenging tasks
    - Career development discussions

**Informal Mentorship:**

- **Sofia & Raj:** Daily collaboration, pair programming
- **Marcus:** Architecture learning (he loves teaching!)
- **Dmitri:** Database optimization tips
- **Liam:** API design best practices

**Learning Opportunities:**

```
Internal:
├─ Tech Talks (bi-weekly, team members present)
│   • "How PostgreSQL Indexes Work" (Dmitri)
│   • "Designing RESTful APIs" (Liam)
│   • "NestJS Best Practices" (Anna)
│
├─ Code Review Learning (every PR is a lesson)
├─ Pair Programming (learn by doing together)
└─ Architecture Reviews (participate in design discussions)

External:
├─ Conference budget (€500/year for online courses)
├─ Book budget (€200/year for technical books)
└─ Company-paid subscriptions (Frontend Masters, etc.)

```

---

## Hierarchy & Decision Making

```
Strategic Decisions (Tech Stack, Architecture)
    ↓
CTO + Tech Lead (Marcus) decide
    ↓
Present to backend team for feedback
    ↓
Final decision documented in Confluence

Feature Implementation Decisions
    ↓
Team Lead (Anna) + Senior Engineers discuss
    ↓
Agree on approach
    ↓
Assign to appropriate engineer

Day-to-Day Decisions (How to implement a function)
    ↓
Individual engineer decides
    ↓
Code review validates approach
    ↓
If controversial, escalate to Anna or Marcus

```

**Decision-making culture:**

- Junior engineers encouraged to propose solutions (not just execute)
- "Strong opinions, weakly held" (be confident but open to feedback)
- Consensus-driven (not top-down)
- Document decisions in Architecture Decision Records (ADRs)

---

This structure provides you with **strong mentorship, clear ownership, and growth opportunities** while working in a collaborative, supportive team environment. You're not isolated - you're part of a cohesive backend team with experienced engineers who invest in your growth.