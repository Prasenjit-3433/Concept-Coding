# Story 2: Real Codebase vs Tutorial Codebase — The Onboarding Shock

---

## Context — Why This Story Matters

This story is not about a feature you built or a bug you fixed. It is about something more fundamental — the gap between knowing how to code and knowing how to work in a real system.

Most engineers who learned through courses or personal projects hit this wall when they join their first real team. The wall is not about skill. It is about context. And how you navigate that wall in the first two weeks shapes everything that comes after.

This story matters in interviews because it shows:

```
- How you handle ambiguity and confusion
- Whether you can learn independently 
  without someone holding your hand 
  for every step
- How you ask for help — 
  specifically, smartly, not randomly
- Whether you understand the difference 
  between "I don't know this" and 
  "I don't know this YET"
```

---

## The Situation

It was day 3. Lukas had said in the welcome call:

*"Your goal for week 1 is simple. Get the service running locally. Read the README. Understand what the Expense Service does at a high level. Don't write any code yet."*

Simple goal. You opened the repository, found the README, and followed the setup instructions.

```
README said:
─────────────
Prerequisites:
- Java 17
- Maven 3.9+
- Docker Desktop running

Setup:
1. Clone the repository
2. Run: docker compose up -d
3. Run: mvn spring-boot:run 
        -pl expense-service 
        -Dspring-boot.run.profiles=dev
4. Service should be available 
   at http://localhost:8080
```

You had Java 17. You had Maven. You had Docker. You ran `docker compose up -d`.

Six containers started:

```
✔ Container moss-postgres    Started
✔ Container moss-redis       Started
✔ Container moss-kafka       Started
✔ Container moss-zookeeper   Started
✔ Container moss-wiremock    Started
✔ Container moss-kafka-ui    Started
```

Good. Then you ran the Spring Boot application.

It failed immediately.

---

## The Failure — And What You Did With It

The error was long. Hundreds of lines in the terminal. Your first instinct was panic. Your second instinct — which was the right one — was to scroll to the very top of the error and read it from the beginning.

```
This is something nobody explicitly 
teaches you:
─────────────────────────────────────
When a Spring Boot application fails 
to start, the FIRST error is the 
real error. Everything below it is 
a cascade — Spring trying to start 
other things that depend on the 
thing that failed first.

If you read from the bottom up,
you're reading consequences, 
not causes.

Always read the error from the top.
```

The actual first error was:

```
***************************
APPLICATION FAILED TO START
***************************

Description:

Failed to configure a DataSource: 
'url' attribute is not specified 
and no embedded datasource could 
be configured.

Reason: Failed to determine a 
suitable driver class

Action:

Consider the following:
  If you want an embedded database 
  (H2, HSQL or Derby), please put it 
  on the classpath.
  If you have database settings to 
  be configured, please define the 
  property 'spring.datasource.url'.
```

You read this carefully. Spring Boot was trying to connect to a database but couldn't find the configuration. But you had started PostgreSQL in Docker — it was running. So why wasn't Spring finding it?

You opened `application.properties`:

```properties
# application.properties (base)
spring.application.name=expense-service
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

management.endpoints.web.exposure.include=*
```

No `spring.datasource.url`. No database credentials. Nothing.

Then you remembered — the README said to run with `-Dspring-boot.run.profiles=dev`. You hadn't done that yet. You had just run `mvn spring-boot:run -pl expense-service`.

You looked for `application-dev.properties`:

```properties
# application-dev.properties
spring.datasource.url=\
  jdbc:postgresql://localhost:5432/expense_db
spring.datasource.username=moss
spring.datasource.password=localdev
spring.datasource.driver-class-name=\
  org.postgresql.Driver

spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

logging.level.com.moss=DEBUG

# Point to local WireMock 
# instead of real User & Org Service
moss.services.user-org.base-url=\
  http://localhost:8090
```

This was your first encounter with Spring Profiles in a real system — not from a tutorial but from actually needing them.

```
What you understood from this:
────────────────────────────────
application.properties = base config,
  shared across all environments.
  Contains things that never change:
  app name, JPA settings, actuator config.

application-dev.properties = dev overrides.
  Contains things specific to local dev:
  local DB credentials, debug logging,
  WireMock URL instead of real services.

When you run with -Dspring-boot.run.profiles=dev,
Spring loads BOTH files.
application-dev.properties values 
override application.properties for 
matching keys.
New keys in application-dev.properties 
are added on top.
```

You ran the application again with the correct command:

```
mvn spring-boot:run \
  -pl expense-service \
  -Dspring-boot.run.profiles=dev
```

It failed again. Different error this time:

```
org.flywaydb.core.api.exception
.FlywayValidateException: 
Validate failed: 
Migrations have failed validation
Detected failed migration to version 3 
(add expense audit logs).
Please restore backups and roll back 
database and code!
```

---

## The Second Failure — Flyway

This one was more confusing. You had never worked with Flyway in a real project before. You had read about it in the Spring Boot notes, but seeing it fail in practice was different.

You spent about 20 minutes reading the Flyway documentation before deciding this was a case where asking was faster than guessing. You sent a message to Tomás on Slack:

*"Hey Tomás — getting a Flyway validation error when starting expense-service locally. It says migration V3 failed. I set up with docker compose up -d and then ran with dev profile. Am I missing a step?"*

Tomás replied in 5 minutes:

*"Ah yes — the postgres container starts with a completely empty database. Flyway will run all migrations from scratch automatically. But it sounds like your postgres container has a partially migrated DB from someone else's earlier testing. Just run: `docker compose down -v` then `docker compose up -d` again. The `-v` flag removes the volumes — wipes the database completely. Then Flyway starts clean."*

```
docker compose down -v
docker compose up -d
mvn spring-boot:run \
  -pl expense-service \
  -Dspring-boot.run.profiles=dev
```

This time it worked. The application started. You saw this in the terminal:

```
INFO  FlywayAutoConfiguration: 
  Flyway Community Edition is enabled.
INFO  FlywayMigration: 
  Current version of schema "public": << Empty Schema >>
INFO  FlywayMigration: 
  Migrating schema "public" to version 1 
  - create companies and employees
INFO  FlywayMigration: 
  Migrating schema "public" to version 2 
  - create expenses table
INFO  FlywayMigration: 
  Migrating schema "public" to version 3 
  - add expense audit logs
INFO  FlywayMigration: 
  Migrating schema "public" to version 4 
  - create approval steps
INFO  FlywayMigration: 
  Migrating schema "public" to version 5 
  - create reimbursements
INFO  FlywayMigration: 
  Migrating schema "public" to version 6 
  - create outbox events
INFO  FlywayMigration: 
  Successfully applied 6 migrations 
  to schema "public" 
  (execution time 00:00.387s)
INFO  TomcatWebServer: 
  Tomcat started on port(s): 8080 (http)
INFO  ExpenseServiceApplication: 
  Started ExpenseServiceApplication 
  in 8.432 seconds
```

The service was running. But you weren't done being confused.

---

## The Third Challenge — WireMock

You opened Postman and tried to call the health endpoint:

```
GET http://localhost:8080/manage/health
```

Response:

```json
{
  "status": "DOWN",
  "components": {
    "userOrgService": {
      "status": "DOWN",
      "details": {
        "error": "Connection refused: localhost:8090"
      }
    },
    "database": { "status": "UP" },
    "kafka": { "status": "UP" },
    "redis": { "status": "UP" }
  }
}
```

Overall status was DOWN because the User & Org Service health check was failing. But you had started all the Docker containers. What was wrong?

You checked docker compose again:

```
✔ Container moss-wiremock    Started  → port 8090
```

WireMock was running on port 8090. But the health check was failing. You opened your browser and went to `http://localhost:8090` — and got a WireMock admin page. WireMock itself was running.

You went to the `wiremock/` folder in the repository and found:

```
wiremock/
├── mappings/
│   ├── get-approval-policy.json
│   ├── get-employee-by-id.json
│   └── get-company-by-id.json
└── __files/
    ├── approval-policy-response.json
    ├── employee-response.json
    └── company-response.json
```

WireMock was configured with stub responses — fake responses for FeignClient calls to User & Org Service. But there was no stub for the health check endpoint.

You looked at the `UserOrgHealthIndicator.java`:

```java
@Component
@RequiredArgsConstructor
public class UserOrgHealthIndicator 
        implements HealthIndicator {

    private final UserOrgFeignClient userOrgClient;

    @Override
    public Health health() {
        try {
            userOrgClient.healthCheck();
            return Health.up().build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

It was calling `userOrgClient.healthCheck()` — a FeignClient call to `GET /manage/health` on the User & Org Service. In production, this would hit the real service. In local dev, it should hit WireMock. But there was no WireMock stub for this endpoint.

You added a stub:

```json
// wiremock/mappings/health-check.json
{
  "request": {
    "method": "GET",
    "url": "/manage/health"
  },
  "response": {
    "status": 200,
    "headers": {
      "Content-Type": "application/json"
    },
    "bodyFileName": "health-response.json"
  }
}
```

```json
// wiremock/__files/health-response.json
{
  "status": "UP"
}
```

You restarted WireMock (just the container):

```
docker compose restart moss-wiremock
```

Checked health again:

```json
{
  "status": "UP",
  "components": {
    "userOrgService": { "status": "UP" },
    "database": { "status": "UP" },
    "kafka": { "status": "UP" },
    "redis": { "status": "UP" }
  }
}
```

All UP. And you had just fixed something in the shared repository that nobody had added yet.

---

## What You Did Next — And Why It Matters

This is the part of the story that matters most for interviews.

You had fixed the WireMock stub for yourself. You could have just moved on. But you stopped and thought about it:

```
If I hit this problem, will the next 
person who joins also hit it?
Answer: yes. Obviously.

Should I just fix it silently and 
move on?
Or should I tell someone?
```

You created a PR with the WireMock stub addition and a one-line update to the README:

```
README update:
───────────────
Added note: "If health check shows 
userOrgService DOWN, restart WireMock 
container: docker compose restart moss-wiremock"
```

You sent a message in `#expense-ap-dev` on Slack:

*"While setting up locally I noticed the WireMock config was missing a stub for the User & Org health check endpoint — the health endpoint showed DOWN even when everything was working. Added a stub and updated the README. PR: [link]. Small fix but might save the next person some confusion."*

Lukas responded in the channel:

*"Nice catch. Merging this."*

Elena responded:

*"This is exactly the right instinct — when you fix something during onboarding, document it immediately. You have the freshest eyes on what's missing."*

```
What this moment taught you:
──────────────────────────────
Onboarding is not just learning.
It is also contributing.

New engineers have something senior 
engineers don't have:
fresh eyes.

You see things that are broken or 
confusing because you don't have 
the "oh, I already know that" filter.
That's valuable.

If you fix something silently, 
you solve it for yourself.
If you document it and raise a PR, 
you solve it for every person 
who joins after you.

That's the difference between 
consuming a codebase and 
contributing to it.
```

---

## The Deeper Lesson — Tutorial vs Real Codebase

By the end of week 2, you sat down and wrote a note to yourself (you kept notes in Notion throughout). This is what you wrote:

```
What's different about a real codebase:
─────────────────────────────────────────

1. NOTHING is self-contained
   ──────────────────────────
   In a tutorial: one service, 
   one database, everything works 
   out of the box.
   
   In reality: the service depends on 
   a database, a cache, a message broker,
   two other services (via FeignClient), 
   an external OCR API (via WireMock),
   a config server, and Spring Security.
   
   Any one of them not working = 
   service doesn't start.
   You need to understand the whole 
   dependency graph just to run it locally.

2. ERRORS are not friendly
   ─────────────────────────
   Tutorial errors: 
   "NullPointerException at line 23"
   → one file, one line, easy to fix.
   
   Real errors: 
   500 lines of stack trace, 
   with the real cause buried 
   on line 3 and everything else 
   being Spring's internal machinery 
   unwinding.
   
   You have to learn to read errors 
   differently — find the first 
   meaningful line, ignore the cascade.

3. CONFIGURATION is layered
   ──────────────────────────
   Tutorial: one application.properties,
   everything in it.
   
   Reality: base properties, 
   dev properties, prod properties, 
   secrets from AWS Secrets Manager, 
   environment variables injected 
   by ECS at runtime, 
   Spring Cloud Config Server 
   overriding local properties.
   
   You need to understand which layer 
   wins and why.

4. CODE TELLS A STORY
   ────────────────────
   Tutorial code is written to teach 
   one concept. It's artificial.
   
   Real code was written by 8 different 
   people over 2 years.
   It has decisions made in sprint 3 
   that were changed in sprint 17.
   It has comments explaining WHY 
   something was done that way 
   (and sometimes no comments at all).
   
   Reading real code is archaeology.
   You find layers of decisions.
   You need to understand not just 
   what the code does but 
   WHY it was written that way.

5. NOTHING breaks cleanly
   ─────────────────────────
   Tutorial: if something is wrong,
   it fails loudly and obviously.
   
   Reality: sometimes the service 
   starts fine but one health check 
   is DOWN. Sometimes everything looks 
   fine but WireMock is returning 
   a 404 for a path that doesn't 
   have a stub. Sometimes the DB 
   is running but Flyway has a 
   dirty state from someone else's 
   testing.
   
   You have to learn to look for 
   the subtle wrongness, not just 
   the obvious crash.
```

---

## The Result

```
What you shipped:
──────────────────
- WireMock stub for User & Org 
  health endpoint (tiny PR, merged)
- README update (one line, merged)
- Service running locally by end of week 2

What you learned:
──────────────────
1. Read the error from the top, 
   not the bottom
2. Spring Profiles in practice — 
   not just in theory
3. Flyway migrations run on startup 
   and need a clean DB state locally
4. WireMock is a real HTTP server 
   that replaces real service calls 
   in local dev
5. When you fix something during 
   onboarding, document it immediately — 
   you have the freshest eyes on 
   what's missing
6. Asking for help is faster than 
   guessing when you've genuinely 
   tried first
   (you spent 20 minutes on Flyway 
    before asking Tomás — 
    not 2 minutes and not 2 hours)

What you couldn't know yet:
─────────────────────────────
Why Spring Cloud Config Server 
wasn't part of local dev setup 
(services fetched config from it 
 in staging and prod but used 
 local properties in dev).
You found this out in month 3 
when a staging deploy behaved 
differently than local.
That's a later story.
```

---

## The "Tricky Question" Preparation

---

**Q1: "You mentioned reading the error from the top. But Spring Boot errors are huge — how do you know what to ignore?"**

```
The pattern I follow:

The first section labelled 
"APPLICATION FAILED TO START" 
is always the most useful.
Below it, Spring gives you:
- Description: what went wrong
- Reason: why it went wrong  
- Action: what to try

Those three sections are written 
by the Spring team specifically 
to be human-readable.
Read those first.

Below that is the full stack trace.
In the stack trace, I look for 
the first line that mentions 
my own code or a framework I know 
(like Flyway, Hibernate, Kafka).
Everything above that in the trace 
is Spring's internal machinery — 
usually not where the problem is.

For example, in the Flyway error:
The first meaningful line was:
"FlywayValidateException: 
 Detected failed migration to version 3"
That told me exactly what failed.
Everything below it was Spring 
unwinding its startup sequence — 
not useful for finding the cause.
```

---

**Q2: "Why does Spring Boot need WireMock for local development instead of just mocking in the code with Mockito?"**

```
Because Mockito mocks live inside 
the Java process.
If you mock the FeignClient with Mockito,
your mock is only active during 
unit tests — where you control 
the Spring context and explicitly 
inject the mock.

When you run the full application 
locally with spring-boot:run,
Mockito is not involved.
The real FeignClient is instantiated,
pointing to the URL in 
application-dev.properties.

WireMock solves this by being 
a real HTTP server.
The FeignClient makes a real HTTP call.
WireMock intercepts that call 
and returns a pre-configured response.
The application has no idea it's 
talking to WireMock instead of 
the real User & Org Service.

This means the full HTTP stack —
timeouts, retries, error handling —
all behaves exactly as it would 
in production.
Mockito can't test that.
WireMock can.

And it means you don't need to run 
the entire User & Org Service 
(with its own database, its own Kafka, 
its own dependencies) just to 
run the Expense Service locally.
```

---

**Q3: "You added a WireMock stub and updated the README. That seems like a very small contribution. Why bring it up?"**

```
Because the size of the contribution 
is not the point.
The point is the instinct behind it.

I could have fixed it silently for myself 
and moved on. Nobody would have noticed.
The next engineer joining would hit 
the same issue and spend time 
figuring it out again.

The instinct I want to demonstrate is:
when you fix something that was broken, 
make sure it stays fixed for the 
next person.
That's not about being impressive.
It's about caring about the team's 
productivity, not just your own.

And honestly, as a junior engineer 
in week 2, I had almost nothing to 
contribute technically.
But I had fresh eyes.
I could see what was confusing because 
I didn't have the "I already know this" 
filter that the senior engineers had.
That IS a contribution, 
even if the PR is two files changed.
```

---

**Q4: "You said 'asking for help is faster than guessing when you've genuinely tried first.' How did you decide when to stop trying and ask?"**

```
This is something I thought about 
explicitly during that first month 
because I was afraid of asking 
too many questions and looking incompetent,
but also afraid of staying stuck 
too long and looking inefficient.

The rule I settled on:
If I don't have a clear direction 
to try within 20-30 minutes of 
looking at a problem, 
I ask.

Not "I've been stuck for 3 hours 
and now I'll ask."
Not "I saw an error and immediately 
messaged the team."

The key is: I should be able to 
describe what I tried and why 
it didn't work before I ask.
That's what makes a good question.

For the Flyway error, I spent about 
20 minutes reading Flyway documentation 
and trying to understand what a 
"failed migration" meant.
I had a theory (something about the 
database state) but I didn't know 
how to fix it.
So I asked Tomás with:
"I'm getting this error. 
 I think it's related to DB state. 
 I started with docker compose up -d. 
 Is there a step I'm missing?"

That's a good question because 
it shows I thought about it,
I have a hypothesis,
and I'm asking for direction 
not for someone to just 
solve it for me.
```

---

Story 2 complete.

**Story 3 next: "Helping Marta Onboard — The First Time You Knew More Than Someone Else"**

This story is about month 3, when Marta joined the team two months after you. It covers the moment you realized you had actually learned something — and what it felt like to be on the other side of the onboarding experience for the first time. Ready?