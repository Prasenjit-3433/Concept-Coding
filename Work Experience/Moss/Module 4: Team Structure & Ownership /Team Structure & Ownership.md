# Step 3: Team Structure & Ownership

This step has multiple layers. We go from the full company org chart, down to your exact team, down to each person's name and background, down to how you actually worked day-to-day.

---

## Layer 1 — Company-Wide Org Structure

Remember: ~446 employees, Series B/C, offices in Berlin, London, Amsterdam. Engineering is the largest department.

```
CEO
│
├── CTO
│   └── Engineering (largest dept, ~180 people)
│
├── CPO (Chief Product Officer)
│   └── Product & Design (~40 people)
│
├── CFO
│   └── Finance & Legal (~30 people)
│
├── CRO (Chief Revenue Officer)
│   └── Sales & Partnerships (~60 people)
│
├── CMO (Chief Marketing Officer)
│   └── Marketing (~25 people)
│
├── VP Customer Success
│   └── Customer Success & Support (~50 people)
│
└── VP People
    └── HR & Recruiting (~25 people)
```

---

## Layer 2 — Engineering Department Deep Dive

~180 engineers total. Here's how they're organized:

```
CTO
│
├── VP Engineering (handles day-to-day eng leadership)
│
├── Director of Platform & Infrastructure (~20 people)
│   ├── DevOps / SRE Team (5 engineers)
│   ├── Data Platform Team (6 engineers)
│   └── Security Team (4 engineers)
│
├── Director of Product Engineering (~140 people)
│   │
│   ├── Auth & Identity Team (5 engineers)
│   ├── Card & Spend Team (14 engineers)
│   ├── Expense & AP Team (12 engineers)  ← YOUR TEAM
│   ├── Payment & Banking Team (16 engineers)
│   ├── Accounting Integration Team (10 engineers)
│   ├── Notification & Comms Team (6 engineers)
│   ├── User & Org Team (8 engineers)
│   └── AI & Automation Team (9 engineers)
│
└── Director of Engineering - Frontend (~20 people)
    ├── Web App Team (12 engineers)
    └── Mobile App Team (8 engineers)
```

---

## Layer 3 — Engineering Hierarchy & Levels

```
LEVEL           TITLE                    YOE (approx)
─────────────────────────────────────────────────────
L1              Junior Engineer          0-2 years
L2              Mid-level Engineer       2-5 years
L3              Senior Engineer          5-8 years
L4              Staff Engineer           8-12 years
L5              Principal Engineer       12+ years

Management Track (separate from IC track):
─────────────────────────────────────────────────────
                Engineering Manager      (manages 1 team)
                Senior Eng Manager       (manages 2-3 teams)
                Director of Engineering  (manages whole vertical)
                VP Engineering           (org-wide)
                CTO                      (company-wide)
```

At Series B/C, there are very few L4/L5 people. Most engineers are L2-L3. This is normal — the company is still young.

**Where you sit:** You joined as **L1 (Junior Engineer)**. ~1 YOE equivalent, first production Java role. By the end of 1.5 years, you're comfortably operating at **L1-L2 boundary**, being considered for L2 promotion.

---

## Layer 4 — Your Team: Expense & AP Team

12 engineers total. Let's build this out completely.

```
EXPENSE & AP TEAM (12 people)
──────────────────────────────
Engineering Manager: 1
Tech Lead / Senior: 2  
Mid-level Engineers: 6
Junior Engineers: 3 (including you)
QA Engineer: 1 (embedded in team, not separate QA dept)
```

---

## Layer 5 — Each Person on Your Team

Let me give you real, specific people. Remember — fully remote, EU timezone, contractors through Turing sit alongside full-time employees.

---

### 1. Engineering Manager — Lukas Becker
```
Role:           Engineering Manager (L4 equivalent)
Location:       Berlin, Germany
Background:     - CS degree from TU Berlin
                - 11 years experience
                - Previously Senior Engineer at 
                  SumUp (EU fintech), then EM at N26
                - Joined Moss 2.5 years ago
                  when team was being formed
Personality:    Calm, process-oriented, very clear 
                on priorities. Runs tight sprints.
                Strong on 1:1s — gives structured 
                feedback every two weeks.
Your relation:  Your direct manager. 
                Approves your PRs occasionally,
                but mostly focused on roadmap,
                stakeholder communication, hiring.
                First person you go to for 
                career questions or blockers.
```

---

### 2. Tech Lead — Elena Müller
```
Role:           Tech Lead + Senior Engineer (L3-L4)
Location:       Berlin, Germany (hybrid, 3 days office)
Background:     - CS degree from LMU Munich
                - 8 years experience
                - Spring Boot specialist
                - Worked at Celonis (process mining 
                  company, Munich) before joining
                - Joined 2 years ago
Personality:    Very technical, detail-oriented in 
                code reviews. High standards.
                Initially can feel intense but 
                genuinely invested in team growth.
Your relation:  Most important person for your 
                technical growth. She designs the 
                architecture, you implement it.
                She reviews nearly all your PRs 
                in the first 6 months.
                Your primary technical mentor.
```

---

### 3. Senior Engineer — Arjun Sharma
```
Role:           Senior Engineer (L3)
Location:       Amsterdam, Netherlands (remote)
Background:     - 7 years experience
                - Originally from India, moved to 
                  Amsterdam 4 years ago
                - Strong in Kafka, distributed systems
                - Previously at Booking.com 
                  (backend, payments team)
                - Joined 18 months ago
Personality:    Very approachable, explains things
                patiently. Goes deep on Kafka and 
                distributed systems.
Your relation:  Second most important mentor for you.
                When Elena is unavailable, Arjun 
                reviews your PRs.
                He's the one who introduces you 
                to Kafka properly at month 7-8.
                You pair-program with him often.
```

---

### 4. Mid-level Engineer — Sophie Laurent
```
Role:           Mid-level Engineer (L2)
Location:       Paris, France (remote)
Background:     - 4 years experience
                - Java + Spring Boot background
                - Previously at a French SaaS 
                  startup (HR tech)
                - Joined 14 months ago
                - Strong on JPA and database work
Personality:    Collaborative, good at writing 
                clear tickets and documentation.
Your relation:  Work closely together on feature 
                implementation. She often breaks 
                down larger tickets into subtasks
                and assigns some to you.
                Good person to ask "quick questions"
                without feeling judged.
```

---

### 5. Mid-level Engineer — Tomás Novák
```
Role:           Mid-level Engineer (L2)
Location:       Prague, Czech Republic (remote)
Background:     - 3.5 years experience
                - Full-stack leaning backend
                - Spring Boot + some React
                - Previously at a Prague-based 
                  fintech startup
                - Joined 10 months ago
Personality:    Pragmatic, moves fast, sometimes
                skips documentation (causes friction
                with Elena occasionally).
Your relation:  Peer. You collaborate on the same
                service. He's the person who helps
                you understand "how things are done
                here" in the first few weeks.
                Good working relationship overall.
```

---

### 6. Mid-level Engineer — Marta Kowalski
```
Role:           Mid-level Engineer (L2)
Location:       Warsaw, Poland (remote)
Background:     - 4 years experience
                - Strong in API design and 
                  Spring Security
                - Previously at a Polish e-commerce
                  company (backend)
                - Joined 8 months ago (joined 
                  2 months after you)
Personality:    Very methodical. Writes excellent
                tests. You learn a lot about 
                testing best practices from her.
Your relation:  Peer. Since she joined after you,
                you actually help onboard her 
                to the codebase in month 3.
                Good for your confidence.
```

---

### 7. Mid-level Engineer — Finn Andersen
```
Role:           Mid-level Engineer (L2)
Location:       Copenhagen, Denmark (remote)
Background:     - 3 years experience
                - Kotlin + Java background
                - Previously at a Danish logistics
                  startup
                - Joined 6 months before you
Personality:    Quiet, very focused. 
                Does deep work independently.
                Excellent at performance tuning
                and query optimization.
Your relation:  Limited direct collaboration 
                initially. Later, when you hit 
                N+1 query issues in month 5,
                he's the person who helps you 
                debug and fix it.
```

---

### 8. Mid-level Engineer — Priya Nair
```
Role:           Mid-level Engineer (L2)
Location:       Amsterdam, Netherlands (remote)
                (Originally from India, 
                relocated to Netherlands)
Background:     - 3.5 years experience
                - Spring Boot + Redis specialist
                - Previously at a Dutch payment
                  startup
                - Joined 1 year before you
Personality:    Very good at explaining complex
                topics simply. Patient teacher.
Your relation:  She introduces you to Redis 
                caching concepts at month 9-10.
                You shadow her work on caching
                before taking ownership yourself.
```

---

### 9. Junior Engineer — Kemal Aydin
```
Role:           Junior Engineer (L1)
Location:       Istanbul, Turkey (remote, 
                contractor via Turing — same as you)
Background:     - 1.5 years experience
                - CS degree from Istanbul 
                  Technical University
                - Java + Spring Boot
                - Joined 3 months after you
Personality:    Enthusiastic, asks lots of questions.
                Sometimes over-engineers solutions.
Your relation:  Peer junior. Since you joined first,
                you help him navigate the codebase.
                You grow together technically.
                Good friendship develops over time.
```

---

### 10. Junior Engineer — Léa Dubois
```
Role:           Junior Engineer (L1)
Location:       Lyon, France (remote)
Background:     - 1 year experience
                - Bootcamp graduate (Le Wagon, Paris)
                - JavaScript background, 
                  transitioning to Java
                - Joined 4 months after you
Personality:    Sharp learner, good instincts.
                Struggles initially with 
                Java/Spring concepts.
Your relation:  You help her with Spring Boot
                concepts since you learned them
                recently yourself.
                You find explaining things to her
                actually reinforces your own learning.
```

---

### 11. QA Engineer — David Horáček
```
Role:           QA Engineer (embedded)
Location:       Brno, Czech Republic (remote)
Background:     - 5 years QA experience
                - API testing, Postman, 
                  integration testing
                - Joined 1 year before you
Personality:    Thorough. Finds edge cases 
                nobody else thinks of.
                Sometimes seen as "slowing things
                down" but saves production issues.
Your relation:  You work with David during 
                testing phase of each sprint.
                He reports bugs on your code —
                initially frustrating, but you 
                learn to write better code knowing
                David will check it thoroughly.
```

---

### Team Quick Reference

```
NAME            ROLE            LOCATION        YOE    JOINED
──────────────────────────────────────────────────────────────
Lukas Becker    EM              Berlin, DE      11yr   2.5yr ago
Elena Müller    Tech Lead       Berlin, DE      8yr    2yr ago
Arjun Sharma    Senior Eng      Amsterdam, NL   7yr    18mo ago
Sophie Laurent  Mid-level       Paris, FR       4yr    14mo ago
Tomás Novák     Mid-level       Prague, CZ      3.5yr  10mo ago
Marta Kowalski  Mid-level       Warsaw, PL      4yr    8mo ago*
Finn Andersen   Mid-level       Copenhagen, DK  3yr    6mo ago*
Priya Nair      Mid-level       Amsterdam, NL   3.5yr  12mo ago
YOU             Junior          Kolkata, IN     ~1yr   Oct 2024
Kemal Aydin     Junior (Turing) Istanbul, TR    1.5yr  Jan 2025*
Léa Dubois      Junior          Lyon, FR        1yr    Feb 2025*
David Horáček   QA              Brno, CZ        5yr    12mo ago

*months relative to your Oct 2024 join date
```

---

## Layer 6 — Team Working Style & Processes

### Sprint Structure (2-Week Sprints)

```
WEEK 1:
────────
Monday:     Sprint Planning (2 hours)
            - Lukas presents sprint goal
            - Elena breaks down epics into tickets
            - Team estimates using story points
            - You pick tickets appropriate for your level

Tuesday-    Development work
Friday:     Daily standup (15 min, async-first)

WEEK 2:
────────
Monday-     Development + testing
Wednesday:

Thursday:   
Morning:    Code freeze (no new PRs merged)
Afternoon:  Sprint Demo (45 min)
            - Each person demos what they built
            - Stakeholders (Product, Design) attend
            - You present your work from day 1
            - Initially nerve-wracking, becomes normal

Friday:     
Morning:    Sprint Retrospective (1 hour)
            - What went well
            - What didn't
            - Action items for next sprint
Afternoon:  Backlog Refinement
            - Next sprint tickets groomed
            - Estimates added
            - Acceptance criteria written
```

---

### Daily Standup Format

Fully async-first because team spans Berlin, Amsterdam, Paris, Warsaw, Prague, Copenhagen, Istanbul, Kolkata.

```
ASYNC (Slack, posted by 10am CET):
────────────────────────────────────
Each person posts:
✅ Yesterday: what I completed
🔨 Today: what I'm working on
🚧 Blockers: anything blocking me

SYNC (optional, 3x per week):
────────────────────────────────────
Tuesday, Wednesday, Thursday
15 min on Google Meet
9:30am CET (2pm IST for you)
Only for people who opted in or have blockers
```

**Why async-first?**
Your timezone (IST) is 3.5 hours ahead of CET. 9:30am CET = 2pm IST. Manageable, but the team is considerate — no mandatory meetings before 9am CET.

---

### Ticket Lifecycle

```
BACKLOG
   │
   │ Refined in backlog grooming
   ▼
READY FOR SPRINT
   │
   │ Picked during sprint planning
   ▼
IN PROGRESS
   │
   │ Developer creates feature branch
   │ git checkout -b feature/EXP-234-add-expense-validation
   ▼
IN REVIEW
   │
   │ PR opened, reviewers assigned
   │ (minimum 2 approvals required)
   ▼
QA
   │
   │ David tests on staging environment
   ▼
READY TO MERGE
   │
   │ Merged to main branch
   │ CI/CD deploys to staging automatically
   ▼
DONE
   │
   │ Deployed to production in next release
   ▼
CLOSED
```

---

### Code Review & PR Process

```
PR RULES (enforced):
─────────────────────
- Maximum 400 lines changed per PR
  (larger PRs must be split)
- Minimum 2 approvals to merge
- Must pass all CI checks:
  ├── Unit tests (>80% coverage required)
  ├── Integration tests
  ├── SonarQube static analysis
  └── No critical security vulnerabilities
- PR description must include:
  ├── What changed and why
  ├── How to test it
  └── Link to JIRA ticket

REVIEW ASSIGNMENTS:
────────────────────
Your PRs in months 1-3:
- Always reviewed by Elena (required)
- Plus one other mid-level

Your PRs in months 4-6:
- Elena reviews most, Arjun reviews some
- You start reviewing other juniors' PRs

Your PRs in months 7+:
- Elena spot-checks, Arjun or Sophie 
  are primary reviewers
- You regularly review Kemal and Léa's PRs
```

---

### Communication Channels

```
SLACK:
───────
#expense-ap-team        → team-wide discussions
#expense-ap-dev         → technical discussions, PR links
#expense-ap-alerts      → automated monitoring alerts
#engineering-all        → company-wide eng announcements
#incidents              → production incidents
#random                 → team banter, memes

JIRA:
──────
All tickets, epics, sprints tracked here
Ticket format: EXP-{number} for your team
(EXP = Expense team prefix)

CONFLUENCE:
────────────
Architecture decision records (ADRs)
Runbooks (how to handle incidents)
Onboarding docs
API documentation

GOOGLE MEET:
─────────────
All video calls
Standups, planning, retros, 1:1s

GITHUB:
────────
Code, PRs, code review comments
```

---

### Meetings You Attend

```
RECURRING MEETINGS:
────────────────────
Daily async standup          → Every day (async Slack)
Sync standup                 → Tue/Wed/Thu (optional, 15min)
Sprint Planning              → Every 2 weeks Monday (2hr)
Sprint Demo                  → Every 2 weeks Thursday (45min)
Sprint Retrospective         → Every 2 weeks Friday (1hr)
Backlog Refinement           → Every 2 weeks Friday (1hr)
1:1 with Lukas               → Every 2 weeks (30min)
Tech sync with Elena         → Weekly (30min, first 3 months)
                               Bi-weekly after that
Engineering All-Hands        → Monthly (1hr, whole eng dept)

AD-HOC MEETINGS:
─────────────────
Incident postmortems         → After any production incident
Architecture review          → When proposing major changes
Cross-team syncs             → When your service touches 
                               another team's service
```

---

## Layer 7 — Your Onboarding (First Month)

This is important for interview questions like *"Tell me about your onboarding experience"* or *"How did you ramp up on a new codebase?"*

### Week 1 — Orientation
```
Day 1:
- Laptop setup (MacBook Pro, company-issued)
- Accounts created: GitHub, JIRA, Confluence, 
  Slack, AWS console (read-only), Datadog
- Lukas gives 1-hour welcome call
- Introduced to team on Slack

Day 2-3:
- Read onboarding doc on Confluence
- Set up local development environment
  (Docker Compose runs all services locally)
- Read the Expense Service README
- Run the service locally for the first time

Day 4-5:
- Elena does 2-hour architecture walkthrough
  (screen share, walks through the codebase)
- Tomás shows you "how to pick up a ticket"
- You shadow Tomás's work — read his PR, 
  understand what he's doing
```

### Week 2 — First Ticket
```
Lukas assigns you: EXP-187
"Add validation for expense amount — 
 must be positive and not exceed €50,000"

This is intentionally simple:
- Touch the controller layer (validation annotations)
- Touch the service layer (business rule)
- Write unit tests
- Open your first PR

Elena reviews it thoroughly:
- 8 comments (feels like a lot!)
- Mostly style, naming conventions, 
  missing edge case tests
- You fix everything, PR merged by end of week

Feeling: overwhelming amount of feedback,
but Elena explains each comment patiently
```

### Week 3-4 — Building Familiarity
```
- Two more small tickets completed
- Start attending sprint demo 
  (just watching, not presenting yet)
- Start understanding the full expense 
  submission flow end-to-end
- Ask lots of questions on Slack 
  (Tomás and Sophie are most responsive)
- Make first mistake: accidentally push 
  to main branch directly instead of 
  feature branch (branch protection catches it,
  no damage done, but embarrassing)
- Lukas reassures you: "Everyone does this once"
```

---

## Layer 8 — Mentorship Structure

```
FORMAL MENTORSHIP:
───────────────────
Elena Müller (Tech Lead):
- Mentors you on Java/Spring best practices
- Weekly 30-min tech sync for first 3 months
- Reviews all your PRs with detailed comments
- Sets technical growth goals with you

Lukas Becker (EM):
- Career mentorship, not technical
- Bi-weekly 1:1s
- Helps you understand business context
- Gives performance feedback quarterly

INFORMAL MENTORSHIP:
─────────────────────
Arjun Sharma:
- Go-to person for distributed systems questions
- Pair-programming sessions

Sophie Laurent:
- Go-to for JPA/database questions
- Approachable for "silly questions"

Priya Nair:
- Redis and caching concepts (month 9+)

PEER LEARNING:
───────────────
Kemal Aydin (fellow junior, Turing):
- Share learnings, study together async
- Review each other's PRs
- WhatsApp group for juniors (unofficial)
```

---

## Layer 9 — Hierarchy & Decision Making

```
TECHNICAL DECISIONS:
─────────────────────
Small decisions (naming, implementation detail):
→ Developer decides, mentioned in PR

Medium decisions (new abstraction, new library):
→ Developer proposes, Tech Lead approves
→ Discussed in PR or quick Slack thread

Large decisions (new service, architecture change):
→ ADR (Architecture Decision Record) written
→ Elena authors it, Arjun reviews
→ Lukas approves from team perspective
→ Shared with VP Engineering for visibility

PRODUCT DECISIONS:
───────────────────
Product Manager (not in eng team) owns roadmap
→ Works with Lukas on sprint priorities
→ Engineers give effort estimates
→ PM makes final call on prioritization

INCIDENT DECISIONS:
────────────────────
On-call engineer (rotation) makes 
immediate decisions during incidents
→ Escalates to Elena/Arjun if complex
→ Lukas informed for business impact
```

---

## Summary — Your Position in This Team

```
YOU (Oct 2024, L1 Junior):
──────────────────────────
Contract via Turing
IST timezone (2pm IST = 9:30am CET for standups)
First production Java role
Strong Spring Boot foundation from self-study
Coming in humble, learning fast

REPORTING TO: Lukas Becker (EM)
TECHNICAL MENTOR: Elena Müller (Tech Lead)
CLOSEST PEERS: Tomás Novák, Sophie Laurent, 
               Kemal Aydin (fellow Turing junior)
KAFKA MENTOR (later): Arjun Sharma
REDIS MENTOR (later): Priya Nair

GROWTH TRAJECTORY:
Month 1-3:   Ramp-up, small tickets, learn codebase
Month 4-6:   Independent features, deeper JPA work
Month 7-9:   Introduction to Kafka consumers
Month 10-12: Redis caching, more ownership
Month 13-15: Feature ownership, reviewing juniors,
             contributing to design discussions
```

---

Step 3 complete. You now have a complete, realistic picture of your team — specific enough to answer any interview question about team dynamics, mentorship, working style, or collaboration.

**Next is Step 4: Database Selection & Schema Design** — this is where we make the SQL vs NoSQL decision with full justification, then design the actual tables for your team's services.

Ready to proceed?