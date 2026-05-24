# Step 6: API Design

---

## Principles First — Before Any Endpoint

Interviewers don't just want to see a list of endpoints. They want to know the *thinking* behind the design. So we establish principles first.

---

### Design Principles Our Team Follows

**1. REST — but pragmatic REST**

```
We follow REST conventions, not REST religion.

What we do:
- Nouns in URLs, not verbs
- HTTP methods carry the action meaning
- Proper status codes
- Stateless requests

What we don't do:
- Argue about HATEOAS for weeks
- Over-engineer for theoretical clients
  that don't exist yet
- Treat every minor deviation as a sin

Series B reality:
Speed of delivery matters more than
textbook API purity.
```

**2. URL Structure**

```
Base URL pattern:
/api/v1/{resource}/{id}/{sub-resource}

Examples:
/api/v1/expenses
/api/v1/expenses/{expenseId}
/api/v1/expenses/{expenseId}/approve
/api/v1/invoices
/api/v1/invoices/{invoiceId}/line-items
```

**3. Versioning**

```
Why version at all?
────────────────────
Mobile app v1.0 uses /api/v1/expenses.
We ship breaking change in API.
Mobile app v1.0 breaks for users 
who haven't updated yet.

Solution: version in URL path.
/api/v1/ stays stable.
Breaking changes go to /api/v2/.
Both run simultaneously during transition.

Why path versioning over header versioning?
────────────────────────────────────────────
Header versioning: Accept: application/vnd.moss.v2+json
Path versioning:   /api/v2/expenses

Path versioning is:
- Visible in logs and browser history
- Easier to test (just change URL)
- Easier to route at gateway level
- What most teams actually use in practice

Header versioning is "more RESTful" 
in theory. Path versioning wins in practice.
```

**4. Authentication Context**

```
Every request arrives at our service 
with these headers (added by API Gateway):
  X-User-Id:    uuid of the calling user
  X-Company-Id: uuid of their company
  X-User-Role:  EMPLOYEE / FINANCE_MANAGER / ADMIN

We NEVER ask the client to send their userId 
in the request body.
"Submit expense for employee X" — 
we get X from the header, not the body.
Prevents users from submitting 
on behalf of others.
```

**5. Standard Error Response Format**

```json
{
  "timestamp": "2025-03-15T10:30:00Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Expense amount must be greater than 0",
  "path": "/api/v1/expenses",
  "traceId": "9e7d21299f4ea8a1"
}
```

```
Why include traceId?
─────────────────────
When a client reports an error,
they give us the traceId.
We search Datadog with that traceId.
We see the exact request, logs, 
and stack trace instantly.

Without traceId: "it failed around 3pm"
→ needle in a haystack.

With traceId: one search, full picture.
```

---

## Part 1 — Expense & Reimbursement Service APIs

### Controller Structure

```
ExpenseController         → /api/v1/expenses
ReimbursementController   → /api/v1/reimbursements
```

---

### Expense Endpoints

#### POST /api/v1/expenses — Submit a new expense (draft)

```
Who calls this: Employee via mobile app
Purpose: Create expense in DRAFT state

Request:
────────
POST /api/v1/expenses
Headers:
  Authorization: Bearer <jwt>
  X-User-Id: uuid
  X-Company-Id: uuid
  Content-Type: multipart/form-data

Body (multipart — because receipt image):
  expenseData: {
    "amount": 85.00,
    "currency": "GBP",
    "category": "CLIENT_ENTERTAINMENT",
    "description": "Client lunch - Acme Corp",
    "merchantName": "The Ivy Restaurant",
    "expenseDate": "2025-03-14"
  }
  receipt: <binary image file>
```

```java
// Request DTO
@Data
public class CreateExpenseRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    @DecimalMax(value = "50000.00", 
        message = "Amount cannot exceed 50,000")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, 
        message = "Currency must be 3-letter ISO code")
    private String currency;

    @NotNull(message = "Category is required")
    private ExpenseCategory category;

    @Size(max = 500, message = "Description too long")
    private String description;

    @Size(max = 255)
    private String merchantName;

    @NotNull(message = "Expense date is required")
    @PastOrPresent(message = "Expense date cannot be in future")
    private LocalDate expenseDate;
}
```

```java
// Controller method
@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExpenseResponse> createExpense(
            @RequestPart("expenseData") 
            @Valid CreateExpenseRequest request,
            @RequestPart("receipt") MultipartFile receipt,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Company-Id") UUID companyId) {

        ExpenseResponse response = 
            expenseService.createExpense(request, receipt, 
                userId, companyId);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }
}
```

```json
Response: 201 Created
{
  "expenseId": "uuid-123",
  "status": "DRAFT",
  "amount": 85.00,
  "currency": "GBP",
  "amountInEur": 99.45,
  "exchangeRate": 1.170000,
  "category": "CLIENT_ENTERTAINMENT",
  "description": "Client lunch - Acme Corp",
  "merchantName": "The Ivy Restaurant",
  "expenseDate": "2025-03-14",
  "receiptUrl": "https://cdn.moss.com/receipts/...",
  "ocrExtractedData": {
    "detectedAmount": 85.00,
    "detectedMerchant": "The Ivy",
    "detectedDate": "2025-03-14",
    "confidence": 0.94
  },
  "createdAt": "2025-03-15T10:30:00Z"
}
```

```
Why 201 and not 200?
─────────────────────
201 Created = a new resource was created.
200 OK = request succeeded, existing resource.
POST that creates something → always 201.
POST that performs an action (like /approve) → 200.
```

---

#### PUT /api/v1/expenses/{expenseId}/submit — Submit for approval

```
Why a separate endpoint for submit?
─────────────────────────────────────
Employee creates expense (DRAFT).
Reviews OCR result.
May edit it.
When satisfied, explicitly submits.

These are two distinct business actions.
One endpoint for both would mean 
the controller has to figure out intent.
Separate endpoints = explicit intent.

Request:
────────
PUT /api/v1/expenses/{expenseId}/submit
Headers: X-User-Id, X-Company-Id
Body: empty (no additional data needed)

Response: 200 OK
{
  "expenseId": "uuid-123",
  "status": "PENDING_APPROVAL",
  "assignedApproverId": "uuid-456",
  "assignedApproverName": "Sarah Chen",
  "submittedAt": "2025-03-15T10:35:00Z"
}
```

```
What happens inside:
─────────────────────
1. Validate expense is in DRAFT state
2. Validate all required fields present
3. Fetch approval policy from User & Org Service
4. Determine approver based on policy + amount
5. @Transactional:
   - Update status → PENDING_APPROVAL
   - Set assignedApproverId
   - Set submittedAt
   - Insert into outbox_events 
     (expense.submitted event)
6. Return updated expense
```

---

#### PUT /api/v1/expenses/{expenseId}/approve — Approve an expense

```
Who calls this: Manager / Finance Manager
Authorization: @PreAuthorize check that
               caller is the assigned approver

Request:
────────
PUT /api/v1/expenses/{expenseId}/approve
Headers: X-User-Id, X-Company-Id, X-User-Role
Body:
{
  "comment": "Approved. Valid business expense."
}

Response: 200 OK
{
  "expenseId": "uuid-123",
  "status": "APPROVED",
  "approvedBy": {
    "id": "uuid-456",
    "name": "Sarah Chen"
  },
  "approvedAt": "2025-03-15T11:00:00Z",
  "comment": "Approved. Valid business expense."
}

Error cases:
─────────────
403 Forbidden  → caller is not the assigned approver
409 Conflict   → expense is not in PENDING_APPROVAL state
404 Not Found  → expenseId doesn't exist
```

```
Why 409 Conflict for wrong status?
────────────────────────────────────
400 Bad Request = client sent invalid data.
409 Conflict = request is valid but 
               conflicts with current state.

"Approving an already-approved expense"
is not a bad request — it's a conflict
with current resource state.
409 is semantically correct here.
```

---

#### PUT /api/v1/expenses/{expenseId}/reject — Reject an expense

```
Request:
────────
PUT /api/v1/expenses/{expenseId}/reject
Body:
{
  "reason": "Receipt unclear, please resubmit 
             with a legible receipt."
}

@NotBlank on reason — 
rejection without reason is not allowed.
Finance manager must explain why.

Response: 200 OK
{
  "expenseId": "uuid-123",
  "status": "REJECTED",
  "rejectedBy": { "id": "uuid-456", "name": "Sarah Chen" },
  "rejectedAt": "2025-03-15T11:05:00Z",
  "reason": "Receipt unclear..."
}
```

---

#### GET /api/v1/expenses — List expenses (with filtering & pagination)

```
This is the most complex GET endpoint.
Finance dashboard needs flexible filtering.

Request:
────────
GET /api/v1/expenses
  ?status=PENDING_APPROVAL
  &employeeId=uuid-789        (optional)
  &category=TRAVEL            (optional)
  &fromDate=2025-03-01        (optional)
  &toDate=2025-03-31          (optional)
  &minAmount=100              (optional)
  &maxAmount=5000             (optional)
  &page=0                     (default 0)
  &size=20                    (default 20, max 100)
  &sortBy=submittedAt         (default)
  &sortDir=desc               (default)
```

```java
@GetMapping
public ResponseEntity<PagedResponse<ExpenseResponse>> getExpenses(
        @RequestParam(required = false) ExpenseStatus status,
        @RequestParam(required = false) UUID employeeId,
        @RequestParam(required = false) ExpenseCategory category,
        @RequestParam(required = false) 
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @RequestParam(required = false) BigDecimal minAmount,
        @RequestParam(required = false) BigDecimal maxAmount,
        @RequestParam(defaultValue = "0") 
        @Min(0) int page,
        @RequestParam(defaultValue = "20") 
        @Min(1) @Max(100) int size,
        @RequestParam(defaultValue = "submittedAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDir,
        @RequestHeader("X-Company-Id") UUID companyId,
        @RequestHeader("X-User-Id") UUID userId,
        @RequestHeader("X-User-Role") String userRole) {

    PagedResponse<ExpenseResponse> response = 
        expenseService.getExpenses(
            companyId, userId, userRole,
            status, employeeId, category,
            fromDate, toDate, minAmount, maxAmount,
            PageRequest.of(page, size, 
                Sort.by(Sort.Direction.fromString(sortDir), sortBy))
        );

    return ResponseEntity.ok(response);
}
```

```json
Response: 200 OK
{
  "content": [
    {
      "expenseId": "uuid-123",
      "status": "PENDING_APPROVAL",
      "amount": 85.00,
      "currency": "GBP",
      "amountInEur": 99.45,
      "category": "CLIENT_ENTERTAINMENT",
      "employeeName": "John Smith",
      "submittedAt": "2025-03-15T10:35:00Z"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 143,
    "totalPages": 8,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

```
Role-based filtering logic inside service:
───────────────────────────────────────────
EMPLOYEE role:
  → Can only see their OWN expenses
  → Force filter: employeeId = userId from header
  → Ignore any employeeId in query param

FINANCE_MANAGER role:
  → Can see ALL expenses in their company
  → companyId filter applied
  → employeeId filter is optional

ADMIN role:
  → Same as FINANCE_MANAGER for now

This logic lives in the service layer,
not the controller.
Controller just passes everything to service.
```

---

#### GET /api/v1/expenses/{expenseId} — Get single expense

```json
Response: 200 OK
{
  "expenseId": "uuid-123",
  "status": "APPROVED",
  "amount": 85.00,
  "currency": "GBP",
  "amountInEur": 99.45,
  "exchangeRate": 1.170000,
  "category": "CLIENT_ENTERTAINMENT",
  "description": "Client lunch - Acme Corp",
  "merchantName": "The Ivy Restaurant",
  "expenseDate": "2025-03-14",
  "receiptUrl": "https://cdn.moss.com/receipts/...",
  "employee": {
    "id": "uuid-789",
    "name": "John Smith",
    "email": "john@acmecorp.com"
  },
  "approvalSteps": [
    {
      "stepOrder": 1,
      "approverName": "Sarah Chen",
      "action": "APPROVED",
      "comment": "Approved. Valid business expense.",
      "actedAt": "2025-03-15T11:00:00Z"
    }
  ],
  "auditLog": [
    {
      "action": "SUBMITTED",
      "performedBy": "John Smith",
      "timestamp": "2025-03-15T10:35:00Z"
    },
    {
      "action": "APPROVED",
      "performedBy": "Sarah Chen",
      "timestamp": "2025-03-15T11:00:00Z"
    }
  ],
  "submittedAt": "2025-03-15T10:35:00Z",
  "approvedAt": "2025-03-15T11:00:00Z"
}
```

---

#### GET /api/v1/expenses/{expenseId}/receipt — Get receipt download URL

```
Why not embed the image in the expense response?
─────────────────────────────────────────────────
Receipt image can be 2-5MB.
Embedding it in every GET /expenses response
would make list responses enormous.
Even single expense GET doesn't need 
the image for most use cases 
(dashboards, reports).

Instead: a separate endpoint returns 
a pre-signed S3 URL.
Client fetches image directly from S3.
Our service is not in the image transfer path.

Response: 200 OK
{
  "downloadUrl": "https://s3.amazonaws.com/moss-receipts/...?signature=...&expires=900",
  "expiresInSeconds": 900
}

Pre-signed URL expires in 15 minutes.
Secure — URL is time-limited.
Efficient — S3 serves the file, not us.
```

---

### Reimbursement Endpoints

#### GET /api/v1/reimbursements — List reimbursements

```
GET /api/v1/reimbursements
  ?status=PENDING
  &page=0
  &size=20

Response: 200 OK
{
  "content": [
    {
      "reimbursementId": "uuid-r1",
      "expenseId": "uuid-123",
      "employeeName": "John Smith",
      "amount": 85.00,
      "currency": "GBP",
      "status": "PENDING",
      "scheduledAt": "2025-03-21T00:00:00Z"
    }
  ],
  "pagination": { ... }
}
```

---

#### GET /api/v1/reimbursements/{reimbursementId} — Get single reimbursement

```json
Response: 200 OK
{
  "reimbursementId": "uuid-r1",
  "expenseId": "uuid-123",
  "employee": {
    "id": "uuid-789",
    "name": "John Smith",
    "iban": "GB29 NWBK 6016 1331 9268 19"
  },
  "amount": 85.00,
  "currency": "GBP",
  "status": "COMPLETED",
  "paymentReference": "MOSS-2025-03-21-001",
  "scheduledAt": "2025-03-21T00:00:00Z",
  "completedAt": "2025-03-21T14:23:00Z"
}
```

---

## Part 2 — Invoice & AP Service APIs

### Controller Structure

```
InvoiceController       → /api/v1/invoices
SupplierController      → /api/v1/suppliers
PaymentRunController    → /api/v1/payment-runs
```

---

### Invoice Endpoints

#### POST /api/v1/invoices — Upload a new invoice

```java
// Request DTO
@Data
public class CreateInvoiceRequest {

    @NotNull
    private UUID supplierId;

    @NotBlank
    private String invoiceNumber;

    @NotNull
    @Positive
    private BigDecimal subtotalAmount;

    @NotNull
    @PositiveOrZero
    private BigDecimal vatAmount;

    @NotNull
    @Positive
    private BigDecimal totalAmount;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    @NotNull
    @PastOrPresent
    private LocalDate invoiceDate;

    @Future
    private LocalDate dueDate;      // optional

    private String glCode;          // optional
    private UUID costCenterId;      // optional

    @Valid
    @NotEmpty
    private List<CreateLineItemRequest> lineItems;
}

@Data
public class CreateLineItemRequest {

    @NotBlank
    private String description;

    @NotNull
    @Positive
    private BigDecimal quantity;

    @NotNull
    @Positive
    private BigDecimal unitPrice;

    @NotNull
    @PositiveOrZero
    @DecimalMax("100.00")
    private BigDecimal vatRate;

    private String glCode;
}
```

```
Request:
────────
POST /api/v1/invoices
Content-Type: multipart/form-data
  invoiceData: { ...CreateInvoiceRequest... }
  document: <PDF file>

Response: 201 Created
{
  "invoiceId": "uuid-inv-1",
  "status": "PENDING_REVIEW",
  "supplierId": "uuid-sup-1",
  "supplierName": "Acme Software GmbH",
  "invoiceNumber": "INV-2025-0342",
  "totalAmount": 3200.00,
  "currency": "EUR",
  "dueDate": "2025-04-15",
  "ocrExtractedData": { ... },
  "createdAt": "2025-03-15T10:30:00Z"
}
```

---

#### PUT /api/v1/invoices/{invoiceId}/verify — Mark as verified

```
Who calls this: The assigned verifier
Purpose: Confirm goods/services were received

Request:
────────
PUT /api/v1/invoices/{invoiceId}/verify
Body:
{
  "comment": "Software license received and activated."
}

Response: 200 OK
{
  "invoiceId": "uuid-inv-1",
  "status": "VERIFIED",
  "verifiedBy": { "id": "uuid-456", "name": "Mark Evans" },
  "verifiedAt": "2025-03-16T09:00:00Z"
}
```

---

#### PUT /api/v1/invoices/{invoiceId}/approve — Approve invoice

```
Request:
────────
PUT /api/v1/invoices/{invoiceId}/approve
Body:
{
  "comment": "Approved for payment."
}

Response: 200 OK
{
  "invoiceId": "uuid-inv-1",
  "status": "APPROVED",
  "approvedBy": { "id": "uuid-789", "name": "Lisa Wang" },
  "approvedAt": "2025-03-16T10:30:00Z",
  "scheduledPaymentDate": "2025-03-21"
}
```

---

#### GET /api/v1/invoices — List invoices with filtering

```
GET /api/v1/invoices
  ?status=PENDING_APPROVAL
  &supplierId=uuid-sup-1        (optional)
  &fromDueDate=2025-03-01       (optional)
  &toDueDate=2025-03-31         (optional)
  &minAmount=500                (optional)
  &maxAmount=10000              (optional)
  &overdue=true                 (optional — 
                                 dueDate < today 
                                 AND not PAID)
  &page=0
  &size=20
  &sortBy=dueDate
  &sortDir=asc
```

```
Note on &overdue=true:
───────────────────────
This is a computed filter, not a DB column.
In the service layer:
if (overdue == true):
  add filter: dueDate < today 
              AND status NOT IN ('PAID', 'CANCELLED')

Using Specification API 
(from your Spring Data JPA notes):
Each filter condition = one Specification.
Chain with .and().
Clean, readable, maintainable.
```

---

#### GET /api/v1/invoices/{invoiceId} — Get single invoice with line items

```json
Response: 200 OK
{
  "invoiceId": "uuid-inv-1",
  "status": "APPROVED",
  "supplier": {
    "id": "uuid-sup-1",
    "name": "Acme Software GmbH",
    "taxId": "DE123456789"
  },
  "invoiceNumber": "INV-2025-0342",
  "invoiceDate": "2025-03-01",
  "dueDate": "2025-04-15",
  "subtotalAmount": 2689.08,
  "vatAmount": 510.92,
  "totalAmount": 3200.00,
  "currency": "EUR",
  "glCode": "6300",
  "lineItems": [
    {
      "lineNumber": 1,
      "description": "Annual Software License - Enterprise",
      "quantity": 1,
      "unitPrice": 2689.08,
      "vatRate": 19.00,
      "vatAmount": 510.92,
      "lineTotal": 3200.00
    }
  ],
  "approvalSteps": [
    {
      "stepOrder": 1,
      "approverName": "Lisa Wang",
      "approverRole": "FINANCE_MANAGER",
      "action": "APPROVED",
      "comment": "Approved for payment.",
      "actedAt": "2025-03-16T10:30:00Z"
    }
  ],
  "documentUrl": "https://cdn.moss.com/invoices/...",
  "createdAt": "2025-03-15T10:30:00Z"
}
```

---

### Supplier Endpoints

#### POST /api/v1/suppliers — Create a supplier

```java
@Data
public class CreateSupplierRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 50)
    private String taxId;

    @NotBlank
    @Size(min = 2, max = 2)
    private String countryCode;

    @NotBlank
    @Size(min = 3, max = 3)
    private String defaultCurrency;

    @Pattern(
        regexp = "[A-Z]{2}[0-9]{2}[A-Z0-9]{4}[0-9]{7}" +
                 "([A-Z0-9]?){0,16}",
        message = "Invalid IBAN format"
    )
    private String iban;

    @Pattern(
        regexp = "[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?",
        message = "Invalid BIC format"
    )
    private String bic;

    private String bankName;
}
```

```
Response: 201 Created
{
  "supplierId": "uuid-sup-1",
  "name": "Acme Software GmbH",
  "countryCode": "DE",
  "defaultCurrency": "EUR",
  "taxId": "DE123456789",
  "createdAt": "2025-03-15T10:00:00Z"
}

Note: IBAN is NOT returned in response.
Sensitive financial data.
Only returned when explicitly needed 
(e.g., payment run preparation).
```

#### GET /api/v1/suppliers — List suppliers

```
GET /api/v1/suppliers
  ?search=Acme          (name search, optional)
  &countryCode=DE       (optional)
  &page=0
  &size=20

Response: 200 OK (paginated list, no IBAN in list view)
```

---

### Payment Run Endpoints

#### POST /api/v1/payment-runs — Create a payment run

```
Who calls this: Finance Manager
Purpose: Group approved invoices into a batch payment

Request:
────────
POST /api/v1/payment-runs
Body:
{
  "invoiceIds": [
    "uuid-inv-1",
    "uuid-inv-2",
    "uuid-inv-3"
  ],
  "scheduledDate": "2025-03-21"
}

Validations:
─────────────
- All invoices must be in APPROVED status
- All invoices must belong to caller's company
- scheduledDate cannot be in the past

Response: 201 Created
{
  "paymentRunId": "uuid-pr-1",
  "status": "SCHEDULED",
  "invoiceCount": 3,
  "totalAmount": 8750.00,
  "currency": "EUR",
  "scheduledDate": "2025-03-21",
  "invoices": [
    { "invoiceId": "uuid-inv-1", "amount": 3200.00,
      "supplierName": "Acme Software GmbH" },
    { "invoiceId": "uuid-inv-2", "amount": 4200.00,
      "supplierName": "Cloud Hosting AG" },
    { "invoiceId": "uuid-inv-3", "amount": 1350.00,
      "supplierName": "Office Supplies BV" }
  ],
  "createdAt": "2025-03-15T10:00:00Z"
}
```

#### GET /api/v1/payment-runs/{paymentRunId} — Get payment run status

```json
Response: 200 OK
{
  "paymentRunId": "uuid-pr-1",
  "status": "COMPLETED",
  "invoiceCount": 3,
  "totalAmount": 8750.00,
  "successfulPayments": 3,
  "failedPayments": 0,
  "scheduledDate": "2025-03-21",
  "executedAt": "2025-03-21T08:00:00Z",
  "completedAt": "2025-03-21T08:15:00Z",
  "sepaFileReference": "MOSS-SEPA-20250321-001"
}
```

---

## Part 3 — Error Handling

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Validation errors
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

    // Business logic errors
    @ExceptionHandler(ExpenseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ExpenseNotFoundException ex,
            HttpServletRequest request) {

        return ResponseEntity.status(404).body(
            ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(404)
                .error("NOT_FOUND")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getCurrentTraceId())
                .build()
        );
    }

    // State conflict errors
    @ExceptionHandler(InvalidExpenseStateException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            InvalidExpenseStateException ex,
            HttpServletRequest request) {

        return ResponseEntity.status(409).body(
            ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(409)
                .error("STATE_CONFLICT")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getCurrentTraceId())
                .build()
        );
    }

    // Authorization errors
    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedActionException ex,
            HttpServletRequest request) {

        return ResponseEntity.status(403).body(
            ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(403)
                .error("FORBIDDEN")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getCurrentTraceId())
                .build()
        );
    }

    private String getCurrentTraceId() {
        // From Micrometer tracing context
        // Covered in Monitoring step
        return tracer.currentSpan() != null
            ? tracer.currentSpan().context().traceId()
            : "unknown";
    }
}
```

---

### Error Response Examples

**Validation failure:**
```json
HTTP 400
{
  "timestamp": "2025-03-15T10:30:00Z",
  "status": 400,
  "error": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "details": [
    "amount: Amount must be greater than 0",
    "currency: Currency must be 3-letter ISO code",
    "expenseDate: Expense date cannot be in future"
  ],
  "path": "/api/v1/expenses",
  "traceId": "9e7d21299f4ea8a1"
}
```

**State conflict:**
```json
HTTP 409
{
  "timestamp": "2025-03-15T11:00:00Z",
  "status": 409,
  "error": "STATE_CONFLICT",
  "message": "Cannot approve expense. 
              Current status is APPROVED. 
              Expected: PENDING_APPROVAL",
  "path": "/api/v1/expenses/uuid-123/approve",
  "traceId": "3b2a11288e3da7b2"
}
```

---

## Part 4 — API Design Decisions Interviewers Will Ask About

**Q: "Why PUT for approve/reject instead of PATCH?"**

```
PATCH = partial update to a resource's fields.
"Change the status field to APPROVED"

PUT for action = explicit business action.
"Perform the approval action on this expense"

Both are defensible. Our team chose PUT
because approval is not just 
a field update — it triggers 
a workflow, Kafka events, notifications.
It's an action, not a data change.

Some teams use POST for actions:
POST /expenses/{id}/approve
That's also fine. Consistency within 
your own API matters more than which 
one you pick.
```

**Q: "How do you handle API versioning when you have breaking changes?"**

```
Step 1: Evaluate if it's truly breaking.
Adding a new optional field → not breaking.
Adding a new required field → breaking.
Removing a field → breaking.
Changing field type → breaking.

Step 2: If breaking, create v2 endpoint.
/api/v2/expenses/{expenseId}

Step 3: Run v1 and v2 simultaneously.
Deprecate v1 (add Deprecation header).
Give clients 3-month migration window.
Then remove v1.

Step 4: At gateway level,
/api/v1/** routes to old controller.
/api/v2/** routes to new controller.
Clean separation.
```

**Q: "How do you prevent an employee from seeing another employee's expenses?"**

```
Two layers of protection:

Layer 1: Role check at service layer.
If caller has EMPLOYEE role:
  Force employeeId filter = userId from header.
  Even if they pass someone else's employeeId 
  as a query param, we ignore it.

Layer 2: Resource-level check on single GET.
GET /expenses/{expenseId}
We fetch the expense, then check:
  expense.companyId == caller's companyId
  AND (caller is FINANCE_MANAGER 
       OR expense.employeeId == caller's userId)
If not: 403 Forbidden.

Never trust the client.
Always verify at the server side.
```

**Q: "Why return `amountInEur` in the response when currency might already be EUR?"**

```
For EUR expenses: amountInEur == amount.
Slight redundancy, yes.

But the dashboard that aggregates 
monthly spend across DE, NL, UK teams
needs everything in EUR for totals.

Rather than the frontend doing currency 
conversion (using what exchange rate? 
today's? submission date's?), 
we return the pre-calculated EUR amount.

Consistency across all clients.
One source of truth for the EUR equivalent.
Exchange rate used is also returned 
for full transparency.
```

---

Step 6 complete.

**Next is Step 7: Caching Strategy** — Redis and Caffeine in our team's services, Cache-Aside pattern, TTL design, cache invalidation, and the production problems like cache stampede and avalanche. Ready?