# Public API's


<aside>
💡

**`Purpose`**: Authentication & User Management (Auth, Profiles, KYC)

</aside>

### **1.1 Authentication Endpoints**

### **POST /auth/register**

Register new user account

**Request:**

```json
{
  "email": "john@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe",
  "dateOfBirth": "1990-05-15",
  "country": "DE",
  "acceptedTerms": true
}
```

**Response: 201 Created**

```json
{
  "user": {
    "id": 123,
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "status": "active",
    "kycStatus": "pending",
    "createdAt": "2026-01-19T10:30:00Z"
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "refresh_token_here",
  "expiresIn": 604800
}
```

**Business Logic:**

1. Validate email format, password strength
2. Check if email already exists (return 409 Conflict)
3. Hash password with bcrypt (cost factor: 12)
4. Create user in PostgreSQL `users` table
5. Generate JWT token (7-day expiry)
6. Store session in Redis
7. Publish `user.registered` event to RabbitMQ (fanout to welcome email, analytics, CRM)
8. Return tokens

**Why this design:**

- Return tokens immediately (user logged in after registration)
- KYC status "pending" (user can browse, but can't invest until verified)
- JWT in response (mobile app stores for subsequent requests)

---

### **POST /auth/login**

Login existing user

**Request:**

```json
{
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Response: 200 OK**

```json
{
  "user": {
    "id": 123,
    "email": "john@example.com",
    "firstName": "John",
    "kycStatus": "verified"
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "refresh_token_here",
  "expiresIn": 604800
}
```

**Errors:**
- `401 Unauthorized`: Invalid credentials
- `403 Forbidden`: Account suspended
- `429 Too Many Requests`: Rate limit exceeded (5 attempts per 15 minutes)

**Business Logic:**
1. Query PostgreSQL for user by email
2. Compare password hash (bcrypt.compare)
3. Check user status (active, suspended, deleted)
4. Generate new JWT token
5. Store session in Redis: `session:{token_hash}`
6. Add to user's active sessions: `user:{userId}:sessions`
7. Log login event to audit_logs table
8. Publish to analytics (track login)

**Security:**
- Rate limiting per IP (prevent brute force)
- Failed login attempts tracked (lock account after 10 failures)
- Audit log every login (IP, device, timestamp)

---

#### **POST /auth/logout**
Logout user (invalidate session)

**Request Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response: 200 OK**

```json
{
  "message": "Logged out successfully"
}
```

**Business Logic:**

1. Extract token from Authorization header
2. Delete session from Redis: `DEL session:{token_hash}`
3. Remove from user's active sessions set
4. Token is now invalid (subsequent requests return 401)

**Why Redis for sessions:**

- Immediate invalidation (delete from Redis = instant logout)
- Fast validation (every request checks Redis in 1-2ms)
- Alternative: Blacklist tokens (but requires storing ALL tokens, wastes memory)

---

### **POST /auth/refresh**

Refresh access token using refresh token

**Request:**

```json
{
  "refreshToken": "refresh_token_here"
}
```

**Response: 200 OK**

```json
{
  "accessToken": "new_access_token",
  "refreshToken": "new_refresh_token",
  "expiresIn": 604800
}
```

**Why refresh tokens:**

- Access tokens expire after 7 days (security)
- Refresh tokens last 30 days (better UX, don't force login often)
- If refresh token stolen, can revoke from database

---

### **POST /auth/forgot-password**

Request password reset email

**Request:**

```json
{
  "email": "john@example.com"
}
```

**Response: 200 OK** (always, even if email not found - security)

```json
{
  "message": "If this email exists, you will receive a password reset link"
}
```

**Business Logic:**

1. Check if email exists in database
2. Generate reset token (random 32-byte hex)
3. Store in PostgreSQL: `password_reset_tokens` table (expires in 1 hour)
4. Publish notification event (send email with reset link)
5. Rate limit: max 3 requests per email per day

**Security:**

- Always return same response (don't reveal if email exists)
- Token expires after 1 hour
- Token can only be used once
- Rate limit prevents abuse

---

### **POST /auth/reset-password**

Reset password with token

**Request:**

```json
{
  "token": "abc123def456...",
  "newPassword": "NewSecurePass123!"
}
```

**Response: 200 OK**

```json
{
  "message": "Password reset successfully"
}
```

**Business Logic:**
1. Validate token exists and not expired
2. Hash new password
3. Update user's password_hash
4. Delete reset token (can only use once)
5. Invalidate all active sessions (force re-login on all devices)
6. Log password change to audit_logs

---

### **1.2 User Profile Endpoints**

#### **GET /users/me**
Get current user's profile

**Request Headers:**
```
Authorization: Bearer {token}
```

**Response: 200 OK**

```json
{
  "id": 123,
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+49123456789",
  "dateOfBirth": "1990-05-15",
  "country": "DE",
  "currency": "EUR",
  "language": "en",
  "timezone": "Europe/Berlin",
  "kycStatus": "verified",
  "kycVerifiedAt": "2026-01-10T14:20:00Z",
  "riskProfile": "moderate",
  "emailVerified": true,
  "createdAt": "2026-01-05T10:00:00Z"
}
```

**Caching:**

- Cache in Redis: `user:profile:{userId}` (TTL: 1 hour)
- Invalidate on profile update

---

### **PATCH /users/me**

Update current user's profile

**Request:**

```json
{
  "firstName": "Jonathan",
  "phone": "+49987654321",
  "language": "de",
  "timezone": "Europe/Berlin"
}
```

**Response: 200 OK**

```json
{
  "id": 123,
  "email": "john@example.com",
  "firstName": "Jonathan",
  "phone": "+49987654321",
  "language": "de",
  "timezone": "Europe/Berlin"
}
```

**Business Logic:**

1. Validate fields (phone format, timezone valid)
2. Update PostgreSQL `users` table
3. Invalidate Redis cache: `DEL user:profile:{userId}`
4. Invalidate dashboard cache: `DEL dashboard:user:{userId}`
5. Log change to audit_logs
6. Return updated profile

**Validation:**

- Phone: E.164 format validation
- Timezone: Must be valid IANA timezone
- Language: Supported languages only (en, de, fr, es, it)

---

### **POST /users/me/risk-assessment**

Complete investment risk assessment questionnaire

**Request:**

```json
{
  "answers": [
    {
      "questionId": "q1",
      "answer": "moderately_comfortable"
    },
    {
      "questionId": "q2",
      "answer": "5-10_years"
    },
    {
      "questionId": "q3",
      "answer": "50-75_percent"
    }
  ]
}
```

**Response: 200 OK**

```json
{
  "riskProfile": "moderate",
  "score": 65,
  "recommendedPortfolios": [
    {
      "type": "balanced",
      "allocation": {
        "stocks": 60,
        "bonds": 40
      },
      "description": "Balanced growth with moderate risk"
    }
  ],
  "completedAt": "2026-01-19T11:00:00Z"
}
```

**Business Logic:**

1. Calculate risk score from answers (algorithm: weighted sum)
2. Determine risk profile:
    - 0-33: Conservative
    - 34-66: Moderate
    - 67-100: Aggressive
3. Update user's `risk_profile` in PostgreSQL
4. Store assessment in `risk_assessments` table (history)
5. Return recommended portfolios based on profile

**Why this matters:**

- Regulatory requirement (MiFID II) - must assess suitability
- Determines which portfolios user can invest in
- Re-assessment every 2 years (remind user)

---

### **GET /users/me/sessions**

Get all active sessions (devices)

**Response: 200 OK**

```json
{
  "sessions": [
    {
      "sessionId": "sess_abc123",
      "deviceInfo": {
        "type": "mobile",
        "os": "iOS",
        "version": "16.2"
      },
      "ipAddress": "192.168.1.1",
      "location": "Berlin, Germany",
      "loginAt": "2026-01-19T10:30:00Z",
      "lastActivityAt": "2026-01-19T11:45:00Z",
      "current": true
    },
    {
      "sessionId": "sess_xyz789",
      "deviceInfo": {
        "type": "desktop",
        "os": "macOS",
        "version": "14.2"
      },
      "ipAddress": "192.168.1.5",
      "location": "Berlin, Germany",
      "loginAt": "2026-01-18T20:15:00Z",
      "lastActivityAt": "2026-01-18T22:30:00Z",
      "current": false
    }
  ]
}
```

**Business Logic:**

1. Get session tokens from Redis: `SMEMBERS user:{userId}:sessions`
2. For each token, get session data: `GET session:{token}`
3. Enrich with geolocation (from IP address)
4. Mark current session (compare with request token)

---

### **DELETE /users/me/sessions/:sessionId**

Logout from specific device

**Response: 200 OK**

```json
{
  "message": "Session terminated successfully"
}
```

**Business Logic:**

1. Verify sessionId belongs to current user (security)
2. Delete session from Redis
3. Remove from user's sessions set
4. Device is now logged out

---

### **DELETE /users/me/sessions**

Logout from all devices

**Response: 200 OK**

```json
{
  "message": "All sessions terminated successfully"
}
```

**Business Logic:**

1. Get all session tokens for user
2. Delete each session from Redis (batch operation)
3. Clear user's sessions set
4. User logged out everywhere (except current device if specified)

---

<aside>
💡

**`Purpose`**: Bank Accounts & Transactions (Bank sync, Balance)

</aside>

### **2.1 Bank Account Connection**

### **POST /accounts/connect**

Connect bank account via Plaid/TrueLayer

**Request:**

```json
{
  "provider": "plaid",
  "publicToken": "public-sandbox-abc123...",
  "accountId": "plaid_account_id"
}
```

**Response: 201 Created**

```json
{
  "connection": {
    "id": 456,
    "provider": "plaid",
    "bankName": "Deutsche Bank",
    "accountType": "checking",
    "accountMask": "****1234",
    "status": "active",
    "lastSyncedAt": null,
    "createdAt": "2026-01-19T12:00:00Z"
  },
  "account": {
    "id": 789,
    "accountName": "Main Checking",
    "balance": 5420.50,
    "currency": "EUR"
  }
}
```

**Business Logic:**

1. Exchange public token for access token (Plaid API call)
2. Fetch account details (balance, type, last 4 digits)
3. Encrypt access token (AES-256)
4. Store in PostgreSQL:
    - `bank_connections` table (connection metadata)
    - `accounts` table (account balance)
5. Schedule initial transaction sync (BullMQ job)
6. Publish `account.connected` event (RabbitMQ)
7. Return connection and account info

**Security:**

- Access token encrypted before storage
- Decryption key in AWS Secrets Manager
- Only background jobs can access tokens (not exposed via API)

---

### **GET /accounts**

List user's accounts

**Response: 200 OK**

```json
{
  "accounts": [
    {
      "id": 789,
      "accountType": "checking",
      "accountName": "Main Checking",
      "balance": 5420.50,
      "currency": "EUR",
      "bankConnection": {
        "id": 456,
        "bankName": "Deutsche Bank",
        "accountMask": "****1234",
        "lastSyncedAt": "2026-01-19T03:00:00Z"
      },
      "isInvestmentAccount": false
    },
    {
      "id": 790,
      "accountType": "investment",
      "accountName": "Growth Portfolio",
      "balance": 12350.75,
      "currency": "EUR",
      "bankConnection": null,
      "isInvestmentAccount": true
    }
  ],
  "totalBalance": 17771.25
}
```

**Caching:**

- Cache for 5 minutes (balances change infrequently for most users)
- Invalidate after bank sync or investment

---

### **GET /accounts/:id**

Get specific account details

**Response: 200 OK**

```json
{
  "id": 789,
  "accountType": "checking",
  "accountName": "Main Checking",
  "balance": 5420.50,
  "currency": "EUR",
  "bankConnection": {
    "id": 456,
    "bankName": "Deutsche Bank",
    "provider": "plaid",
    "accountMask": "****1234",
    "lastSyncedAt": "2026-01-19T03:00:00Z",
    "syncFrequency": "daily"
  },
  "statistics": {
    "avgMonthlyIncome": 3500.00,
    "avgMonthlyExpenses": 2800.00,
    "largestExpense": 450.00,
    "mostFrequentCategory": "food"
  },
  "createdAt": "2026-01-05T10:30:00Z"
}
```

---

### **POST /accounts/:id/sync**

Manually trigger bank account sync

**Response: 202 Accepted**

```json
{
  "message": "Account sync started",
  "jobId": "sync_job_abc123",
  "estimatedCompletion": "2026-01-19T12:05:00Z"
}
```

**Business Logic:**

1. Check if last sync was < 5 minutes ago (prevent spam)
2. Add sync job to BullMQ: `syncQueue.add('sync-account', {accountId})`
3. Return job ID (user can check status)
4. Background worker:
    - Fetch transactions from Plaid
    - Insert new transactions into PostgreSQL
    - Update account balance
    - Publish `account.synced` event
    - Invalidate caches

**Why async:**

- Plaid API can be slow (5-10 seconds)
- Don't block user request
- User sees "Syncing..." in UI, updates when complete

---

### **DELETE /accounts/:id/disconnect**

Disconnect bank account

**Response: 200 OK**

```json
{
  "message": "Bank account disconnected successfully"
}
```

**Business Logic:**
1. Mark bank_connection as `status='disconnected'`
2. Delete encrypted access token
3. Keep historical transactions (for records)
4. Stop future syncs
5. Publish `account.disconnected` event

---

### **2.2 Transactions**

#### **GET /transactions**
List user's transactions with filters

**Query Parameters:**
```
?accountId=789
&startDate=2026-01-01
&endDate=2026-01-31
&category=food
&minAmount=-500
&maxAmount=-10
&page=1
&limit=50
&sortBy=transaction_date
&sortOrder=desc
```

**Response: 200 OK**

```json
{
  "transactions": [
    {
      "id": 9876,
      "accountId": 789,
      "amount": -45.20,
      "currency": "EUR",
      "description": "Starbucks",
      "merchantName": "Starbucks",
      "category": "food",
      "subcategory": "coffee_shops",
      "transactionDate": "2026-01-18",
      "status": "posted",
      "isRecurring": false,
      "userModifiedCategory": false
    },
    {
      "id": 9875,
      "accountId": 789,
      "amount": -120.00,
      "currency": "EUR",
      "description": "REWE Supermarket",
      "merchantName": "REWE",
      "category": "food",
      "subcategory": "groceries",
      "transactionDate": "2026-01-17",
      "status": "posted",
      "isRecurring": false,
      "userModifiedCategory": false
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 50,
    "totalPages": 3,
    "totalCount": 142
  },
  "summary": {
    "totalIncome": 3500.00,
    "totalExpenses": -2845.50,
    "netCashFlow": 654.50
  }
}
```

**Performance:**

- Indexed query on (user_id, transaction_date, category)
- Query time: < 50ms for 1000s of transactions
- Pagination prevents loading too much data

---

### **GET /transactions/:id**

Get transaction details

**Response: 200 OK**

```json
{
  "id": 9876,
  "accountId": 789,
  "amount": -45.20,
  "currency": "EUR",
  "description": "Starbucks Berlin Mitte",
  "merchantName": "Starbucks",
  "category": "food",
  "subcategory": "coffee_shops",
  "transactionDate": "2026-01-18",
  "postedDate": "2026-01-19",
  "status": "posted",
  "isRecurring": false,
  "location": {
    "address": "Friedrichstraße 123, Berlin",
    "coordinates": {
      "lat": 52.5200,
      "lon": 13.4050
    }
  },
  "externalTransactionId": "plaid_txn_abc123",
  "externalCategory": "Food and Drink > Coffee Shop",
  "userModifiedCategory": false,
  "notes": null,
  "createdAt": "2026-01-19T03:05:00Z"
}
```

---

### **PATCH /transactions/:id**

Update transaction (category, notes)

**Request:**

```json
{
  "category": "entertainment",
  "subcategory": "movies",
  "notes": "Date night with Sarah"
}
```

**Response: 200 OK**

```json
{
  "id": 9876,
  "category": "entertainment",
  "subcategory": "movies",
  "notes": "Date night with Sarah",
  "userModifiedCategory": true
}
```

**Business Logic:**
1. Update transaction in PostgreSQL
2. Mark `user_modified_category = true`
3. Store in user's category preferences (ML learning)
4. Recalculate budget spending (if category changed)
5. Invalidate caches

**Why allow editing:**
- Auto-categorization can be wrong
- User knows best (Starbucks might be "business expense" not "food")
- Learn from corrections (improve future categorization)

---

#### **GET /transactions/categories**
Get transaction category statistics

**Query Parameters:**
```
?startDate=2026-01-01
&endDate=2026-01-31
```

**Response: 200 OK**

```json
{
  "period": {
    "start": "2026-01-01",
    "end": "2026-01-31"
  },
  "categories": [
    {
      "category": "food",
      "totalAmount": -650.00,
      "transactionCount": 24,
      "percentage": 23.5,
      "trend": "+5.2%",
      "topMerchants": [
        {"name": "REWE", "amount": -280.00},
        {"name": "Starbucks", "amount": -95.00}
      ]
    },
    {
      "category": "transportation",
      "totalAmount": -180.00,
      "transactionCount": 15,
      "percentage": 6.5,
      "trend": "-2.1%"
    }
  ],
  "totalExpenses": -2765.00
}
```

**Use case:**

- Visualize spending breakdown (pie chart)
- Compare to previous month (trend)
- Identify where money goes

---

<aside>
💡

**`Purpose`**: Expenses, Categories & Alerts

</aside>

### **3.1 Budget Management**

### **POST /budgets**

Create new budget

**Request:**

```json
{
  "category": "food",
  "limitAmount": 600.00,
  "currency": "EUR",
  "periodType": "monthly",
  "periodStartDate": "2026-01-01",
  "alertThreshold": 80,
  "allowRollover": false
}
```

**Response: 201 Created**

```json
{
  "id": 234,
  "userId": 123,
  "category": "food",
  "limitAmount": 600.00,
  "spentAmount": 450.00,
  "remainingAmount": 150.00,
  "percentage": 75.0,
  "currency": "EUR",
  "periodType": "monthly",
  "periodStartDate": "2026-01-01",
  "periodEndDate": "2026-01-31",
  "alertThreshold": 80,
  "alertSent": false,
  "status": "active",
  "createdAt": "2026-01-19T12:00:00Z"
}
```

**Business Logic:**
1. Validate category (from predefined list)
2. Calculate period end date (monthly: last day of month)
3. Calculate current spending from transactions
4. Insert into PostgreSQL `budgets` table
5. Check if already exceeded (send alert if needed)
6. Return budget with current progress

**Auto-calculation:**
- `spentAmount` calculated from transactions in period
- Database trigger updates on new transactions
- Real-time accuracy

---

#### **GET /budgets**
List user's budgets

**Query Parameters:**
```
?status=active
&periodStartDate=2026-01-01
```

**Response: 200 OK**

```json
{
  "budgets": [
    {
      "id": 234,
      "category": "food",
      "limitAmount": 600.00,
      "spentAmount": 650.00,
      "remainingAmount": -50.00,
      "percentage": 108.3,
      "status": "exceeded",
      "periodEndDate": "2026-01-31",
      "daysRemaining": 12
    },
    {
      "id": 235,
      "category": "transportation",
      "limitAmount": 200.00,
      "spentAmount": 120.00,
      "remainingAmount": 80.00,
      "percentage": 60.0,
      "status": "on_track",
      "periodEndDate": "2026-01-31",
      "daysRemaining": 12
    }
  ],
  "summary": {
    "totalBudget": 1500.00,
    "totalSpent": 1285.00,
    "totalRemaining": 215.00,
    "overallPercentage": 85.7,
    "budgetsExceeded": 1,
    "budgetsOnTrack": 4
  }
}
```

**Status logic:**

- `on_track`: < 80% spent
- `approaching_limit`: 80-99% spent
- `exceeded`: ≥ 100% spent

<aside>
💡

**`Purpose`**: Savings Goals (Savings, Tracking)

</aside>

### **4.1 Goal Management**

### **GET /goals**

List user's goals

**Response: 200 OK**

```json
{
  "goals": [
    {
      "id": 345,
      "name": "Emergency Fund",
      "targetAmount": 10000.00,
      "currentAmount": 3200.00,
      "progressPercentage": 32.0,
      "remainingAmount": 6800.00,
      "currency": "EUR",
      "targetDate": "2027-12-31",
      "monthsRemaining": 23,
      "status": "active",
      "onTrack": true,
      "autoSaveEnabled": true,
      "autoSaveAmount": 200.00
    },
    {
      "id": 346,
      "name": "Vacation to Italy",
      "targetAmount": 3000.00,
      "currentAmount": 1500.00,
      "progressPercentage": 50.0,
      "remainingAmount": 1500.00,
      "currency": "EUR",
      "targetDate": "2026-07-01",
      "monthsRemaining": 5,
      "status": "active",
      "onTrack": false,
      "autoSaveEnabled": false,
      "autoSaveAmount": null
    }
  ],
  "summary": {
    "totalGoals": 2,
    "totalTargetAmount": 13000.00,
    "totalCurrentAmount": 4700.00,
    "totalRemainingAmount": 8300.00,
    "overallProgress": 36.2,
    "goalsOnTrack": 1,
    "goalsCompleted": 0
  }
}

```

**"onTrack" calculation:**

```
Expected progress = (days elapsed / total days) * 100
Actual progress = (currentAmount / targetAmount) * 100

If actual >= expected: onTrack = true
Else: onTrack = false

Example (Emergency Fund):
- Started: 2026-01-01
- Target: 2027-12-31 (730 days)
- Today: 2026-01-19 (19 days elapsed)
- Expected: (19/730) * 100 = 2.6%
- Actual: 32%
- onTrack: true (way ahead!)

Example (Italy Vacation):
- Started: 2025-12-01
- Target: 2026-07-01 (213 days)
- Today: 2026-01-19 (50 days elapsed)
- Expected: (50/213) * 100 = 23.5%
- Actual: 50%
- But only 5 months left, need €300/month to finish
- If not auto-saving, might not make it

```

---

### **GET /goals/:id**

Get goal details

**Response: 200 OK**

```json
{
  "id": 345,
  "userId": 123,
  "name": "Emergency Fund",
  "description": "6 months of expenses",
  "targetAmount": 10000.00,
  "currentAmount": 3200.00,
  "remainingAmount": 6800.00,
  "progressPercentage": 32.0,
  "currency": "EUR",
  "targetDate": "2027-12-31",
  "startedAt": "2026-01-01",
  "daysElapsed": 19,
  "daysRemaining": 711,
  "monthsRemaining": 23,
  "linkedAccount": {
    "id": 790,
    "accountName": "Savings Account",
    "currentBalance": 5420.50
  },
  "autoSaveEnabled": true,
  "autoSaveAmount": 200.00,
  "autoSaveFrequency": "monthly",
  "autoSaveDayOfMonth": 1,
  "nextAutoSaveDate": "2026-02-01",
  "status": "active",
  "onTrack": true,
  "projectedCompletion": "2028-02-01",
  "milestones": [
    {
      "amount": 2500.00,
      "percentage": 25,
      "reached": true,
      "reachedAt": "2026-01-05T10:30:00Z"
    },
    {
      "amount": 5000.00,
      "percentage": 50,
      "reached": false,
      "reachedAt": null,
      "estimatedDate": "2026-08-01"
    },
    {
      "amount": 7500.00,
      "percentage": 75,
      "reached": false,
      "reachedAt": null,
      "estimatedDate": "2027-03-01"
    },
    {
      "amount": 10000.00,
      "percentage": 100,
      "reached": false,
      "reachedAt": null,
      "estimatedDate": "2028-02-01"
    }
  ],
  "contributionHistory": [
    {
      "id": 567,
      "amount": 500.00,
      "type": "manual",
      "contributedAt": "2026-01-15T14:00:00Z",
      "notes": "Bonus from work"
    },
    {
      "id": 566,
      "amount": 200.00,
      "type": "auto",
      "contributedAt": "2026-01-01T00:00:00Z",
      "notes": null
    }
  ],
  "progressChart": [
    {"date": "2026-01-01", "amount": 0.00},
    {"date": "2026-01-01", "amount": 200.00},
    {"date": "2026-01-05", "amount": 2700.00},
    {"date": "2026-01-15", "amount": 3200.00}
  ],
  "insights": {
    "averageMonthlyContribution": 160.00,
    "suggestedMonthlyAmount": 295.00,
    "message": "To reach your goal on time, consider increasing monthly contribution to €295"
  },
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-19T12:30:00Z"
}

```

**Why so detailed:**

- Progress chart for visualization (line graph in UI)
- Milestones for motivation (25%, 50%, 75%, 100%)
- Contribution history for transparency
- Insights to guide user ("save €295/month to hit target")

---

### **PATCH /goals/:id**

Update goal

**Request:**

```json
{
  "targetAmount": 15000.00,
  "targetDate": "2028-12-31",
  "autoSaveAmount": 250.00
}

```

**Response: 200 OK**

```json
{
  "id": 345,
  "name": "Emergency Fund",
  "targetAmount": 15000.00,
  "currentAmount": 3200.00,
  "progressPercentage": 21.3,
  "targetDate": "2028-12-31",
  "autoSaveAmount": 250.00,
  "projectedCompletion": "2029-12-01",
  "status": "active"
}

```

**Business Logic:**

1. Update goal in PostgreSQL
2. Recalculate progress percentage
3. Update BullMQ recurring job (new amount)
4. Recalculate projected completion date
5. Update milestones (new target = new milestones)
6. Invalidate dashboard cache

---

### **POST /goals/:id/contribute**

Manually add money to goal

**Request:**

```json
{
  "amount": 500.00,
  "notes": "Year-end bonus",
  "sourceAccountId": 789
}

```

**Response: 200 OK**

```json
{
  "contribution": {
    "id": 568,
    "goalId": 345,
    "amount": 500.00,
    "type": "manual",
    "contributedAt": "2026-01-19T13:00:00Z",
    "notes": "Year-end bonus"
  },
  "goal": {
    "id": 345,
    "currentAmount": 3700.00,
    "progressPercentage": 37.0,
    "remainingAmount": 6300.00,
    "milestoneReached": false
  }
}

```

**Business Logic:**

1. Validate amount > 0
2. Check source account has sufficient balance
3. Create ledger entry (debit source account)
4. Insert into `goal_contributions` table
5. Update goal's `current_amount`
6. Check if milestone reached (e.g., hit €5000 = 50%)
7. If milestone reached:
    - Publish notification event
    - Send push: "🎉 You reached 50% of your Emergency Fund goal!"
8. Invalidate caches

**Transaction safety:**

```sql
BEGIN;
UPDATE accounts SET balance_cents = balance_cents - 50000 WHERE id = 789;
INSERT INTO ledger (user_id, entry_type, amount_cents) VALUES (123, 'goal_contribution', -50000);
INSERT INTO goal_contributions (goal_id, amount_cents) VALUES (345, 50000);
UPDATE savings_goals SET current_amount_cents = current_amount_cents + 50000 WHERE id = 345;
COMMIT;

-- All succeed or all rollback (atomicity)

```

---

### **DELETE /goals/:id**

Delete/complete goal

**Response: 200 OK**

```json
{
  "message": "Goal deleted successfully"
}

```

**Business Logic:**

1. If goal completed (currentAmount >= targetAmount):
    - Mark `status='completed'`
    - Set `completed_at` timestamp
    - Send congratulations notification
    - Keep record (historical data)
2. If goal cancelled before completion:
    - Mark `status='cancelled'`
    - Stop auto-save recurring job (remove from BullMQ)
    - Option to transfer funds back to source account
3. Don't actually delete from database (audit trail)

---

### **POST /goals/:id/pause**

Pause auto-save contributions

**Response: 200 OK**

```json
{
  "id": 345,
  "autoSaveEnabled": false,
  "message": "Auto-save paused. You can resume anytime."
}

```

**Use case:**

- User facing temporary financial difficulty
- Pause auto-save without deleting goal
- Easy to resume later

---

### **POST /goals/:id/resume**

Resume auto-save contributions

**Response: 200 OK**

```json
{
  "id": 345,
  "autoSaveEnabled": true,
  "nextAutoSaveDate": "2026-02-01",
  "message": "Auto-save resumed"
}

```

---

<aside>
💡

**`Purpose`**: Courses, Lessons

</aside>

### **5.1 Courses**

### **GET /education/courses**

List available courses

**Query Parameters:**

```
?difficulty=beginner
&category=investing
&status=published

```

**Response: 200 OK**

```json
{
  "courses": [
    {
      "id": 101,
      "title": "Investing 101: Getting Started",
      "slug": "investing-101",
      "description": "Learn the basics of investing, from stocks to ETFs",
      "thumbnailUrl": "https://cdn.finverse.com/courses/investing-101.jpg",
      "difficulty": "beginner",
      "category": "investing",
      "estimatedDuration": 45,
      "lessonsCount": 8,
      "isFree": true,
      "requiredPlan": null,
      "completionRate": 0,
      "isEnrolled": false,
      "publishedAt": "2026-01-01T00:00:00Z"
    },
    {
      "id": 102,
      "title": "Understanding ETFs",
      "slug": "understanding-etfs",
      "description": "Deep dive into Exchange-Traded Funds",
      "thumbnailUrl": "https://cdn.finverse.com/courses/etfs.jpg",
      "difficulty": "intermediate",
      "category": "investing",
      "estimatedDuration": 60,
      "lessonsCount": 10,
      "isFree": false,
      "requiredPlan": "premium",
      "completionRate": 0,
      "isEnrolled": false,
      "publishedAt": "2026-01-10T00:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "totalPages": 2,
    "totalCount": 35
  }
}

```

**Sorting/Filtering:**

- Beginner courses shown first (most relevant for new users)
- Free courses prioritized
- User's enrolled courses at top

---

### **GET /education/courses/:slug**

Get course details

**Response: 200 OK**

```json
{
  "id": 101,
  "title": "Investing 101: Getting Started",
  "slug": "investing-101",
  "description": "Learn the basics of investing, from stocks to ETFs. This beginner-friendly course covers everything you need to know to start your investment journey.",
  "thumbnailUrl": "https://cdn.finverse.com/courses/investing-101.jpg",
  "difficulty": "beginner",
  "category": "investing",
  "estimatedDuration": 45,
  "lessonsCount": 8,
  "isFree": true,
  "requiredPlan": null,
  "publishedAt": "2026-01-01T00:00:00Z",
  "syllabus": [
    {
      "lessonId": 201,
      "order": 1,
      "title": "What is Investing?",
      "contentType": "video",
      "duration": 5,
      "isCompleted": false,
      "isLocked": false
    },
    {
      "lessonId": 202,
      "order": 2,
      "title": "Stocks vs Bonds",
      "contentType": "article",
      "duration": 8,
      "isCompleted": false,
      "isLocked": false
    },
    {
      "lessonId": 203,
      "order": 3,
      "title": "Understanding Risk",
      "contentType": "video",
      "duration": 7,
      "isCompleted": false,
      "isLocked": false
    },
    {
      "lessonId": 204,
      "order": 4,
      "title": "Quiz: Basics of Investing",
      "contentType": "quiz",
      "duration": 5,
      "isCompleted": false,
      "isLocked": true,
      "lockMessage": "Complete previous lessons to unlock"
    }
  ],
  "userProgress": {
    "isEnrolled": false,
    "completedLessons": 0,
    "progressPercentage": 0,
    "startedAt": null,
    "lastAccessedAt": null
  },
  "instructor": {
    "name": "Sarah Johnson",
    "title": "Financial Educator",
    "avatarUrl": "https://cdn.finverse.com/instructors/sarah.jpg",
    "bio": "10+ years experience in financial education"
  },
  "learningOutcomes": [
    "Understand different types of investments",
    "Learn about risk and return",
    "Know how to start investing with small amounts",
    "Understand diversification basics"
  ],
  "prerequisites": [],
  "relatedCourses": [
    {
      "id": 102,
      "title": "Understanding ETFs",
      "slug": "understanding-etfs"
    }
  ]
}

```

---

### **POST /education/courses/:id/enroll**

Enroll in course

**Response: 200 OK**

```json
{
  "message": "Successfully enrolled in course",
  "progress": {
    "courseId": 101,
    "userId": 123,
    "status": "in_progress",
    "completedLessons": 0,
    "totalLessons": 8,
    "progressPercentage": 0,
    "startedAt": "2026-01-19T14:00:00Z"
  }
}

```

**Business Logic:**

1. Check if user already enrolled (return existing progress)
2. Check course access (premium required? user has premium?)
3. Create record in PostgreSQL `user_course_progress`
4. Publish analytics event: `course.enrolled`
5. Return progress object

**Error cases:**

- `403 Forbidden`: Course requires premium, user is free tier
- `409 Conflict`: Already enrolled

---

### **5.2 Lessons**

### **GET /education/lessons/:id**

Get lesson content

**Response: 200 OK (Video Lesson)**

```json
{
  "id": 201,
  "courseId": 101,
  "title": "What is Investing?",
  "slug": "what-is-investing",
  "description": "Introduction to the concept of investing and why it matters",
  "contentType": "video",
  "videoUrl": "https://cdn.finverse.com/lessons/201/video.mp4",
  "videoDuration": 300,
  "videoThumbnail": "https://cdn.finverse.com/lessons/201/thumb.jpg",
  "transcript": "Investing is the act of committing money...",
  "order": 1,
  "estimatedMinutes": 5,
  "requiresLesson": null,
  "nextLesson": {
    "id": 202,
    "title": "Stocks vs Bonds",
    "slug": "stocks-vs-bonds"
  },
  "userProgress": {
    "status": "in_progress",
    "videoProgressSeconds": 120,
    "completedPercentage": 40,
    "startedAt": "2026-01-19T14:05:00Z",
    "lastAccessedAt": "2026-01-19T14:10:00Z"
  },
  "resources": [
    {
      "title": "Investing Glossary (PDF)",
      "url": "https://cdn.finverse.com/resources/glossary.pdf",
      "type": "pdf"
    }
  ]
}

```

**Response: 200 OK (Article Lesson)**

```json
{
  "id": 202,
  "courseId": 101,
  "title": "Stocks vs Bonds",
  "slug": "stocks-vs-bonds",
  "description": "Understanding the difference between stocks and bonds",
  "contentType": "article",
  "articleContent": {
    "mongoId": "article_abc123",
    "blocks": [
      {
        "type": "heading",
        "level": 1,
        "text": "Stocks vs Bonds: Key Differences"
      },
      {
        "type": "paragraph",
        "text": "Stocks represent ownership in a company, while bonds are loans to companies or governments."
      },
      {
        "type": "image",
        "url": "https://cdn.finverse.com/images/stocks-bonds.png",
        "alt": "Comparison chart of stocks and bonds",
        "caption": "Stocks offer higher returns but more risk"
      },
      {
        "type": "callout",
        "style": "info",
        "text": "💡 Tip: Diversify your portfolio with both stocks and bonds"
      }
    ]
  },
  "order": 2,
  "estimatedMinutes": 8,
  "requiresLesson": 201,
  "nextLesson": {
    "id": 203,
    "title": "Understanding Risk"
  },
  "userProgress": {
    "status": "not_started",
    "scrollPosition": 0,
    "completedPercentage": 0
  }
}

```

**Response: 200 OK (Quiz Lesson)**

```json
{
  "id": 204,
  "courseId": 101,
  "title": "Quiz: Basics of Investing",
  "slug": "quiz-basics",
  "description": "Test your knowledge",
  "contentType": "quiz",
  "quiz": {
    "mongoId": "quiz_xyz789",
    "passingScore": 70,
    "timeLimit": 300,
    "allowRetries": true,
    "maxAttempts": 3,
    "questions": [
      {
        "id": "q1",
        "type": "multiple_choice",
        "question": "What does ETF stand for?",
        "options": [
          {"id": "a", "text": "Exchange Traded Fund"},
          {"id": "b", "text": "European Trading Firm"},
          {"id": "c", "text": "Equity Transfer Fee"},
          {"id": "d", "text": "External Tax Form"}
        ],
        "points": 10
      },
      {
        "id": "q2",
        "type": "true_false",
        "question": "Diversification helps reduce investment risk.",
        "points": 10
      }
    ]
  },
  "order": 4,
  "estimatedMinutes": 5,
  "requiresLesson": 203,
  "userProgress": {
    "status": "not_started",
    "attempts": 0,
    "bestScore": null,
    "passed": false
  }
}

```

**Business Logic:**

1. Check if user enrolled in course (403 if not)
2. Check if lesson locked (requires previous lesson completion)
3. Fetch content from PostgreSQL (metadata) + MongoDB (content)
4. Track lesson access in `user_lesson_progress`
5. Return content based on type

**Why different content types:**

- **Video**: Best for demonstrations, visual explanations
- **Article**: Best for detailed written content, reference material
- **Quiz**: Test comprehension, reinforce learning

---

### **POST /education/lessons/:id/progress**

Update lesson progress (video playback position)

**Request:**

```json
{
  "videoProgressSeconds": 180,
  "completed": false
}

```

**Response: 200 OK**

```json
{
  "lessonId": 201,
  "videoProgressSeconds": 180,
  "completedPercentage": 60,
  "status": "in_progress"
}

```

**Business Logic:**

1. Update `user_lesson_progress` table
2. If `videoProgressSeconds` >= 90% of duration:
    - Mark lesson as completed
    - Update course progress
    - Check if course completed (all lessons done)
    - Send milestone notification if applicable
3. Store progress in PostgreSQL
4. Return updated progress

**Why track video progress:**

- Resume feature (user returns, video starts where they left off)
- Completion tracking (watched 90%+ = completed)
- Analytics (which lessons are engaging? where do users drop off?)

---

### **POST /education/lessons/:id/complete**

Mark lesson as completed (articles)

**Response: 200 OK**

```json
{
  "lessonId": 202,
  "status": "completed",
  "completedAt": "2026-01-19T14:30:00Z",
  "courseProgress": {
    "completedLessons": 2,
    "totalLessons": 8,
    "progressPercentage": 25
  },
  "nextLesson": {
    "id": 203,
    "title": "Understanding Risk",
    "slug": "understanding-risk"
  }
}

```

**Business Logic:**

1. Mark lesson as completed in `user_lesson_progress`
2. Update course progress (trigger recalculation)
3. Check for milestones (25%, 50%, 75%, 100% complete)
4. Unlock next lesson (if was previously locked)
5. Publish analytics event
6. Return updated progress + next lesson

---

### **POST /education/quizzes/:quizId/submit**

Submit quiz answers

**Request:**

```json
{
  "lessonId": 204,
  "answers": [
    {
      "questionId": "q1",
      "selectedAnswer": "a"
    },
    {
      "questionId": "q2",
      "selectedAnswer": true
    }
  ],
  "timeSpentSeconds": 245
}

```

**Response: 200 OK**

```json
{
  "attemptId": 789,
  "score": 80,
  "scorePercentage": 80,
  "passingScore": 70,
  "passed": true,
  "totalQuestions": 10,
  "correctAnswers": 8,
  "incorrectAnswers": 2,
  "timeSpent": 245,
  "results": [
    {
      "questionId": "q1",
      "yourAnswer": "a",
      "correctAnswer": "a",
      "isCorrect": true,
      "explanation": "ETF stands for Exchange Traded Fund..."
    },
    {
      "questionId": "q2",
      "yourAnswer": true,
      "correctAnswer": true,
      "isCorrect": true,
      "explanation": "Diversification spreads risk across multiple assets..."
    }
  ],
  "lessonCompleted": true,
  "courseProgress": {
    "completedLessons": 4,
    "totalLessons": 8,
    "progressPercentage": 50
  },
  "certificateEarned": false
}

```

**Business Logic:**

1. Fetch quiz from MongoDB
2. Grade answers (compare with correct answers)
3. Calculate score
4. Check if passed (score >= passingScore)
5. Store attempt in MongoDB `quiz_attempts` collection
6. If passed:
    - Mark lesson as completed
    - Update course progress
    - Unlock next lesson
7. If failed:
    - Check attempts remaining
    - If maxAttempts reached, user must wait 24 hours
8. Return detailed results with explanations

**Why show explanations:**

- Learning opportunity (understand mistakes)
- Not just testing, but teaching
- Explanations help retention

---

<aside>
💡

**`Purpose`**: Subscriptions & Payments (Billing, Plans)

</aside>

### **7.1 Subscription Management**

### **GET /subscriptions/plans**

List available subscription plans

**Response: 200 OK**

```json
{
  "plans": [
    {
      "planType": "free",
      "name": "Free",
      "price": 0.00,
      "currency": "EUR",
      "billingCycle": null,
      "features": [
        "Connect up to 2 bank accounts",
        "Basic budgeting tools",
        "Free educational courses",
        "Invest in 3 portfolio types",
        "Email support"
      ],
      "limitations": {
        "maxBankAccounts": 2,
        "maxGoals": 3,
        "prioritySupport": false
      }
    },
    {
      "planType": "premium",
      "name": "Premium",
      "price": 6.99,
      "currency": "EUR",
      "billingCycle": "monthly",
      "features": [
        "Everything in Free",
        "Unlimited bank accounts",
        "Unlimited savings goals",
        "Advanced analytics & insights",
        "Premium educational courses",
        "Tax optimization tools",
        "Priority customer support",
        "Early access to new features"
      ],
      "limitations": null,
      "savings": {
        "yearly": {
          "price": 69.99,
          "monthlyEquivalent": 5.83,
          "savingsAmount": 13.89,
          "savingsPercentage": 16.6
        }
      },
      "popular": true
    },
    {
      "planType": "family",
      "name": "Family",
      "price": 11.99,
      "currency": "EUR",
      "billingCycle": "monthly",
      "features": [
        "Everything in Premium",
        "Up to 4 family members",
        "Shared budgets & goals",
        "Family financial dashboard",
        "Kids investment accounts"
      ],
      "limitations": null,
      "maxMembers": 4
    }
  ]
}

```

---

### **GET /subscriptions/me**

Get current user's subscription

**Response: 200 OK**

```json
{
  "id": 567,
  "userId": 123,
  "planType": "premium",
  "billingCycle": "monthly",
  "price": 6.99,
  "currency": "EUR",
  "status": "active",
  "currentPeriodStart": "2026-01-01",
  "currentPeriodEnd": "2026-01-31",
  "nextBillingDate": "2026-02-01",
  "cancelAtPeriodEnd": false,
  "paymentMethod": {
    "type": "card",
    "last4": "4242",
    "brand": "Visa",
    "expiryMonth": 12,
    "expiryYear": 2028
  },
  "trialStart": null,
  "trialEnd": null,
  "createdAt": "2026-01-01T10:00:00Z"
}

```

---

### **POST /subscriptions/upgrade**

Upgrade subscription plan

**Request:**

```json
{
  "planType": "premium",
  "billingCycle": "yearly"
}

```

**Response: 200 OK**

```json
{
  "subscription": {
    "id": 567,
    "planType": "premium",
    "billingCycle": "yearly",
    "price": 69.99,
    "status": "active",
    "nextBillingDate": "2027-01-01"
  },
  "payment": {
    "amountCharged": 64.16,
    "prorationCredit": 5.83,
    "description": "Prorated upgrade from monthly to yearly"
  },
  "message": "Successfully upgraded to Premium (Yearly)"
}

```

**Business Logic:**

1. Calculate proration (unused days in current period)
2. Create Stripe subscription update
3. Charge difference (with proration credit)
4. Update PostgreSQL `subscriptions` table
5. Publish `subscription.upgraded` event
6. Return updated subscription

**Proration example:**

- User on monthly plan (€6.99), paid Jan 1
- Upgrades to yearly (€69.99) on Jan 19
- Used 19 days of monthly (19/31 = 61%)
- Unused: 39% of €6.99 = €2.73 credit
- Yearly cost: €69.99
- Immediate charge: €69.99 - €2.73 = €67.26

---

### **POST /subscriptions/cancel**

Cancel subscription (at end of period)

**Response: 200 OK**

```json
{
  "subscription": {
    "id": 567,
    "status": "active",
    "cancelAtPeriodEnd": true,
    "currentPeriodEnd": "2026-01-31",
    "message": "Your Premium subscription will end on Jan 31, 2026. You'll retain access until then."
  }
}

```

**Business Logic:**

1. Mark `cancel_at_period_end = true` in PostgreSQL
2. Update Stripe subscription (don't renew)
3. User keeps access until period end
4. On Jan 31, webhook fires → downgrade to free tier
5. Publish `subscription.cancelled` event

**Why not immediate cancellation:**

- User paid for full month (keep access)
- Better UX (less frustration)
- User might change mind (can reactivate)

---

### **POST /subscriptions/reactivate**

Reactivate cancelled subscription

**Response: 200 OK**

```json
{
  "subscription": {
    "id": 567,
    "status": "active",
    "cancelAtPeriodEnd": false,
    "nextBillingDate": "2026-02-01",
    "message": "Your Premium subscription has been reactivated and will continue on Feb 1, 2026."
  }
}

```

---

### **7.2 Payment Methods**

### **POST /payment-methods**

Add payment method

**Request:**

```json
{
  "stripePaymentMethodId": "pm_abc123def456",
  "setAsDefault": true
}

```

**Response: 201 Created**

```json
{
  "paymentMethod": {
    "id": "pm_abc123def456",
    "type": "card",
    "card": {
      "brand": "Visa",
      "last4": "4242",
      "expiryMonth": 12,
      "expiryYear": 2028
    },
    "isDefault": true,
    "createdAt": "2026-01-19T16:00:00Z"
  }
}

```

**Flow:**

1. Frontend collects card via Stripe Elements (PCI compliant)
2. Stripe returns payment method ID
3. Backend attaches to Stripe customer
4. Store reference in PostgreSQL
5. Set as default if requested

**Security:**

- Never handle raw card numbers
- Stripe handles all sensitive data
- Only store Stripe IDs (not card data)

---

### **GET /payment-methods**

List payment methods

**Response: 200 OK**

```json
{
  "paymentMethods": [
    {
      "id": "pm_abc123def456",
      "type": "card",
      "card": {
        "brand": "Visa",
        "last4": "4242",
        "expiryMonth": 12,
        "expiryYear": 2028
      },
      "isDefault": true,
      "createdAt": "2026-01-19T16:00:00Z"
    },
    {
      "id": "pm_xyz789ghi012",
      "type": "sepa_debit",
      "sepaDebit": {
        "last4": "3000",
        "bankCode": "DEXXXXXX"
      },
      "isDefault": false,
      "createdAt": "2026-01-10T12:00:00Z"
    }
  ]
}

```

---

### **DELETE /payment-methods/:id**

Remove payment method

**Response: 200 OK**

```json
{
  "message": "Payment method removed successfully"
}

```

**Validation:**

- Can't delete default payment method if active subscription exists
- Must set another as default first

---

### **7.3 Billing History**

### **GET /payments**

List payment history

**Query Parameters:**

```
?startDate=2025-01-01
&endDate=2026-01-31
&status=succeeded
&limit=20

```

**Response: 200 OK**

```json
{
  "payments": [
    {
      "id": 789,
      "amount": 6.99,
      "currency": "EUR",
      "status": "succeeded",
      "description": "Premium subscription - Monthly",
      "paymentMethod": {
        "type": "card",
        "last4": "4242"
      },
      "receiptUrl": "https://stripe.com/receipts/xyz...",
      "paidAt": "2026-01-01T10:05:00Z"
    },
    {
      "id": 788,
      "amount": 6.99,
      "currency": "EUR",
      "status": "succeeded",
      "description": "Premium subscription - Monthly",
      "paidAt": "2025-12-01T10:05:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "totalPages": 1,
    "totalCount": 12
  }
}

```

---

### **GET /payments/:id/invoice**

Download invoice PDF

**Response: 200 OK**

```
Content-Type: application/pdf
Content-Disposition: attachment; filename="invoice-2026-01.pdf"

[PDF binary data]

```

**Generation:**

- Fetch payment from PostgreSQL
- Generate PDF using template (PDFKit library)
- Include: invoice number, date, items, amount, VAT
- Cache in S3 (don't regenerate every time)

---


# Private API’s (Internal Use Only)

<aside>
💡

**`Note`**: Most investment logic is in Investment Engine service (Go), but Core API has some *read endpoints* for convenience.

</aside>

### **6.1 Portfolios (Read-Only)**

### **GET /portfolios**

List user's portfolios

**Response: 200 OK**

```json
{
  "portfolios": [
    {
      "id": 101,
      "name": "Growth Portfolio",
      "portfolioType": "growth",
      "totalValue": 12350.75,
      "totalInvested": 11000.00,
      "totalReturn": 1350.75,
      "returnPercentage": 12.28,
      "currency": "EUR",
      "status": "active",
      "createdAt": "2026-01-05T10:00:00Z"
    },
    {
      "id": 102,
      "name": "Retirement Fund",
      "portfolioType": "balanced",
      "totalValue": 5620.30,
      "totalInvested": 5000.00,
      "totalReturn": 620.30,
      "returnPercentage": 12.41,
      "currency": "EUR",
      "status": "active",
      "createdAt": "2026-01-10T14:00:00Z"
    }
  ],
  "summary": {
    "totalValue": 17971.05,
    "totalInvested": 16000.00,
    "totalReturn": 1971.05,
    "overallReturnPercentage": 12.32
  }
}

```

**Data source:**

- Fetched from PostgreSQL `portfolios` table
- Cached in Redis (15-minute TTL)
- Investment Engine updates values every 15 minutes

---

### **GET /portfolios/:id**

Get portfolio details

**Response: 200 OK**

```json
{
  "id": 101,
  "userId": 123,
  "name": "Growth Portfolio",
  "portfolioType": "growth",
  "totalValue": 12350.75,
  "totalInvested": 11000.00,
  "totalReturn": 1350.75,
  "returnPercentage": 12.28,
  "currency": "EUR",
  "status": "active",
  "targetAllocation": {
    "stocks": 70,
    "bonds": 30
  },
  "currentAllocation": {
    "stocks": 72.5,
    "bonds": 27.5
  },
  "allocationDrift": 2.5,
  "needsRebalancing": false,
  "autoRebalance": true,
  "lastRebalancedAt": "2026-01-15T02:00:00Z",
  "holdings": [
    {
      "id": 201,
      "symbol": "VUSA.L",
      "name": "Vanguard S&P 500 UCITS ETF",
      "assetType": "etf",
      "quantity": 50.5,
      "averageBuyPrice": 82.50,
      "currentPrice": 85.50,
      "totalValue": 4317.75,
      "costBasis": 4166.25,
      "unrealizedGain": 151.50,
      "unrealizedGainPercentage": 3.64,
      "allocationPercentage": 35.0
    },
    {
      "id": 202,
      "symbol": "VEUR.L",
      "name": "Vanguard FTSE Europe UCITS ETF",
      "assetType": "etf",
      "quantity": 120.0,
      "averageBuyPrice": 35.00,
      "currentPrice": 36.80,
      "totalValue": 4416.00,
      "costBasis": 4200.00,
      "unrealizedGain": 216.00,
      "unrealizedGainPercentage": 5.14,
      "allocationPercentage": 35.7
    },
    {
      "id": 203,
      "symbol": "VGOV.L",
      "name": "Vanguard Global Aggregate Bond UCITS ETF",
      "assetType": "etf",
      "quantity": 140.0,
      "averageBuyPrice": 25.00,
      "currentPrice": 23.98,
      "totalValue": 3357.20,
      "costBasis": 3500.00,
      "unrealizedGain": -142.80,
      "unrealizedGainPercentage": -4.08,
      "allocationPercentage": 27.2
    }
  ],
  "performance": {
    "today": {
      "change": 45.20,
      "changePercentage": 0.37
    },
    "1week": {
      "change": 120.50,
      "changePercentage": 0.98
    },
    "1month": {
      "change": 380.75,
      "changePercentage": 3.18
    },
    "ytd": {
      "change": 380.75,
      "changePercentage": 3.18
    },
    "1year": {
      "change": 1350.75,
      "changePercentage": 12.28
    },
    "allTime": {
      "change": 1350.75,
      "changePercentage": 12.28
    }
  },
  "createdAt": "2026-01-05T10:00:00Z",
  "updatedAt": "2026-01-19T15:00:00Z"
}

```

**Data freshness:**

- Holdings values updated every 15 minutes (market data refresh)
- Performance calculated on-the-fly from historical data
- Cached in Redis with 5-minute TTL

**Allocation drift explanation:**

- Target: 70% stocks, 30% bonds
- Current: 72.5% stocks, 27.5% bonds
- Drift: 2.5% (within acceptable 5% threshold)
- `needsRebalancing: false` (drift < 5%)
- If drift > 5%, automatic rebalancing triggers on next Saturday 2 AM

---

### **GET /portfolios/:id/history**

Get portfolio value history for charting

**Query Parameters:**

```
?period=1year
&interval=day

```

**Response: 200 OK**

```json
{
  "portfolioId": 101,
  "period": "1year",
  "interval": "day",
  "dataPoints": [
    {
      "date": "2025-01-19",
      "value": 11000.00,
      "invested": 11000.00,
      "return": 0.00,
      "returnPercentage": 0.00
    },
    {
      "date": "2025-02-19",
      "value": 11150.00,
      "invested": 11000.00,
      "return": 150.00,
      "returnPercentage": 1.36
    },
    {
      "date": "2025-03-19",
      "value": 11280.00,
      "invested": 11000.00,
      "return": 280.00,
      "returnPercentage": 2.55
    },
    {
      "date": "2026-01-19",
      "value": 12350.75,
      "invested": 11000.00,
      "return": 1350.75,
      "returnPercentage": 12.28
    }
  ],
  "summary": {
    "startValue": 11000.00,
    "endValue": 12350.75,
    "totalReturn": 1350.75,
    "returnPercentage": 12.28,
    "highestValue": 12450.00,
    "lowestValue": 10850.00
  }
}

```

**Use case:**

- Line chart in mobile app showing portfolio growth
- Compare performance over different periods
- Visualize volatility (high/low points)

**Performance optimization:**

- Pre-calculated daily snapshots (stored in database)
- Redis caching (1-hour TTL)
- Aggregated by interval (day, week, month)

---

### **6.2 Orders (Write Operations Delegated)**

### **POST /orders**

Create investment order (proxies to Investment Engine)

**Request:**

```json
{
  "portfolioId": 101,
  "amount": 500.00,
  "currency": "EUR",
  "sourceAccountId": 789
}

```

**Response: 201 Created**

```json
{
  "orderId": 456,
  "portfolioId": 101,
  "amount": 500.00,
  "currency": "EUR",
  "status": "pending",
  "estimatedCompletion": "2026-01-19T15:10:00Z",
  "createdAt": "2026-01-19T15:05:00Z"
}

```

**Flow:**

1. Core API validates request (amount > 0, account has balance)
2. Checks user KYC status (must be verified to invest)
3. Creates order record in PostgreSQL (status: pending)
4. Publishes `order.created` event to RabbitMQ
5. Investment Engine picks up event (calculates allocation)
6. Transaction Service executes trades
7. User receives notifications when complete

**Why async:**

- Order processing takes 5-30 seconds (market data, broker API)
- User shouldn't wait (poor mobile experience)
- Return immediately with order ID
- User can check status or get notified

---

### **GET /orders**

List user's orders

**Query Parameters:**

```
?status=completed
&portfolioId=101
&startDate=2026-01-01
&limit=20

```

**Response: 200 OK**

```json
{
  "orders": [
    {
      "id": 456,
      "portfolioId": 101,
      "portfolioName": "Growth Portfolio",
      "orderType": "buy",
      "amount": 500.00,
      "currency": "EUR",
      "status": "completed",
      "allocation": [
        {
          "symbol": "VUSA.L",
          "amount": 350.00,
          "quantity": 4.09,
          "price": 85.50
        },
        {
          "symbol": "VGOV.L",
          "amount": 150.00,
          "quantity": 6.26,
          "price": 23.98
        }
      ],
      "totalFees": 1.50,
      "createdAt": "2026-01-19T15:05:00Z",
      "executedAt": "2026-01-19T15:08:23Z"
    },
    {
      "id": 455,
      "portfolioId": 101,
      "portfolioName": "Growth Portfolio",
      "orderType": "buy",
      "amount": 1000.00,
      "currency": "EUR",
      "status": "completed",
      "executedAt": "2026-01-15T10:22:15Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "totalPages": 2,
    "totalCount": 35
  }
}

```

---

### **GET /orders/:id**

Get order details

**Response: 200 OK**

```json
{
  "id": 456,
  "userId": 123,
  "portfolioId": 101,
  "portfolioName": "Growth Portfolio",
  "orderType": "buy",
  "amount": 500.00,
  "currency": "EUR",
  "status": "completed",
  "allocation": [
    {
      "symbol": "VUSA.L",
      "name": "Vanguard S&P 500 UCITS ETF",
      "amount": 350.00,
      "quantity": 4.09,
      "executionPrice": 85.50,
      "totalCost": 349.70,
      "brokerOrderId": "broker_order_abc123"
    },
    {
      "symbol": "VGOV.L",
      "name": "Vanguard Global Aggregate Bond UCITS ETF",
      "amount": 150.00,
      "quantity": 6.26,
      "executionPrice": 23.98,
      "totalCost": 150.10,
      "brokerOrderId": "broker_order_xyz789"
    }
  ],
  "fees": {
    "platformFee": 1.50,
    "brokerFee": 0.00,
    "totalFees": 1.50
  },
  "timeline": [
    {
      "status": "pending",
      "timestamp": "2026-01-19T15:05:00Z",
      "message": "Order received"
    },
    {
      "status": "processing",
      "timestamp": "2026-01-19T15:05:02Z",
      "message": "Calculating allocation"
    },
    {
      "status": "executing",
      "timestamp": "2026-01-19T15:06:00Z",
      "message": "Executing trades"
    },
    {
      "status": "completed",
      "timestamp": "2026-01-19T15:08:23Z",
      "message": "All trades executed successfully"
    }
  ],
  "createdAt": "2026-01-19T15:05:00Z",
  "executedAt": "2026-01-19T15:08:23Z"
}

```

**Timeline feature:**

- Real-time status updates (via WebSocket or polling)
- Shows user exactly what's happening
- Builds trust (transparency)

---

### **8.1 Dashboard**

### **GET /dashboard**

Get user's main dashboard data

**Response: 200 OK**

```json
{
  "user": {
    "id": 123,
    "firstName": "John",
    "email": "john@example.com",
    "kycStatus": "verified",
    "memberSince": "2026-01-05"
  },
  "accounts": {
    "totalBalance": 17771.25,
    "bankAccounts": [
      {
        "id": 789,
        "accountName": "Main Checking",
        "balance": 5420.50,
        "lastSynced": "2026-01-19T03:00:00Z"
      }
    ],
    "investmentAccounts": [
      {
        "id": 790,
        "accountName": "Growth Portfolio",
        "balance": 12350.75,
        "returnPercentage": 12.28
      }
    ]
  },
  "netWorth": {
    "current": 17771.25,
    "change": {
      "amount": 1450.75,
      "percentage": 8.9,
      "period": "month"
    },
    "trend": "up"
  },
  "budgets": {
    "summary": {
      "totalBudget": 1500.00,
      "totalSpent": 1285.00,
      "remainingAmount": 215.00,
      "overallPercentage": 85.7
    },
    "topCategories": [
      {
        "category": "food",
        "spent": 650.00,
        "limit": 600.00,
        "percentage": 108.3,
        "status": "exceeded"
      },
      {
        "category": "transportation",
        "spent": 120.00,
        "limit": 200.00,
        "percentage": 60.0,
        "status": "on_track"
      }
    ],
    "alerts": [
      {
        "type": "budget_exceeded",
        "category": "food",
        "message": "You've exceeded your food budget by €50"
      }
    ]
  },
  "goals": {
    "activeGoals": 2,
    "totalTargetAmount": 13000.00,
    "totalCurrentAmount": 4700.00,
    "overallProgress": 36.2,
    "topGoals": [
      {
        "id": 345,
        "name": "Emergency Fund",
        "progress": 32.0,
        "onTrack": true
      },
      {
        "id": 346,
        "name": "Vacation to Italy",
        "progress": 50.0,
        "onTrack": false
      }
    ]
  },
  "recentActivity": [
    {
      "id": 9876,
      "type": "transaction",
      "description": "Starbucks",
      "amount": -45.20,
      "date": "2026-01-18"
    },
    {
      "id": 456,
      "type": "investment",
      "description": "Investment order completed",
      "amount": 500.00,
      "date": "2026-01-19"
    }
  ],
  "insights": [
    {
      "type": "spending_pattern",
      "title": "High coffee shop spending",
      "message": "You spent €95 on coffee this month, 15% more than usual",
      "actionable": true,
      "action": "Set a coffee budget?"
    },
    {
      "type": "investment_milestone",
      "title": "Portfolio milestone reached",
      "message": "Your Growth Portfolio just passed €12,000!",
      "actionable": false
    }
  ],
  "upcomingEvents": [
    {
      "type": "auto_save",
      "description": "Emergency Fund auto-save",
      "amount": 200.00,
      "date": "2026-02-01"
    },
    {
      "type": "bill_due",
      "description": "Premium subscription renewal",
      "amount": 6.99,
      "date": "2026-02-01"
    }
  ]
}

```

**Caching strategy:**

- Cache entire response in Redis: `dashboard:user:{userId}`
- TTL: 5 minutes
- Invalidate on any data change (transaction, investment, budget update)

**Performance:**

- Cache hit: 2-5ms response time
- Cache miss: 100-150ms (aggregates from 5+ tables)
- Most users hit cache (dashboard loaded multiple times per session)

---

**Note**: These are internal APIs used by Notification Service, not exposed to users directly.

### **POST /internal/notifications/send**

Send notification (called by Notification Service)

**Request:**

```json
{
  "userId": 123,
  "type": "budget_exceeded",
  "channels": ["email", "push"],
  "data": {
    "category": "food",
    "spent": 650.00,
    "limit": 600.00,
    "percentage": 108.3
  }
}

```

**Response: 202 Accepted**

```json
{
  "notificationId": "notif_abc123",
  "status": "queued",
  "message": "Notification queued for delivery"
}

```

---

<aside>
💡

`Note`: Admin endpoints require special authentication (admin role).

</aside>

### **GET /admin/users/:id**

View user details (admin only)

**Response: 200 OK**

```json
{
  "user": {
    "id": 123,
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "kycStatus": "verified",
    "status": "active",
    "createdAt": "2026-01-05T10:00:00Z"
  },
  "accounts": [...],
  "subscriptions": [...],
  "recentOrders": [...],
  "auditLog": [
    {
      "action": "user.login",
      "timestamp": "2026-01-19T15:00:00Z",
      "ipAddress": "192.168.1.1"
    }
  ]
}

```

---

### **PATCH /admin/users/:id/kyc**

Manually approve/reject KYC

**Request:**

```json
{
  "kycStatus": "verified",
  "notes": "Documents verified manually"
}

```

**Response: 200 OK**

```json
{
  "userId": 123,
  "kycStatus": "verified",
  "kycVerifiedAt": "2026-01-19T16:00:00Z",
  "message": "KYC status updated"
}

```

**Business Logic:**

1. Update user's `kyc_status` in PostgreSQL
2. Set `kyc_verified_at` timestamp
3. Store admin notes in `kyc_verifications` table
4. Log action in `audit_logs`
5. Publish `user.kyc.verified` event (send notification to user)
6. Return updated status

---