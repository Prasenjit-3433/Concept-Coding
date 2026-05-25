Me to you:
    I'm writing my work experience working as an SDE at a fintech startup like this,
    where the startup looks like this (provided through GitHub):
    * Business Model, Offerings, Revenue, Scale & Market.md file
    
    Btw, who I'm (at time of joining, my level)?
    I finished BS, MS in mathematics in 2021. so entire 2022, I spend learning development in 
    * JS, TS tech stack (react, nodejs, Express - went in-depth internal working of Node.js as well), 
      * learned about LLD, HLD (beginner level), 
      * DSA in java, like finished striver dsa sheet of 500+ dsa problems. 
      * Then Took an Udemy course on Nestjs like 10-15hrs length, which teaches Nestjs in depth, 
    
    Then build 3 really good quality full stack (watching youtube)
    * One was real time polling or voting system using react, Nodejs, nestjs, web-socket, docker etc - named "Rankr" (take a look at Rankr.md file, which has the readme of the project)
      * another was a core backend project in Nestjs using microservice - named Bookify.io
    
    
    ..so I've good hand holding over Nestjs. 
    
    Then started applying here & there with no reply & rejection. Finally then got an opportunity to work at EU based fintech start around personal expense & investing as a contractual job through Turing.com for almost 1 years or 12 months contract.
    
    
    
    I provided all the files regarding my knowledge & the company business model...go through it in detail.
    understand each & everything I learn because based on this foundational knowledge I've worked as a Node.js backend engineer for 12 months under contract via Turing.
    so when writing my work experience, my roles & responsibilities and my contribution while working there, you've use all the knowledge applying through my day-to-day work.
    
    
    Don't write anything, just read in detail & understand the context!
    if you've have any problem, pls let me know!

---------------------------------------------------------------
You to me:
    I've read and understood everything carefully. Here's my summary of the full context:
    
    **Who you are at the time of joining:**
    
    You're essentially a self-taught backend developer transitioning from an academic math background (BS + MS, 2021). You spent all of 2022 systematically building your skills — JS/TS ecosystem, Node.js internals, NestJS, some LLD/HLD basics, and solid DSA practice. You built two strong portfolio projects (Rankr and Bookify.io) that demonstrate real architectural thinking — not just tutorial projects. You joined FinVerse as a junior-to-mid level Node.js/NestJS backend engineer, likely on the lower end of mid given the self-taught path and no prior professional experience. The Turing.com channel also suggests you were hired as a remote contractor, probably benchmarked at a junior-mid level.
    
    **What FinVerse looks like as a workplace:**
    
    It's a lean, fast-moving Series A fintech (~85 people, 45 in eng/product) operating across 8 EU markets. The backend work would be serious — PSD2/Open Banking integrations, MiFID II compliance constraints, multi-country tax logic, ETF portfolio management flows, financial data aggregation — all areas where reliability, correctness, and security matter enormously. The stack would naturally align with what you know: Node.js/NestJS microservices, likely with message queues, REST/GraphQL APIs, and cloud infrastructure.
    
    **What I'll be working with when writing your experience:**
    
    Your day-to-day contributions will be grounded in NestJS microservices backend work — things like building or maintaining APIs for expense tracking, investment flows, savings goals, notifications, authentication — mapped authentically to your actual skill level and the company's real technical needs.
    
    I'm ready. Tell me how you'd like to proceed — format, target role/JD you're applying to, tone, or anything else!

--------------------------------------------------------------------
Me to you:

    Now you've enough idea about the business & my skills.
    I would like to mention one thing as well -
    since I worked through Turing, they signed an NDA, so in my resume, I can't disclose the company identity. I would say, I work in a persoanl spend management & investing platform.
    Give the company a hypothetical name FinVerse.
    Moreover I worked there from Aug, 2023 to Aug, 2024 (12 months almost).
    
    Our aim is to decide & write:
    
    * Business Model, Offerings, Revenue, Scale & Market (done!)
      * Tech Stack & System Architecture
      * System Design & Data Flows
      * DEEP DIVE: TEAM STRUCTURE & OWNERSHIP
      * Database selection & schema Design
      * RabbitMQ & BullMQ use cases, architecture in the whole system, like Exchange & Queue Mapping, Message Routing & so on
      * API design
      * How Redis or caching is used in My Work or in my team
      * How transaction handling happens in My Work or in my team
      * How deployment, CI, CD, DevOps stuff happens overall throughout the whole system and in my team
      * YOUR ROLE, RESPONSIBILITY, CONTRIBUTION & OWNERSHIP AS AN SDE
    
    as you can see, there are so many work. So we're gonna do all these in a very organized, step by step way. All these informations are very important because in any interview process of any good company, after 2-3 tech round, there would be a round based on my resume, prev work experience, where they grill you on "what you did, how you did, why you did...and then so many situation or scenario based questions, why you did that, what if this happen what you would do...etc". That's why understanding each step in detail is very important.
    
    The order of those steps also matters...
    Upto "System Design & Data Flows", discussion doesn't go into any particular direction or component of system.
    The system would be obviously microservice, distributed system.
    So from "DEEP DIVE: TEAM STRUCTURE & OWNERSHIP" we should have a clear ownership of which team owns which service or component of the big system. It should be very clean & clear like a good product based company, not like an early-stage crap startup, where one person doing everything!
    At this same step, my team matching should happen based on skill & experience.
    Also most important, organization-wise total employees and hierarchy on eng dept wise...from ceo, cto, vp, manager, staff eng, tech lead, team lead, mid-level eng, junior eng.
    Which team manages which services, no of people in each team. Then deep dive into the team I will be working, like each person name, background, years of experience, seniority, from which country (in EU) he/she is from as we're working remotely.
    Then Team Working Style & Processes, Daily Workflow, Sprint Structure (2-week sprints), Code Review & PR Process, Communication Channels, Meetings, Onboarding Process (Your First Month), Mentorship & Growth (although it's contract / freelance), Hierarchy & Decision Making etc.
    
    Then it's time to deep dive into the service or component my team is managing, because it's not possible to deeply study each component and moreover not needed as I'm not working on them.
    Now from "Database selection & schema Design" we're gonna study which database selection — PostgreSQL vs MongoDB (SQL vs NoSQL) — (interviewer favourite discussion, 100% chance will be asked in interview, with deep discussion in interview. Interviewer throws so many tricky ones on this because this is part of system design interview, you've to justify them properly and that showcases your seniority) and then all schema designs using Prisma ORM — how models are defined, migrations handled, and any best practices the team follows.
    
    Then again, interviewer favourite — RabbitMQ & BullMQ! (what, how & why!)
    Here we're gonna see RabbitMQ architecture, cluster setup, how producers work, exchange types (direct, topic, fanout, headers), queue & binding setup, producer confirms, consumer setup, how consumers work, and most important — exception handling & dead letter exchange (DLX) / dead letter queue (DLQ), then consumer advanced configuration for edge case handling. Then transaction handling with RabbitMQ — publisher confirms & consumer acknowledgement strategies.
    Then BullMQ — why it's needed alongside RabbitMQ (Node.js single-threaded nature, background job processing), job queues, job priorities, retries, delayed jobs, repeatable jobs, concurrency control, and worker setup in NestJS.
    Then if our system or our service needs to use microservice design patterns like SAGA or Outbox pattern — for example in case of outbox, polling strategy, CDC (Debezium) strategy.
    If our system or the services my team is working on need to use these, we should have a very detailed discussion on these.
    (btw, I only know very basics of message queues...none of these above topics I know deeply, I just listed them down for your help), so you explain these topics in very clean, clear, easy to understand language wherever we need to use them.
    
    Once we're done with use of RabbitMQ & BullMQ in the whole big system, more specifically in the services my team is working on, we're gonna deep dive into designing our APIs...but it would be only for the services my team is working on. Since the primary UI is React Native (mobile), API design should also consider mobile-first concerns like payload size, pagination, offline handling hints etc. Internal web tools (React + Tailwind) consumed APIs should also be noted where relevant.
    
    Then coming to caching in our services:
    
    * In-process (Node.js in-memory / node-cache) vs. Distributed (Redis) and when to use which
      * Consistency Patterns: A deep dive into Cache-Aside, Write-Through, and Write-Behind & other caching strategies
      * How we handle "Scary" Production Issues: Handling Cache Avalanche, Cache Stampede (using sync/locks), and Hot Key problems
      * Multi-Level Caching: Synchronizing L1 (Local in-process) + L2 (Distributed Redis) caches
      * Real-world Monitoring: Key metrics like Hit/Miss ratios and eviction rates
        How we do all these in our team?
        Not only these, how data is stored in Redis — format, structure, any best practices the team follows, how eviction happens, TTL, how invalidation happens (invalidation is interviewer favourite topic to ask and it's one of the difficult topics as well)
    
    Then transaction handling in our team — using PostgreSQL transactions via Prisma, any best practices followed, concurrency & isolation levels, moreover if any scenarios when transactions have to work with async operations or background jobs via BullMQ — most critical ones.
    
    Then how deployment, CI, CD, DevOps stuff happens by our team. Because in interview, sometimes interviewer directly asks, "explain your CI/CD process in your team."
    
    Then monitoring, distributed tracing, distributed logging:
    This is very important because in an interview, they directly ask, "okay, you wrote in your resume, you improved this by 60% or latency reduced from X to Y etc"...how did you measure this.
    Even though the system is likely two monoliths — one NestJS and one Go service — distributed tracing is still essential, because without propagating a shared trace ID across both services, logs from each are completely isolated and you lose end-to-end request visibility.
    We use OpenTelemetry (OTEL) as the instrumentation layer in both services — vendor-neutral, so traces, metrics, and logs all flow through the OTEL Collector, which exports everything to Datadog. Datadog serves as our single observability platform — covering APM & distributed tracing, centralized log management, infrastructure metrics, dashboards, and alerting. This managed SaaS-first approach is a deliberate startup choice: full observability without the overhead of maintaining open-source tooling yourself
    
    if you have any doubt ask me!

---------------------------------------------------------------------
You to me:
    This is an incredibly well-thought-out plan. I've read every word carefully. Let me reflect back my full understanding so we're perfectly aligned before we start:
    
    ---
    
    **The Big Picture Goal:**
    Build a complete, interview-proof knowledge base of your 12-month contract at FinVerse — so detailed and authentic that you can confidently answer *any* "tell me about your previous work" question, including deep system design, architectural trade-offs, and situational/behavioural grilling.
    
    ---
    
    **The Ordered Plan (as I understand it):**
    
    1. ✅ Business Model, Offerings, Revenue, Scale & Market — *Done*
       2. Tech Stack & System Architecture
       3. System Design & Data Flows
       4. Deep Dive: Team Structure & Ownership ← *pivot point — everything gets scoped to your team from here*
       5. Database Selection & Schema Design (PostgreSQL vs MongoDB + Prisma)
       6. RabbitMQ & BullMQ — architecture, patterns, SAGA/Outbox if applicable
       7. API Design — scoped to your team's services, mobile-first (React Native)
       8. Caching — Redis deep dive, invalidation, multi-level, production failure modes
       9. Transaction Handling — PostgreSQL + Prisma, concurrency, isolation levels, async edge cases
       10. CI/CD & DevOps
       11. Monitoring, Distributed Tracing & Logging — OpenTelemetry → Datadog
       12. Your Role, Responsibilities, Contributions & Ownership as an SDE
    
    ---
    
    **Key constraints I'll respect throughout:**
    
    * NDA — company called FinVerse, described as a "personal spend management & investing platform"
      * Aug 2023 – Aug 2024 (12 months), via Turing.com
      * You joined as junior-to-mid NestJS backend engineer, no prior professional experience
      * System is microservices/distributed — but I need to design it realistically for a ~85-person Series A fintech, not over-engineer it
      * Your team's ownership must be clearly scoped — you go deep only on what your team owns
      * Observability stack is already decided: OpenTelemetry → Datadog
      * You're honest about your knowledge gaps (message queues etc.) — I'll teach as I build
      * Everything must be defensible in an interview — not just written, but *explainable*
    
    ---
    Q: Primary client — what's the main way users interact with FinVerse? (Select all that apply)
    A: Mobile app (React Native) — primary

    Q: For the Go service — are you okay with me deciding where Go fits naturally (e.g. portfolio calc, data aggregation) based on what makes technical sense? Or do you want to keep it pure NestJS everywhere?
    A: Yes, you decide where Go fits naturally

    Q: Third-party integrations — which of these does FinVerse realistically connect to? (Select all that apply)
    A: Open Banking / PSD2 banks, ETF/investment data providers, Payment processors (Stripe etc.), Email/SMS providers (SendGrid, Twilio), State pension APIs (EU gov)

    Q: Open Banking / PSD2 integration — which provider does FinVerse use to connect to EU banks?
    A: You decide what fits best

    Q: ETF & market data — where does FinVerse get real-time and historical ETF prices?
    A: You decide what fits best

    Q: Payment processing — what does Stripe handle here specifically? (Select all that apply)
    A: Subscription billing (Premium & Family plans), Investment deposits (moving money into portfolios), Goal-based automated transfers, Refunds & cancellations

    Got it — all confirmed. The document above is our locked Step 2.
    Now before we jump into Step 3, I want to do what I should have done after Step 2 was first written — a quick comprehension check. Because Step 3 (System Design & Data Flows) builds directly on top of this architecture. If anything is unclear now, it will compound in the next step.
    
    Three questions — answer them like an interviewer just asked you. Your own words, no pressure:
    1. "You have a modular monolith for Core Product but separated out Payments and Notifications as their own services. Why not just keep everything together in one big NestJS app?"
    2. "Why did your team pick Go for the Market Data service? Why not just spin up another NestJS service?"
    3. "Core Product publishes a budget.threshold.exceeded event to RabbitMQ instead of directly calling the Notification Service's API. Why? What does that buy you?"
-------------------------------------------------------------------------------------
Me to you:
    You don't need to put interview questions answer things. We don't need them right now.
    Right now Just focus on writing good quality content for those future steps.
    
    Okay, if you're not ready to write Step 2: Tech Stack & System Architecture, pls write it

---------------------------------------------------------------------------------
You to me:
    it's ready and saved with filename - Tech Stack & System Architecture.md in folder - in Module 2: Tech Stack & System Architecture.
    ready for Step 3: System Design & Data Flow