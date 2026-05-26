# Story 1: The First PR Experience

---

## Context — Before The Story Begins

To understand this story properly, you need to understand what joining felt like at day one.

```
What you expected:
───────────────────
You had finished the Spring Boot course.
You had built projects.
You understood controllers, services,
repositories, JPA, REST APIs.
You felt reasonably prepared.

What you actually found:
─────────────────────────
A real production codebase is nothing 
like a tutorial project.

Tutorial project:
  └── src/
      ├── controller/
      │   └── UserController.java
      ├── service/
      │   └── UserService.java
      └── repository/
          └── UserRepository.java

Real codebase:
  └── src/
      ├── controller/
      │   ├── ExpenseController.java
      │   ├── ReimbursementController.java
      │   └── advice/
      │       └── GlobalExceptionHandler.java
      ├── service/
      │   ├── ExpenseService.java
      │   ├── ExpenseValidationService.java
      │   ├── ApprovalPolicyService.java
      │   └── ExpenseEventPublisher.java
      ├── repository/
      │   ├── ExpenseRepository.java
      │   ├── ApprovalStepRepository.java
      │   └── OutboxEventRepository.java
      ├── entity/
      │   ├── Expense.java
      │   ├── ApprovalStep.java
      │   └── OutboxEvent.java
      ├── dto/
      │   ├── request/
      │   │   ├── CreateExpenseRequest.java
      │   │   └── SubmitExpenseRequest.java
      │   └── response/
      │       ├── ExpenseResponse.java
      │       └── PagedResponse.java
      ├── mapper/
      │   └── ExpenseMapper.java
      ├── configuration/
      │   ├── SecurityConfig.java
      │   ├── KafkaConfig.java
      │   └── AsyncConfig.java
      ├── filter/
      │   └── CorrelationIdFilter.java
      ├── event/
      │   └── ExpenseApprovedEvent.java
      └── exception/
          ├── ExpenseNotFoundException.java
          ├── InvalidExpenseStateException.java
          └── UnauthorizedActionException.java

And this is just ONE service.
Multiple services. Hundreds of files.
Flyway migration scripts.
Docker Compose with 6 containers.
GitHub Actions pipelines.
JIRA tickets referencing code 
you haven't read yet.

Week 1 feeling: completely lost.
Not "a little confused."
Genuinely lost.
```

This is the honest starting point. And this is important to understand because it makes what happened with the first PR meaningful — it wasn't just about code. It was about learning how to operate in a real team for the first time.

---

## The Situation

It was the end of week 2. You had spent week 1 reading documentation, setting up your local environment, and following along as Tomás walked you through the codebase. You hadn't written a single line of production code yet.

Lukas assigned you your first ticket in JIRA:

```
Ticket: EXP-187
Title:  Add validation for expense amount

Description:
────────────
Currently, the expense submission endpoint 
accepts any value for `amount`, including 
negative numbers and zero. This causes 
downstream issues when the accounting 
integration tries to export these expenses 
to DATEV.

Acceptance Criteria:
─────────────────────
1. Amount must be greater than 0
2. Amount must not exceed €50,000
   (company policy limit)
3. If validation fails, return a 
   clear error message to the client
4. Unit tests must cover valid and 
   invalid cases

Assigned to: You
Story points: 2
```

Two story points. The smallest possible ticket. Intentionally simple. Lukas knew exactly what he was doing — giving you something achievable to build confidence, while also being real enough to teach you how the team works.

---

## Your Task

Your specific responsibility:

```
1. Add validation annotations to 
   the CreateExpenseRequest DTO
2. Make sure validation errors return 
   the team's standard error format
   (not Spring's default ugly error)
3. Write unit tests for the validation
4. Open a PR following team conventions
5. Address review feedback
```

Simple on paper. More complex in practice — because you had never opened a PR in a real team before. You didn't fully know what "team's standard error format" meant yet. And you definitely didn't know how thorough Elena's code reviews were.

---

## The Action — Step by Step

### Step 1: Understanding What Already Existed

Before writing anything, you spent half a day reading the existing code. This is something Tomás specifically told you in week 1:

*"Before you touch anything, read it first. Understand what's already there. Don't assume the codebase is empty just because your ticket is small."*

You looked at `CreateExpenseRequest.java` as it existed:

```java
// BEFORE your changes
@Data
public class CreateExpenseRequest {

    private BigDecimal amount;
    private String currency;
    private ExpenseCategory category;
    private String description;
    private String merchantName;
    private LocalDate expenseDate;
}
```

No validation at all. Clean slate for your task.

Then you looked at how other DTOs in the codebase were already validated — specifically `CreateInvoiceRequest.java` — to understand the pattern the team followed:

```java
// You studied this as a reference
@Data
public class CreateInvoiceRequest {

    @NotNull(message = "Supplier is required")
    private UUID supplierId;

    @NotBlank(message = "Invoice number is required")
    private String invoiceNumber;

    @NotNull
    @Positive(message = "Total amount must be greater than 0")
    private BigDecimal totalAmount;

    @NotBlank
    @Size(min = 3, max = 3,
        message = "Currency must be a 3-letter ISO code")
    private String currency;
}
```

Good. You now knew the pattern:
- Use annotations from `jakarta.validation.constraints`
- Always include a `message` in the annotation
- The team uses `@NotNull` + `@Positive` for numeric fields

You also found the `GlobalExceptionHandler.java` and read it carefully:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(fe -> fe.getField() + ": " +
                       fe.getDefaultMessage())
            .collect(Collectors.toList());

        return ResponseEntity.badRequest().body(
            ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(400)
                .error("VALIDATION_FAILED")
                .message("Request validation failed")
                .details(errors)
                .path(request.getRequestURI())
                .traceId(getCurrentTraceId())
                .build()
        );
    }
}
```

This answered your "standard error format" question. The handler already existed. You just needed to make sure your validation annotations triggered it correctly. You didn't need to touch the handler at all.

This was your first real lesson from reading existing code:

```
Don't build what already exists.
Read first. Understand the pattern.
Slot your change into the existing system.
This is how real teams work.
```

---

### Step 2: Writing the Code

Now you made your changes to `CreateExpenseRequest.java`:

```java
// AFTER your changes
@Data
public class CreateExpenseRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    @DecimalMax(
        value = "50000.00",
        message = "Amount cannot exceed €50,000"
    )
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3,
        message = "Currency must be a 3-letter ISO code")
    private String currency;

    @NotNull(message = "Category is required")
    private ExpenseCategory category;

    @Size(max = 500,
        message = "Description cannot exceed 500 characters")
    private String description;

    @Size(max = 255)
    private String merchantName;

    @NotNull(message = "Expense date is required")
    @PastOrPresent(
        message = "Expense date cannot be in the future")
    private LocalDate expenseDate;
}
```

Then you checked the controller to make sure `@Valid` was present on the request body — because without `@Valid`, none of the annotations do anything:

```java
// ExpenseController.java — you checked this existed
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<ExpenseResponse> createExpense(
        @RequestPart("expenseData")
        @Valid CreateExpenseRequest request,  // ← @Valid
        @RequestPart("receipt") MultipartFile receipt,
        @RequestHeader("X-User-Id") UUID userId,
        @RequestHeader("X-Company-Id") UUID companyId) {

    ExpenseResponse response =
        expenseService.createExpense(
            request, receipt, userId, companyId);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
}
```

Good. `@Valid` was already there. You didn't need to change the controller.

---

### Step 3: Writing Unit Tests

This is where you spent most of your time — and where you made your first mistake.

You wrote your tests initially like this:

```java
// YOUR FIRST VERSION — what you wrote initially
@SpringBootTest
class CreateExpenseRequestTest {

    @Test
    void testNegativeAmountFails() {
        CreateExpenseRequest request = 
            new CreateExpenseRequest();
        request.setAmount(new BigDecimal("-10.00"));
        // ... how do I actually test this?
        // I need to send an HTTP request...
        // So I need the whole Spring context?
    }
}
```

You got stuck. You weren't sure how to test validation annotations. Should you start the whole Spring Boot application? Should you mock things? You sat with this for about an hour before asking Sophie on Slack:

*"Hey Sophie, quick question — how do we test validation annotations on DTOs? Do we need @SpringBootTest for this or is there a simpler way?"*

Sophie replied within 10 minutes:

*"You don't need the full Spring context for DTO validation. Use the Validator directly — it's much faster. Look at how we test CreateInvoiceRequest — there's already a test class for that."*

You found `CreateInvoiceRequestTest.java` and understood immediately:

```java
// What you learned from reading 
// CreateInvoiceRequestTest.java
class CreateInvoiceRequestTest {

    // Validator is from jakarta.validation
    // It's the thing that reads your 
    // annotations and checks them
    private Validator validator;

    @BeforeEach
    void setUp() {
        // This creates a validator 
        // without needing Spring Boot
        ValidatorFactory factory =
            Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void whenAmountIsNull_shouldHaveViolation() {
        CreateInvoiceRequest request = 
            new CreateInvoiceRequest();
        request.setAmount(null); // null amount

        Set<ConstraintViolation<CreateInvoiceRequest>> 
            violations = validator.validate(request);

        // Check that a violation exists 
        // for the amount field
        assertThat(violations)
            .anyMatch(v -> 
                v.getPropertyPath()
                 .toString()
                 .equals("totalAmount"));
    }
}
```

Now you understood the pattern. The `Validator` reads your annotations directly — no HTTP request, no Spring context, just pure Java validation logic. Much faster to run and much simpler to write.

You rewrote your tests properly:

```java
// YOUR FINAL VERSION — after learning from Sophie
class CreateExpenseRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory =
            Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ── Amount validation ─────────────────────────

    @Test
    void whenAmountIsNull_shouldFailValidation() {
        CreateExpenseRequest request = 
            validRequest();
        request.setAmount(null);

        Set<ConstraintViolation<CreateExpenseRequest>>
            violations = validator.validate(request);

        assertThat(violations)
            .anyMatch(v ->
                v.getPropertyPath()
                 .toString().equals("amount") &&
                v.getMessage()
                 .equals("Amount is required"));
    }

    @Test
    void whenAmountIsZero_shouldFailValidation() {
        CreateExpenseRequest request = validRequest();
        request.setAmount(BigDecimal.ZERO);

        Set<ConstraintViolation<CreateExpenseRequest>>
            violations = validator.validate(request);

        assertThat(violations)
            .anyMatch(v ->
                v.getPropertyPath()
                 .toString().equals("amount"));
    }

    @Test
    void whenAmountIsNegative_shouldFailValidation() {
        CreateExpenseRequest request = validRequest();
        request.setAmount(new BigDecimal("-1.00"));

        Set<ConstraintViolation<CreateExpenseRequest>>
            violations = validator.validate(request);

        assertThat(violations)
            .anyMatch(v ->
                v.getPropertyPath()
                 .toString().equals("amount"));
    }

    @Test
    void whenAmountExceedsLimit_shouldFailValidation() {
        CreateExpenseRequest request = validRequest();
        request.setAmount(new BigDecimal("50001.00"));

        Set<ConstraintViolation<CreateExpenseRequest>>
            violations = validator.validate(request);

        assertThat(violations)
            .anyMatch(v ->
                v.getPropertyPath()
                 .toString().equals("amount") &&
                v.getMessage()
                 .contains("50,000"));
    }

    @Test
    void whenAmountIsAtLimit_shouldPassValidation() {
        CreateExpenseRequest request = validRequest();
        request.setAmount(new BigDecimal("50000.00"));

        Set<ConstraintViolation<CreateExpenseRequest>>
            violations = validator.validate(request);

        assertThat(violations)
            .noneMatch(v ->
                v.getPropertyPath()
                 .toString().equals("amount"));
    }

    @Test
    void whenAmountIsValid_shouldPassValidation() {
        CreateExpenseRequest request = validRequest();
        // 85.00 is a completely normal expense

        Set<ConstraintViolation<CreateExpenseRequest>>
            violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    // ── Date validation ───────────────────────────

    @Test
    void whenExpenseDateIsInFuture_shouldFailValidation() {
        CreateExpenseRequest request = validRequest();
        request.setExpenseDate(LocalDate.now().plusDays(1));

        Set<ConstraintViolation<CreateExpenseRequest>>
            violations = validator.validate(request);

        assertThat(violations)
            .anyMatch(v ->
                v.getPropertyPath()
                 .toString().equals("expenseDate"));
    }

    @Test
    void whenExpenseDateIsToday_shouldPassValidation() {
        CreateExpenseRequest request = validRequest();
        request.setExpenseDate(LocalDate.now());

        Set<ConstraintViolation<CreateExpenseRequest>>
            violations = validator.validate(request);

        assertThat(violations)
            .noneMatch(v ->
                v.getPropertyPath()
                 .toString().equals("expenseDate"));
    }

    // ── Helper method ─────────────────────────────

    // A fully valid request — each test
    // only changes ONE field to isolate 
    // what it's testing
    private CreateExpenseRequest validRequest() {
        CreateExpenseRequest request =
            new CreateExpenseRequest();
        request.setAmount(new BigDecimal("85.00"));
        request.setCurrency("EUR");
        request.setCategory(
            ExpenseCategory.CLIENT_ENTERTAINMENT);
        request.setExpenseDate(LocalDate.now());
        return request;
    }
}
```

One important thing you learned from Sophie's feedback on this helper method:

```
The validRequest() pattern:
─────────────────────────────
Each test changes exactly ONE thing 
from a valid baseline.

Why this matters:
─────────────────
If you start from an invalid baseline,
you don't know which violation is 
causing your test to fail.

Example of what NOT to do:
───────────────────────────
@Test
void whenAmountIsNegative_shouldFail() {
    CreateExpenseRequest request = 
        new CreateExpenseRequest();
    // Everything is null/empty PLUS 
    // amount is negative
    request.setAmount(new BigDecimal("-1.00"));
    
    violations = validator.validate(request);
    // You'll get 5 violations — 
    // null currency, null category, 
    // null date, null amount message, 
    // negative amount.
    // Your assertion for "amount" passes,
    // but for the wrong reason.
}
```

---

### Step 4: Opening the PR

You created your branch, committed your changes, and opened the PR. You wrote the PR description following the template you'd seen in the team wiki:

```
PR: EXP-187 — Add validation for expense amount

What changed:
─────────────
Added validation annotations to 
CreateExpenseRequest for the amount field:
- @NotNull: amount cannot be null
- @Positive: amount must be > 0
- @DecimalMax("50000.00"): cannot exceed €50,000
Also added @PastOrPresent to expenseDate 
and size constraints to description/merchantName.

How to test:
─────────────
Send POST /api/v1/expenses with:
1. amount: -10 → expect 400 with message 
   "Amount must be greater than 0"
2. amount: 60000 → expect 400 with message 
   "Amount cannot exceed €50,000"
3. expenseDate: tomorrow's date → 
   expect 400 with message 
   "Expense date cannot be in the future"
4. Valid request → expect 201 Created

Tests:
───────
CreateExpenseRequestTest — 
6 new unit tests, all passing.
Coverage on CreateExpenseRequest: 100%

JIRA: EXP-187
```

You felt good about it. The code was clean, the tests were thorough, the PR description was complete.

Then Elena reviewed it.

---

### Step 5: The Review — 8 Comments

Elena left 8 comments. At the time, this felt like a lot. Reading them now, every single one was correct.

Let's go through the most important ones:

---

**Comment 1 — On the `@DecimalMax` annotation:**

```java
// What you wrote:
@DecimalMax(
    value = "50000.00",
    message = "Amount cannot exceed €50,000"
)
private BigDecimal amount;
```

Elena's comment:
> *"The `€` symbol in the message string will cause issues in environments with non-UTF-8 encoding. Use 'EUR 50,000' or '50000 EUR' instead. Also — where does this 50,000 limit come from? Is it a business rule? If so, it should be a configurable value from application.properties, not hardcoded. What if the limit changes next quarter?"*

This was your first introduction to a principle you'd hear many times:

```
Don't hardcode business rules in code.
────────────────────────────────────────
Business rules change.
If the limit changes from €50,000 to €75,000:
- With hardcoded value: find every place 
  it appears in code, update, test, redeploy
- With config value: update 
  application.properties, refresh, done

The fix:
─────────
# application.properties
moss.expense.max-amount=50000.00
```

```java
// Updated DTO
@Data
@Validated
public class CreateExpenseRequest {

    // Now reads from config instead of hardcoded
    // But wait — you can't inject @Value 
    // into a DTO directly.
    // Elena explained why below.
```

Elena then explained the right approach:

```
@Value doesn't work in DTOs.
─────────────────────────────
DTOs are created by Jackson (JSON 
deserialization) — not by Spring.
Spring doesn't manage their lifecycle.
So @Autowired and @Value don't work there.

The validation constraint itself 
can't read from config.

Two options:
─────────────
Option A: Move the business rule 
          validation to the service layer
          (not the DTO layer).
          DTO validates format/type.
          Service validates business rules.

Option B: Use a custom validator class 
          that Spring DOES manage,
          which CAN read from config.

At your level (month 2), 
Option A is the right choice.
Keep it simple.
```

You updated the approach:

```java
// CreateExpenseRequest.java — FINAL version
@Data
public class CreateExpenseRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    // Removed @DecimalMax — moved to service layer
    private BigDecimal amount;

    // ... rest of fields
}
```

```java
// ExpenseService.java — business rule validation
@Service
@RequiredArgsConstructor
public class ExpenseService {

    @Value("${moss.expense.max-amount:50000.00}")
    private BigDecimal maxExpenseAmount;

    @Transactional
    public ExpenseResponse createExpense(
            CreateExpenseRequest request,
            MultipartFile receipt,
            UUID userId,
            UUID companyId) {

        // Business rule validation in service layer
        if (request.getAmount()
                .compareTo(maxExpenseAmount) > 0) {
            throw new ExpenseAmountExceededException(
                "Expense amount cannot exceed " +
                maxExpenseAmount + " EUR"
            );
        }

        // ... rest of method
    }
}
```

This taught you something fundamental about layering:

```
DTO validation (annotations):
───────────────────────────────
Format, type, null checks, size limits.
Things that are ALWAYS true regardless 
of business context.
"Amount must be a number" — always true.
"Currency must be 3 characters" — always true.

Service layer validation:
──────────────────────────
Business rules that come from policy.
Things that COULD change.
"Amount cannot exceed X" — business policy.
"This user is allowed to submit for 
 this company" — authorization logic.

This separation means:
If the limit changes, you change config.
If the format requirement changes, 
you change the annotation.
They don't step on each other.
```

---

**Comment 2 — On the test helper method naming:**

```java
// What you wrote:
private CreateExpenseRequest validRequest() {
```

Elena's comment:
> *"Good pattern. One suggestion — name it something more descriptive like `aValidExpenseRequest()` or `defaultExpenseRequest()`. The 'a' prefix is a common convention in test builders that makes it read more naturally in the test: `assertThat(validator.validate(aValidExpenseRequest())).isEmpty()` reads like English."*

Small comment. But it showed you that code readability matters even in tests — maybe especially in tests, since tests are documentation of how the code is supposed to behave.

---

**Comment 3 — On missing edge case:**

Elena's comment on your amount tests:
> *"You've tested null, zero, negative, and above limit. Good. What about `0.001` — an amount with more decimal places than a currency supports? EUR has 2 decimal places. If someone sends `85.999`, what should happen? The ticket doesn't specify this — but you should flag it to Lukas and the PM rather than silently accepting or rejecting it."*

This was not a code fix. It was a lesson about thinking beyond the ticket:

```
A ticket describes what was asked.
It doesn't describe everything 
that needs to be decided.

Your job as an engineer:
─────────────────────────
Identify the gaps.
Raise them explicitly.
Don't make silent assumptions.
Don't silently accept edge cases 
that should be a product decision.

What you did:
──────────────
Added a comment on the JIRA ticket:
"EXP-187 — Edge case not covered 
 in acceptance criteria: 
 What should happen with amounts 
 that have more than 2 decimal places?
 e.g., amount: 85.999
 Currently accepting this. 
 Is that correct per business rules?"

Lukas responded:
"Good catch. Round to 2 decimal places 
 on save. I'll update the ticket."
```

This one interaction taught you something that would define how you worked for the rest of your time there:

```
Junior engineers answer the question asked.
Good engineers notice the question 
that wasn't asked but should have been.
```

---

**Comment 4 — On the `@Size` annotation on `merchantName`:**

```java
// What you wrote:
@Size(max = 255)
private String merchantName;
```

Elena's comment:
> *"Missing `message` on this one. Every constraint annotation should have an explicit message. If this validation fails and the client gets back an error, they should know WHY. The default message from the framework is not user-friendly."*

```java
// Fixed:
@Size(max = 255,
    message = "Merchant name cannot exceed 255 characters")
private String merchantName;
```

Small fix. But it reinforced a discipline: **be explicit about error messages.** Clients consuming your API are people (or other developers). They deserve to understand what went wrong.

---

### Step 6: Fixing the Comments and the Second Review

You spent a few hours addressing all 8 comments. Some were code fixes. Some were conversations on the PR. Some resulted in JIRA updates.

You pushed the fixes. Elena reviewed again.

This time: 2 approvals (Elena + Sophie). PR merged.

```
Timeline:
──────────
Day 1 (Monday):    PR opened
Day 2 (Tuesday):   Elena's review — 8 comments
Day 2-3:           You fix all comments, 
                   ask 2 clarifying questions
                   on the PR
Day 3 (Wednesday): Elena re-reviews
Day 3:             Sophie reviews (second approval)
Day 3:             PR merged to main
Day 3:             Auto-deployed to staging
Day 4:             David (QA) tests on staging,
                   confirms acceptance criteria met
```

---

## The Result

```
What shipped:
──────────────
- Expense amount validation working 
  in production
- Business rule (max amount) now 
  configurable via application.properties
- 6 unit tests, all passing
- Edge case flagged → product decision 
  made → rounding behaviour defined

What you learned:
──────────────────
1. Read existing code before writing new code
2. DTO validates format, 
   service validates business rules
3. Business rules belong in config, 
   not hardcoded
4. Every constraint needs an explicit message
5. Tests should change one variable at a time
6. Tickets don't specify everything — 
   your job includes finding the gaps
7. 8 PR comments is not failure. 
   It's how you learn.

How you felt after:
────────────────────
Honestly? Relieved.
The 8 comments initially felt like 
"I'm not good enough."
But Elena's tone was always 
"here's why this matters" —
not "you got this wrong."
That made the difference.
By the time the PR was merged,
you felt like you understood 
something real —
not just "the task is done"
but "I understand WHY it works this way."
```

---

## The "Tricky Question" Preparation

These are real questions an interviewer might ask based on this story. You should be able to answer all of them without hesitation.

---

**Q1: "Why did you move the max-amount validation to the service layer instead of keeping it in the DTO?"**

```
Because the DTO is the wrong place 
for business rules.

DTO validation is for format and type — 
things that are always true regardless 
of context. "Amount must be a number." 
"Currency must be 3 characters."

The max-amount limit is a business policy. 
It could change — a company might want 
different limits for different employee 
roles, or the limit might be raised next 
quarter. If it's hardcoded in a DTO 
annotation, you have to find every 
place it appears and change it.

By moving it to the service layer and 
reading it from application.properties 
via @Value, changing the limit means 
changing one line in config — 
no code change, no redeploy in the 
worst case, no risk of missing a place.

And @Value doesn't work in DTOs anyway 
because DTOs are created by Jackson, 
not by Spring — Spring doesn't manage 
their lifecycle, so injection doesn't work.
```

---

**Q2: "What's the difference between `@Positive` and `@Min(1)` for validating an amount?"**

```
Both prevent zero and negative numbers 
in most cases, but there's a subtle 
difference.

@Min(1) enforces a minimum integer value 
of 1. For BigDecimal, this means 
anything less than 1.0 fails — 
including 0.50, which is a valid expense.

@Positive enforces that the value is 
strictly greater than 0. So 0.01 passes, 
0.00 fails, -0.01 fails. For a financial 
amount, @Positive is semantically correct — 
any positive number, including cents.

For an expense amount in EUR, 
you could have a €0.50 expense 
(a parking meter, for example). 
@Min(1) would wrongly reject it. 
@Positive correctly accepts it.
```

---

**Q3: "You mentioned Elena said to use `application.properties` for the limit. But what if the limit needs to be different per company — some companies allow €50,000, others allow €100,000?"**

```
That's a great question and it's 
exactly the kind of thing I should 
have thought about at the time — 
but didn't.

At month 2, the solution I implemented 
was a single global limit from config. 
That's appropriate for the scope of 
that ticket.

If the requirement were per-company limits, 
the right approach would be to store the 
limit in the database — probably in a 
company_settings table or as part of 
the approval policy that lives in 
User & Org Service.

Then in the service layer, instead of 
reading from @Value, you'd fetch the 
company's specific limit via FeignClient 
or from a cached approval policy object, 
and validate against that.

That's a more complex solution and 
would have been over-engineering for 
a ticket that explicitly said 
"€50,000 limit". The right thing was 
to implement what was asked, ship it, 
and flag that a per-company limit might 
be needed later — which I did add as 
a comment on the ticket.
```

---

**Q4: "How does `@Valid` actually trigger the validation? What happens internally?"**

```
When a request comes in and hits the 
controller method, Spring's argument 
resolver kicks in. It sees @RequestBody 
or @RequestPart and uses Jackson to 
deserialize the JSON into a Java object.

Once deserialization is done, Spring 
checks if the parameter has @Valid 
or @Validated on it. If it does, 
Spring invokes the Bean Validation 
framework — specifically it calls 
Validator.validate() on the 
deserialized object.

The Validator reads all the constraint 
annotations (@NotNull, @Positive, etc.) 
on the class's fields and evaluates them.

If any constraint is violated, the 
Validator collects all the violations 
into a ConstraintViolation set and 
Spring throws MethodArgumentNotValidException, 
passing those violations into the exception.

Then our GlobalExceptionHandler catches 
MethodArgumentNotValidException, extracts 
the field errors from the BindingResult 
inside the exception, formats them into 
our standard ErrorResponse, and returns 
a 400 Bad Request to the client.

If @Valid is missing, the deserialized 
object is passed straight to the method 
with no validation — the annotations 
on the DTO are completely ignored.
```

---

**Q5: "You said you felt 'relieved' when the PR merged. What would you do differently if you were starting that ticket today?"**

```
A few things.

First, I'd read the existing test 
patterns before writing a single test. 
I wasted an hour figuring out how to 
test validation annotations when the 
answer was already in the codebase — 
CreateInvoiceRequestTest.java was right 
there. Reading first would have saved time.

Second, I'd flag the edge cases before 
writing code, not after. The "what about 
0.001 amounts" question is something I 
could have identified by reading the 
acceptance criteria carefully before 
touching the keyboard. One message to 
Lukas before starting would have 
clarified it immediately.

Third, I'd write the PR description 
more carefully the first time. I got 
the structure right, but I didn't 
explain the decision to move max-amount 
validation to the service layer. 
Reviewers shouldn't have to guess why 
you made a decision — explain it in 
the PR description so the conversation 
is more productive.

But honestly — for month 2, 
it went as it should have gone. 
The mistakes I made were the right 
mistakes to make at that stage.
```

---

Story 1 complete.

**Story 2 next: "Onboarding — Real Codebase vs Tutorial Codebase Shock"**

This story goes deeper into what those first two weeks actually felt like technically — specifically the moment you ran the project locally for the first time, what broke, how you navigated it, and what that experience taught you about reading code in a real system. Ready?