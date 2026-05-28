# Story 14: Teaching Léa @PreAuthorize — The First Time Someone Came to You

---

## Context — Where Léa Was at This Point

```
Léa Dubois joined the team in 
month 4 (February 2025).
Bootcamp graduate. Le Wagon, Paris.
JavaScript background.
Transitioning to Java.

When she joined, you had been 
there 4 months.
You helped her with some Spring Boot 
basics early on — the way you had 
helped Marta with codebase navigation 
in month 3.

But month 4 you helping Léa was 
casual. Opportunistic.
She'd ask something in Slack,
you'd answer if you knew it.
Nothing structured.

By month 10 you were 10 months in.
Léa was 6 months in.
The gap between you had grown —
not because she wasn't learning,
but because you had been in 
deeper water for longer.

You had shipped Kafka consumers.
You had been in a production 
incident war room.
You had designed and built 
a Redis caching layer.
You had fixed a cache stampede 
that the senior engineer missed.

Léa was still working through 
the fundamentals.
Spring Security was on her plate 
this sprint.
```

---

## The Situation

It was a Wednesday morning, week 3 of month 10. You had just merged the stampede fix PR the day before and were starting a new ticket. Your Slack notification pinged:

```
Léa (Slack DM):
────────────────
"Hey — sorry to bother you.
 I'm working on EXP-279, 
 the expense permission checks.
 
 Elena said to use @PreAuthorize 
 but I've been reading the docs 
 for an hour and I'm still confused 
 about how it actually works.
 
 I know you've used it.
 Do you have 20 minutes?"
```

You looked at your calendar. Nothing until standup at 2pm. You had a morning of focused work planned, but this was a 20-minute ask from someone who was genuinely stuck.

You replied:

```
You:
─────
"Sure. Call in 10?"
```

---

## Before the Call — You Checked What She Was Working On

You had 10 minutes. You opened JIRA and found EXP-279:

```
Ticket: EXP-279
Title:  Add permission checks to 
        expense retrieval endpoints

Description:
─────────────
Currently GET /api/v1/expenses and 
GET /api/v1/expenses/{id} have no 
role-based access control.
Any authenticated user can retrieve 
any expense from their company.

Requirements:
─────────────
1. EMPLOYEE role: can only retrieve 
   their own expenses
2. FINANCE_MANAGER role: can retrieve 
   any expense in their company
3. ADMIN role: same as FINANCE_MANAGER

Elena has suggested using @PreAuthorize
for method-level security.
```

Good. You understood the ticket. You looked at the current `ExpenseController`:

```java
// Current state — no authorization
@GetMapping("/{expenseId}")
public ResponseEntity<ExpenseResponse> getExpense(
        @PathVariable UUID expenseId,
        @RequestHeader("X-User-Id") UUID userId,
        @RequestHeader("X-Company-Id") UUID companyId) {

    ExpenseResponse response = 
        expenseService.getExpense(expenseId, companyId);

    return ResponseEntity.ok(response);
}
```

No role check. Any employee can call this with any expenseId and get back data. The ticket was right — this needed fixing.

You thought about how you had learned `@PreAuthorize` yourself. It was around month 6. Sophie had reviewed a PR where you'd written authorization logic inside the service method — checking roles inline with if-statements. She had suggested `@PreAuthorize` as a cleaner approach. Elena had explained how it worked when you asked.

You remembered what had confused you at the time: the SpEL (Spring Expression Language) syntax looked like magic. `hasRole('FINANCE_MANAGER')` — how did Spring know what roles the current user had? Where did that come from?

That was probably what was confusing Léa too.

You opened your notes from that time and refreshed the key concepts in your head before the call.

---

## The Call — 45 Minutes

Léa shared her screen. She had the Spring Security documentation open and a half-written controller with several commented-out attempts.

```
Léa:
─────
"So I understand that @PreAuthorize 
 is supposed to check roles before 
 the method runs.
 
 But I don't understand where the 
 role information comes from.
 
 The headers say X-User-Role: EMPLOYEE.
 But @PreAuthorize doesn't read headers.
 It reads something called 
 SecurityContext.
 
 And I don't understand how the role 
 gets from the header into 
 SecurityContext.
 
 Elena said 'it's set up in the 
 security config' but the security 
 config is huge and I can't find 
 where it happens."
```

```
This was a clear question.
Not vague — specific.
She had identified exactly 
what was confusing her:
the gap between the header 
and SecurityContext.

That's good debugging instinct.
She hadn't just said 
"I don't understand security."
She had narrowed it to 
one specific missing link.

You knew the answer to this.
```

You said:

```
You:
─────
"Okay — let's trace through 
 the whole thing.
 
 The confusion makes sense because 
 there are actually three separate 
 pieces, and the docs show each 
 piece in isolation without 
 explaining how they connect.
 
 Let me show you all three."
```

You shared your own screen and opened the codebase. You walked through it piece by piece.

---

### Piece 1 — The Filter

You found `JwtAuthenticationFilter.java`:

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter 
        extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Read headers that API Gateway 
        // added after JWT validation
        String userId = request.getHeader("X-User-Id");
        String companyId = request.getHeader("X-Company-Id");
        String userRole = request.getHeader("X-User-Role");

        if (userId != null && userRole != null) {

            // Convert role string to Spring Security's 
            // GrantedAuthority format
            List<GrantedAuthority> authorities = 
                List.of(new SimpleGrantedAuthority(
                    "ROLE_" + userRole
                    // ROLE_ prefix is Spring convention
                ));

            // Create an Authentication object 
            // representing this user
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    userId,       // principal (who is this?)
                    null,         // credentials (not needed)
                    authorities   // what they're allowed to do
                );

            // Store in SecurityContext for this request
            // This is what @PreAuthorize reads from
            SecurityContextHolder.getContext()
                .setAuthentication(authentication);
        }

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }
}
```

You explained what was happening:

```
You (explaining):
──────────────────
"This is the piece you were 
 looking for — the bridge.
 
 The filter runs before every 
 request reaches your controller.
 It reads the X-User-Role header
 that API Gateway added.
 
 Then it creates an Authentication 
 object — Spring Security's way 
 of representing 'who is making 
 this request and what can they do.'
 
 The important detail is this line:
 'ROLE_' + userRole
 
 If the header says EMPLOYEE,
 Spring stores ROLE_EMPLOYEE.
 If it says FINANCE_MANAGER,
 Spring stores ROLE_FINANCE_MANAGER.
 
 Spring Security always adds the 
 ROLE_ prefix automatically 
 when you use hasRole() in @PreAuthorize.
 So hasRole('FINANCE_MANAGER') 
 looks for ROLE_FINANCE_MANAGER 
 in the authorities list.
 
 Then it stores this authentication 
 in SecurityContextHolder — 
 a thread-local store that's 
 available for the entire request.
 
 That's where @PreAuthorize 
 reads from.
 
 Now it's not magic anymore."
```

Léa:

```
Léa:
─────
"Oh. So the header → filter → 
 SecurityContext → @PreAuthorize.
 That's the chain."

You:
─────
"Exactly. Four steps.
 The docs show you step 4 
 without explaining steps 1-3."
```

---

### Piece 2 — The Security Configuration

You found `SecurityConfig.java`:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ← THIS enables @PreAuthorize
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http) throws Exception {

        return http
            .csrf(csrf -> csrf.disable())
            // We don't need CSRF — 
            // API Gateway handles this
            .sessionManagement(session -> session
                .sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS))
            // No session — JWT per request
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/manage/**")
                    .permitAll()
                // Health checks — no auth needed
                .anyRequest()
                    .authenticated()
                // Everything else needs authentication
            )
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            // Our filter runs before Spring's 
            // default auth filter
            .build();
    }
}
```

```
You (explaining):
──────────────────
"Two things to notice here.

 First: @EnableMethodSecurity at the top.
 Without this annotation, @PreAuthorize 
 literally does nothing.
 The annotation exists in the code,
 but Spring ignores it.
 Elena got bit by this once 
 in an early version of the service.
 It's the first thing to check 
 if @PreAuthorize seems to 
 not be working.

 Second: the filter is registered here.
 addFilterBefore — our JwtAuthenticationFilter 
 runs before Spring's own 
 UsernamePasswordAuthenticationFilter.
 This is the order that 
 makes everything work.
 Our filter populates SecurityContext,
 then Spring's machinery reads 
 SecurityContext for authorization."
```

Léa wrote something down.

---

### Piece 3 — The Annotation Itself

Now you showed her how to actually use `@PreAuthorize`:

```java
@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    // FINANCE_MANAGER or ADMIN can call this
    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'ADMIN')")
    public ResponseEntity<PagedResponse<ExpenseResponse>> 
            getExpenses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-Company-Id") UUID companyId,
            @RequestHeader("X-User-Id") UUID userId) {

        PagedResponse<ExpenseResponse> response = 
            expenseService.getExpenses(companyId, pageable);

        return ResponseEntity.ok(response);
    }

    // Anyone authenticated can call this —
    // but we'll check ownership inside the service
    @GetMapping("/{expenseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExpenseResponse> getExpense(
            @PathVariable UUID expenseId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Company-Id") UUID companyId) {

        ExpenseResponse response = 
            expenseService.getExpense(
                expenseId, userId, companyId);

        return ResponseEntity.ok(response);
    }
}
```

Léa looked at the second endpoint:

```
Léa:
─────
"Wait — why isAuthenticated() 
 for the single expense endpoint?
 Shouldn't it check if the user 
 owns the expense?"
```

This was a good question. Exactly the right question.

```
You:
─────
"It should — but @PreAuthorize 
 can't check that.
 
 @PreAuthorize runs before the 
 method executes.
 At that point, Spring only knows 
 the user's role from SecurityContext.
 
 Spring doesn't know which expense 
 this request is asking for.
 It doesn't have the expense data 
 from the database yet.
 
 So you can't say:
 'only allow this if the expense 
  belongs to the current user'
 ...at the @PreAuthorize level.
 Because you'd need to fetch 
 the expense to check that,
 and fetching is what the method does.
 
 The ownership check — 
 'is this my expense?' — 
 has to happen inside the service method,
 after you've fetched the expense 
 from the database.
 
 @PreAuthorize handles: 
   who is allowed to call this at all?
 
 Service layer handles: 
   are they allowed to access 
   THIS specific resource?
 
 Two different layers. 
 Two different checks."
```

Léa: "Ohh. Okay. That makes sense."

You continued:

```
You:
─────
"So in the service method, 
 you'd do something like this:"
```

```java
@Transactional(readOnly = true)
public ExpenseResponse getExpense(
        UUID expenseId, 
        UUID requestingUserId,
        UUID companyId) {

    Expense expense = expenseRepository
        .findById(expenseId)
        .orElseThrow(() -> 
            new ExpenseNotFoundException(expenseId));

    // Company check — always enforced
    if (!expense.getCompanyId().equals(companyId)) {
        throw new UnauthorizedActionException(
            "Expense does not belong to your company"
        );
    }

    // Ownership check — 
    // get current user's role from SecurityContext
    Authentication auth = SecurityContextHolder
        .getContext()
        .getAuthentication();

    boolean isFinanceManagerOrAdmin = 
        auth.getAuthorities().stream()
            .anyMatch(a -> 
                a.getAuthority().equals("ROLE_FINANCE_MANAGER")
                || a.getAuthority().equals("ROLE_ADMIN"));

    // EMPLOYEE can only see their own expenses
    if (!isFinanceManagerOrAdmin 
            && !expense.getEmployeeId()
                       .equals(requestingUserId)) {
        throw new UnauthorizedActionException(
            "You can only view your own expenses"
        );
    }

    return ExpenseResponse.from(expense);
}
```

Léa studied this:

```
Léa:
─────
"So SecurityContextHolder is available 
 inside the service too?
 Not just the controller?"

You:
─────
"Yes — it's a thread-local.
 Stays available for the entire 
 request lifecycle.
 Controller, service, repository — 
 anything running on the same thread 
 as the original request can 
 access it.
 
 But — and this is important —
 don't do this in @Async methods.
 @Async runs on a different thread.
 SecurityContext doesn't transfer 
 between threads by default.
 
 We hit this as a team earlier.
 But that's a separate story — 
 don't worry about it for 
 this ticket."
```

Léa wrote something down again. Then:

```
Léa:
─────
"Can I ask something dumb?"

You:
─────
"There are no dumb questions 
 in this kind of session.
 What is it?"

Léa:
─────
"Why do we check the role in the 
 service layer AGAIN when we 
 already checked it with @PreAuthorize?
 
 Isn't that duplicating the check?"
```

```
You paused.

This was actually a sharp question.
Not dumb at all.
She had noticed something real.

You thought about it for a moment 
before answering.
```

```
You:
─────
"Good question — and you're right 
 that there's some overlap.
 
 @PreAuthorize at the controller 
 catches the broad case:
 'only FINANCE_MANAGER and ADMIN 
  can call /expenses at all.'
 
 The service check catches 
 the specific case:
 'even if FINANCE_MANAGER can call 
  /expenses, they can only see 
  expenses from THEIR company,
  not another company's.'
 
 They're checking different things.
 @PreAuthorize checks the role.
 Service checks the data.
 
 The overlap for a FINANCE_MANAGER 
 trying to access another company's 
 expense: @PreAuthorize lets them 
 through (they have the right role),
 but the service throws 
 UnauthorizedActionException 
 (wrong company).
 
 Without the service check, 
 a finance manager from company A 
 could theoretically call 
 GET /expenses/uuid-from-company-B 
 and get data they shouldn't have.
 
 @PreAuthorize protects the endpoint.
 The service protects the resource.
 Defense in depth."
```

Léa: "Defense in depth. Okay. Got it."

---

## What Happened Next — Léa's PR

The call lasted 45 minutes total. By the end, Léa had a clear picture of all three pieces. She thanked you and closed the call.

Two hours later, she opened a draft PR for EXP-279 and pinged you:

```
Léa (Slack DM):
────────────────
"I think I have it — would you 
 mind taking a look before I 
 formally request Elena's review?
 
 Not asking you to fully review it.
 Just a sanity check."
```

You looked at her PR. The core structure was correct — filter, config, annotation. But you noticed one thing:

```java
// Léa's version — one issue
@GetMapping
@PreAuthorize("hasAnyRole('FINANCE_MANAGER', 'ADMIN')")
public ResponseEntity<PagedResponse<ExpenseResponse>> 
        getExpenses(...) {
    // ...
}

// And for single expense:
@GetMapping("/{expenseId}")
@PreAuthorize("hasRole('EMPLOYEE') or 
              hasRole('FINANCE_MANAGER') or 
              hasRole('ADMIN')")
// ↑ This is unnecessarily verbose
// isAuthenticated() is cleaner 
// since any authenticated user could be valid
// (ownership check handles specifics in service)
```

You left a comment in the draft:

```
Your comment on Léa's PR:
──────────────────────────
"The logic is correct.
 One small suggestion:
 
 For the single expense endpoint,
 instead of listing all three roles,
 isAuthenticated() is cleaner.
 
 The reason: you're not restricting 
 by role here — you're restricting 
 to authenticated users only.
 The actual authorization (who can 
 see which expense) happens in the 
 service layer.
 
 hasRole('X') or hasRole('Y') or 
 hasRole('Z') covering every role 
 is effectively the same as 
 isAuthenticated() — but if you 
 add a new role in the future,
 you'd need to remember to update 
 this list too.
 
 isAuthenticated() automatically 
 covers all authenticated users 
 regardless of role.
 
 Everything else looks correct.
 The service layer ownership check 
 is exactly right."
```

Léa replied:

```
Léa:
─────
"Oh that makes sense — I was 
 just listing all three because 
 I thought I needed to be explicit.
 
 isAuthenticated() is much cleaner.
 Updated.
 
 Thanks for catching that."
```

She formally requested Elena's review. Elena approved it with two minor style comments. The PR merged end of week.

---

## What Léa Did After — The Unexpected Part

Three days later, Kemal hit a problem with `@PreAuthorize`. He posted in `#expense-ap-dev`:

```
Kemal (Slack #expense-ap-dev):
────────────────────────────────
"Quick question — I added 
 @PreAuthorize to a method and 
 it doesn't seem to be doing 
 anything. The unauthorized user 
 is still getting through.
 
 Anyone know what's wrong?"
```

Before you could respond, Léa replied:

```
Léa:
─────
"Check SecurityConfig — is 
 @EnableMethodSecurity present?
 Without it, @PreAuthorize is 
 silently ignored.
 
 Also check that your filter is 
 registered in the chain.
 Without the filter, SecurityContext 
 never gets populated and 
 @PreAuthorize always sees 
 no authorities."
```

Kemal: "Missing @EnableMethodSecurity.
That was it. Thanks!"

```
You saw this exchange in Slack.
You didn't say anything.
You didn't need to.

Three days ago, Léa didn't know 
why @PreAuthorize seemed to 
do nothing.
Three days later, she answered 
Kemal's question in Slack before 
you had typed a word.

Not because you explained it 
to her perfectly.
Because she had understood it 
well enough to teach it.
That's the only real test 
of understanding.

You felt something in that moment
that was different from the usual 
satisfaction of shipping code.
It was quieter. More abstract.

You had passed something on.
```

---

## The Broader Reflection — What Teaching Does to You

```
When Léa asked you about 
@PreAuthorize, your instinct was 
to answer her question.
Correct the gap. Fill the missing 
piece. Send her on her way.

But the call ended up being 
45 minutes, not 20.
Not because the topic was complex.
Because Léa kept asking 
follow-up questions.

"Isn't that duplicating the check?"
"Why isAuthenticated() instead of 
 listing all roles?"
"Is SecurityContextHolder available 
 in the service too?"

Each follow-up pushed you to 
explain the reason behind the thing,
not just the thing itself.

And explaining the reason behind 
the thing is what crystallizes 
your own understanding.

You could have told Léa:
"Use @PreAuthorize hasRole() on 
the controller and check ownership 
in the service."
She would have copied the pattern 
and shipped the ticket.

But she wouldn't have known why.
And you wouldn't have tested 
whether YOU knew why.

The follow-up questions are what 
separate "I know how to use this" 
from "I understand this."

Léa's question about defense in depth —
"why check it twice?" — was the 
sharpest question in the session.
And answering it clearly was 
the moment you knew that you 
actually understood Spring Security 
at the right level.
Not just how to write the annotation.
Why the annotation exists,
what it can and can't do,
and what has to be handled elsewhere.

You learned that by teaching it.
```

---

## A Quiet Conversation With Elena

A week after Léa's ticket merged, Elena mentioned it in your weekly tech sync:

```
Elena:
───────
"Léa's @PreAuthorize implementation 
 was clean. She said you helped her.
 
 I saw you left a comment on 
 her draft PR too.
 Good call on isAuthenticated() 
 vs listing all roles.
 
 One thing I want to say:
 you're doing something right 
 with how you explain things.
 
 I've seen some engineers 
 who are helpful but make 
 the person they're helping 
 feel small — like 'this is obvious,
 why didn't you know this?'
 
 Léa mentioned you didn't do that.
 She said she felt okay 
 asking follow-up questions.
 
 That's not a small thing.
 
 The best technical environments 
 are ones where it's safe to 
 not know something.
 You contributed to that."
```

```
You didn't know what to say 
to this, so you said:

"I remember what it felt like 
 to not understand SecurityContext.
 Not that long ago.
 Made it easy to not be impatient."

Elena:
───────
"That's the right instinct to keep."
```

---

## The "Tricky Question" Preparation

---

**Q1: "Explain how @PreAuthorize actually works under the hood."**

```
@PreAuthorize is Spring Security's 
method-level authorization mechanism.
It uses AOP — the same proxy 
mechanism as @Transactional.

When you annotate a method with 
@PreAuthorize, Spring wraps the 
bean in a proxy.
When the method is called,
the proxy intercepts it and evaluates 
the SpEL expression in the annotation
before delegating to the real method.

The SpEL expression — like 
hasRole('FINANCE_MANAGER') — is 
evaluated against the SecurityContext.
Spring reads the Authentication 
object from SecurityContextHolder,
gets the GrantedAuthority list,
and checks whether any authority 
matches the expression.

If the expression evaluates to true:
the method executes normally.

If false: Spring throws 
AccessDeniedException,
which the GlobalExceptionHandler 
catches and returns as 403 Forbidden.

One important constraint:
because it uses AOP proxying,
@PreAuthorize has the same 
self-call limitation as @Transactional.
If method A in a class calls 
method B in the same class 
where B has @PreAuthorize,
the annotation is silently ignored —
the proxy is bypassed.

Also: @EnableMethodSecurity must be 
present in the security configuration.
Without it, @PreAuthorize compiles 
and deploys without error but 
does absolutely nothing at runtime.
Silent failure. 
First thing to check if it 
seems not to work.
```

---

**Q2: "Why can't @PreAuthorize check data ownership — like 'only allow if this is the user's own expense'?"**

```
Because @PreAuthorize runs before 
the method executes.

At that point, Spring only knows 
what's in SecurityContext —
the user's ID and roles, 
set by the authentication filter.

Spring does not know which expense 
the request is asking for.
It cannot fetch the expense from 
the database to check ownership 
because fetching is exactly 
what the method is supposed to do.

To check ownership, you'd need 
to know:
1. Who is making the request?
   (available: userId from SecurityContext)
2. Who owns the requested resource?
   (NOT available: requires DB lookup)

The DB lookup happens inside the method.
@PreAuthorize runs before the method.
It can't use data that doesn't exist yet.

There is a Spring Security feature 
called @PostAuthorize that runs AFTER 
the method — it could theoretically 
check the returned object's ownership.
But it still means the method ran,
the DB was queried, and the data 
was loaded before the check.
If you're going to load it anyway,
it's cleaner to just check inside 
the method and throw explicitly 
with a meaningful message.

The correct pattern:
@PreAuthorize → role-level gate 
                (can this type of user 
                call this at all?)
Service layer → resource-level gate 
                (can this specific user 
                access this specific data?)

Defense in depth.
Two different questions, two different places.
```

---

**Q3: "Léa asked 'isn't that duplicating the check?' when you explained the service layer ownership check. How did you answer that?"**

```
She had noticed something real — 
there is an overlap.

@PreAuthorize with hasRole('FINANCE_MANAGER') 
AND a service check that says 
"if not FINANCE_MANAGER, check ownership" 
do overlap for the FINANCE_MANAGER case.

But they're not checking the same thing.

@PreAuthorize checks:
"Is this type of user allowed to 
call this endpoint at all?"

The service checks:
"Is this specific user allowed to 
access this specific resource?"

These look the same but differ 
on specificity.

Example where they diverge:
A FINANCE_MANAGER from company A 
calls GET /expenses/uuid-from-company-B.

@PreAuthorize: passes (correct role).
Service check: fails 
(wrong company — throws UnauthorizedActionException).

Without the service check, 
a finance manager could call 
any expense UUID — including ones 
from other companies — and get data.
@PreAuthorize doesn't stop that 
because it only knows the role,
not which company the expense belongs to.

So the "duplication" is actually:
@PreAuthorize = coarse-grained gate
Service = fine-grained gate

Defense in depth — even if one layer 
has a bug or misconfiguration, 
the other layer still protects.
For financial data, this is correct.
```

---

**Q4: "You mentioned @PreAuthorize has the same self-call limitation as @Transactional. Can you explain that?"**

```
Both @Transactional and @PreAuthorize 
use Spring's AOP proxy mechanism.

Spring doesn't modify your class.
It wraps it in a proxy object.
The proxy intercepts method calls 
from OUTSIDE the class and 
applies the behavior 
(transaction management or 
authorization check).

Self-calls — where method A in a class 
calls method B in the same class — 
go directly to the real object,
bypassing the proxy.
The proxy never sees the call.
The annotation is silently ignored.

For @PreAuthorize, this means:
If a controller method calls a service method
that has @PreAuthorize on it —
the check works because the call 
crosses from controller bean to 
service bean (external call, proxy active).

But if a service method calls another 
method in the SAME service class 
that has @PreAuthorize —
the check does NOT work.
The proxy is bypassed.

The fix is the same as for @Transactional:
extract the method to a separate bean,
so the call crosses a bean boundary 
and goes through the proxy.

In practice: we don't put @PreAuthorize 
on service methods for this reason.
We put it on controller methods only.
Service methods do their own 
explicit checks in code,
which work correctly regardless 
of how they're called.
```

---

Story 14 complete.

```
What this story demonstrates:
───────────────────────────────

Technical:
  @PreAuthorize — how it actually works,
  not just how to write the annotation.
  The three-piece chain:
    Filter → SecurityContext → @PreAuthorize.
  The self-call proxy limitation.
  The two-layer authorization pattern:
    endpoint gate + resource gate.
  When to use isAuthenticated() 
    vs hasRole().

Behavioral:
  Made Léa feel safe asking 
  follow-up questions.
  Explained why, not just what.
  Left a code review comment 
  on her draft PR before 
  she asked for full review.
  The comment improved her code 
  without making her feel corrected.
  
  Three days later, she answered 
  Kemal's question before you did.
  That's the real outcome.

Growth marker in Block 4:
  Story 12: you proposed.
  Story 13: you found and fixed.
  Story 14: you passed something on.
  
  Three different kinds of 
  contribution. All in the 
  same month.
  
  That's what "Trusted Contributor" 
  actually means.
```

Ready for Story 15 — your first ADR contribution. Shall I begin?