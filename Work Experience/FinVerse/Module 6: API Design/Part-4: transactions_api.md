Base URL: `https://api.finverse.com/api/v1/transactions`

**Purpose**: Execute investment orders, manage financial ledger, handle settlements. Critical path for money movement - requires ACID guarantees, idempotency, and audit trails.

**Technology Choice**: Go is used because:

- High-performance concurrent transaction processing
- Strong type safety (critical for financial operations)
- Excellent error handling (no silent failures with money)
- Built-in concurrency primitives for parallel order execution
- Low latency for time-sensitive trades

**Security Principles:**

- All operations are idempotent (can safely retry)
- Every money movement logged in immutable ledger
- Database transactions ensure atomicity (all succeed or all fail)
- Comprehensive audit trail for regulatory compliance

---

# 🔗Endpoint 1: Execute Order

## **POST /orders/:id/execute**

Execute calculated investment order with broker

**Authentication**: Internal service-to-service (called after Investment Engine calculates allocation)

**Request:**

```json
{
  "orderId": 456,
  "portfolioId": 101,
  "userId": 123,
  "sourceAccountId": 789,
  "allocation": [
    {
      "symbol": "VUSA.L",
      "quantity": 4.09,
      "amount": 349.70
    },
    {
      "symbol": "VGOV.L",
      "quantity": 6.26,
      "amount": 150.10
    }
  ],
  "totalAmount": 499.80,
  "platformFee": 1.50,
  "idempotencyKey": "order_456_exec_1737285602"
}

```

**Response: 200 OK**

```json
{
  "orderId": 456,
  "status": "completed",
  "executions": [
    {
      "executionId": "exec_001",
      "symbol": "VUSA.L",
      "quantity": 4.09,
      "executionPrice": 85.52,
      "totalCost": 349.78,
      "brokerOrderId": "broker_abc123",
      "executedAt": "2026-01-19T15:08:15.234Z"
    },
    {
      "executionId": "exec_002",
      "symbol": "VGOV.L",
      "quantity": 6.26,
      "executionPrice": 23.97,
      "totalCost": 150.05,
      "brokerOrderId": "broker_xyz789",
      "executedAt": "2026-01-19T15:08:18.567Z"
    }
  ],
  "summary": {
    "totalInvested": 499.83,
    "platformFee": 1.50,
    "totalDeducted": 501.33,
    "remainingBalance": 4919.17
  },
  "ledgerEntries": [
    {
      "id": 1001,
      "type": "investment_debit",
      "amount": -501.33,
      "description": "Investment order #456"
    }
  ],
  "completedAt": "2026-01-19T15:08:20.123Z",
  "processingTimeMs": 5123
}

```

**Detailed Business Logic:**

### **Step 1: Idempotency Check**

```go
// Check Redis for previous execution
previousResult, err := redis.Get(ctx, idempotencyKey)
if err == nil && previousResult != "" {
    // Already processed - return cached result
    log.Info("Duplicate request detected, returning cached result")
    return json.Unmarshal(previousResult)
}

// Store processing flag to prevent concurrent execution
acquired, err := redis.SetNX(ctx, idempotencyKey+":lock", "processing", 30*time.Second)
if !acquired {
    return errors.New("Order already being processed")
}

```

**Why idempotency is critical:**

- Network issues cause retries
- Same order might be sent twice
- Must not execute duplicate trades (user loses money)
- Redis lock prevents concurrent processing

---

### **Step 2: Database Transaction (BEGIN)**

```go
tx, err := db.BeginTx(ctx, &sql.TxOptions{
    Isolation: sql.LevelSerializable,
})
if err != nil {
    return err
}
defer tx.Rollback() // Rollback if anything fails

// All subsequent operations happen inside this transaction
// Either ALL succeed, or ALL rollback (atomicity)

```

**Why serializable isolation:**

- Highest isolation level (prevents race conditions)
- If two orders try to debit same account simultaneously, one waits
- Prevents overdraft (account balance can't go negative)

---

### **Step 3: Validate Account Balance**

```go
account, err := tx.QueryRow(`
    SELECT id, balance_cents, status
    FROM accounts
    WHERE id = $1 AND user_id = $2
    FOR UPDATE  -- Lock this row until transaction commits
`, sourceAccountId, userId).Scan(&account)

if err != nil {
    return errors.New("Account not found")
}

if account.Status != "active" {
    return errors.New("Account is not active")
}

totalDeduction := (totalAmount + platformFee) * 100 // Convert to cents
if account.BalanceCents < totalDeduction {
    return errors.New("Insufficient funds")
}

```

**FOR UPDATE clause:**

- Locks account row until transaction commits
- Other transactions trying to access this account will wait
- Prevents double-spending

---

### **Step 4: Debit Source Account**

```go
result, err := tx.Exec(`
    UPDATE accounts
    SET balance_cents = balance_cents - $1,
        updated_at = NOW()
    WHERE id = $2
`, totalDeduction, sourceAccountId)

if err != nil {
    return errors.Wrap(err, "Failed to debit account")
}

rowsAffected, _ := result.RowsAffected()
if rowsAffected == 0 {
    return errors.New("Account debit failed - concurrent modification")
}

```

---

### **Step 5: Record in Ledger (Immutable Audit Trail)**

```go
// Ledger entry for investment debit
ledgerEntryId, err := tx.Exec(`
    INSERT INTO ledger (
        user_id,
        account_id,
        entry_type,
        amount_cents,
        balance_after_cents,
        reference_type,
        reference_id,
        description,
        created_by
    ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
`,
    userId,
    sourceAccountId,
    "investment_debit",
    -totalDeduction,
    account.BalanceCents - totalDeduction,
    "order",
    orderId,
    fmt.Sprintf("Investment order #%d", orderId),
    "system",
)

// Ledger entry for platform fee
_, err = tx.Exec(`
    INSERT INTO ledger (
        user_id,
        account_id,
        entry_type,
        amount_cents,
        reference_type,
        reference_id,
        description
    ) VALUES ($1, $2, $3, $4, $5, $6, $7)
`,
    userId,
    sourceAccountId,
    "fee_debit",
    -150, // €1.50
    "order",
    orderId,
    fmt.Sprintf("Platform fee for order #%d", orderId),
)

```

**Ledger importance:**

- Immutable record of every money movement
- Regulatory requirement (audit trail)
- Can reconstruct account balance at any point in time
- PostgreSQL rules prevent UPDATE/DELETE on ledger table

---

### **Step 6: Execute Trades with Broker (Parallel)**

```go
// Use goroutines to execute trades in parallel
var wg sync.WaitGroup
executions := make(chan Execution, len(allocation))
errors := make(chan error, len(allocation))

for _, trade := range allocation {
    wg.Add(1)

    go func(t Trade) {
        defer wg.Done()

        // Call external broker API
        execution, err := executeTrade(t)
        if err != nil {
            errors <- err
            return
        }

        executions <- execution
    }(trade)
}

// Wait for all trades to complete
wg.Wait()
close(executions)
close(errors)

// Check for any errors
if len(errors) > 0 {
    // At least one trade failed
    firstError := <-errors

    // Rollback database transaction
    tx.Rollback()

    return errors.Wrap(firstError, "Trade execution failed")
}

// Collect all successful executions
executionResults := []Execution{}
for exec := range executions {
    executionResults = append(executionResults, exec)
}

```

**Parallel execution benefits:**

- Execute 2 trades in ~2 seconds instead of 4 seconds
- Broker API is I/O bound (waiting for response)
- Go goroutines are lightweight (can spawn thousands)

**Broker API integration:**

```go
func executeTrade(trade Trade) (Execution, error) {
    // Real broker API call (e.g., Interactive Brokers, Alpaca)
    resp, err := brokerClient.PlaceOrder(BrokerOrder{
        Symbol:   trade.Symbol,
        Quantity: trade.Quantity,
        OrderType: "market",
        Side:     "buy",
        TimeInForce: "day",
    })

    if err != nil {
        return Execution{}, err
    }

    // Poll for fill status (trades aren't instant)
    for i := 0; i < 30; i++ {
        time.Sleep(100 * time.Millisecond)

        status, err := brokerClient.GetOrderStatus(resp.OrderID)
        if err != nil {
            return Execution{}, err
        }

        if status.Status == "filled" {
            return Execution{
                ExecutionID: generateID(),
                Symbol: trade.Symbol,
                Quantity: status.FilledQuantity,
                ExecutionPrice: status.AveragePrice,
                TotalCost: status.FilledQuantity * status.AveragePrice,
                BrokerOrderID: resp.OrderID,
                ExecutedAt: status.FilledAt,
            }, nil
        }

        if status.Status == "rejected" {
            return Execution{}, errors.New("Order rejected by broker")
        }
    }

    return Execution{}, errors.New("Order timeout - not filled within 3 seconds")
}

```

**Error scenarios:**

- **Broker API down**: Retry with exponential backoff (3 attempts)
- **Insufficient margin**: Rollback entire order
- **Symbol not found**: Rollback entire order
- **Partial fill**: Accept partial, refund difference to user

---

### **Step 7: Record Order Executions**

```go
for _, exec := range executionResults {
    _, err := tx.Exec(`
        INSERT INTO order_executions (
            order_id,
            symbol,
            quantity,
            price_cents,
            total_amount_cents,
            broker_order_id,
            broker_name,
            executed_at
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
    `,
        orderId,
        exec.Symbol,
        exec.Quantity,
        int64(exec.ExecutionPrice * 100),
        int64(exec.TotalCost * 100),
        exec.BrokerOrderID,
        "interactive_brokers",
        exec.ExecutedAt,
    )

    if err != nil {
        return errors.Wrap(err, "Failed to record execution")
    }
}

```

---

### **Step 8: Update Portfolio Holdings**

```go
for _, exec := range executionResults {
    // Check if holding already exists
    var existingHolding Holding
    err := tx.QueryRow(`
        SELECT id, quantity, average_buy_price_cents, total_cost_basis_cents
        FROM holdings
        WHERE portfolio_id = $1 AND symbol = $2
    `, portfolioId, exec.Symbol).Scan(
        &existingHolding.ID,
        &existingHolding.Quantity,
        &existingHolding.AverageBuyPrice,
        &existingHolding.TotalCostBasis,
    )

    if err == sql.ErrNoRows {
        // New holding - insert
        _, err = tx.Exec(`
            INSERT INTO holdings (
                portfolio_id,
                user_id,
                asset_type,
                symbol,
                asset_name,
                quantity,
                average_buy_price_cents,
                total_cost_basis_cents,
                first_purchased_at
            ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, NOW())
        `,
            portfolioId,
            userId,
            "etf",
            exec.Symbol,
            exec.AssetName,
            exec.Quantity,
            int64(exec.ExecutionPrice * 100),
            int64(exec.TotalCost * 100),
        )
    } else {
        // Existing holding - update (calculate new average price)
        newQuantity := existingHolding.Quantity + exec.Quantity
        newCostBasis := existingHolding.TotalCostBasis + int64(exec.TotalCost * 100)
        newAveragePrice := newCostBasis / int64(newQuantity * 100)

        _, err = tx.Exec(`
            UPDATE holdings
            SET quantity = $1,
                average_buy_price_cents = $2,
                total_cost_basis_cents = $3,
                updated_at = NOW()
            WHERE id = $4
        `, newQuantity, newAveragePrice, newCostBasis, existingHolding.ID)
    }

    if err != nil {
        return errors.Wrap(err, "Failed to update holdings")
    }
}

```

**Average buy price calculation:**

```
Example:
Existing: 10 shares at €80 = €800 cost basis
New purchase: 5 shares at €90 = €450 cost basis

New total: 15 shares, €1250 cost basis
New average: €1250 / 15 = €83.33 per share

```

---

### **Step 9: Update Order Status**

```go
_, err = tx.Exec(`
    UPDATE orders
    SET status = 'completed',
        executed_at = NOW(),
        execution_details = $1::jsonb,
        updated_at = NOW()
    WHERE id = $2
`, executionDetailsJSON, orderId)

if err != nil {
    return errors.Wrap(err, "Failed to update order status")
}

```

---

### **Step 10: Update Portfolio Value**

```go
// Recalculate total portfolio value
_, err = tx.Exec(`
    UPDATE portfolios
    SET total_value_cents = (
        SELECT SUM(quantity * current_price_cents)
        FROM holdings
        WHERE portfolio_id = $1
    ),
    total_invested_cents = total_invested_cents + $2,
    updated_at = NOW()
    WHERE id = $1
`, portfolioId, int64(totalAmount * 100))

if err != nil {
    return errors.Wrap(err, "Failed to update portfolio value")
}

```

---

### **Step 11: Commit Transaction**

```go
err = tx.Commit()
if err != nil {
    return errors.Wrap(err, "Transaction commit failed")
}

log.Info("Order executed successfully",
    "orderId", orderId,
    "executions", len(executionResults),
    "totalCost", totalAmount,
)

```

**At this point:**

- ✅ Money deducted from account
- ✅ Trades executed with broker
- ✅ Holdings updated
- ✅ Ledger entries created
- ✅ Order marked complete
- All changes are permanent (committed to database)

---

### **Step 12: Post-Commit Actions (Async)**

```go
// Cache result for idempotency
redis.Set(ctx, idempotencyKey, result, 24*time.Hour)

// Invalidate related caches
redis.Del(ctx,
    fmt.Sprintf("dashboard:user:%d", userId),
    fmt.Sprintf("portfolio:live:%d", portfolioId),
    fmt.Sprintf("account:balance:%d", sourceAccountId),
)

// Publish completion event to RabbitMQ
event := OrderCompletedEvent{
    OrderID: orderId,
    UserID: userId,
    PortfolioID: portfolioId,
    TotalAmount: totalAmount,
    Executions: executionResults,
}

rabbitmq.Publish("orders_exchange", "order.completed", event)
// Triggers:
// - Notification Service → sends email/push
// - Analytics Service → logs completion
// - Core API → updates any in-memory state

```

---

### **Error Handling Summary**

| Error | Action | User Impact |
| --- | --- | --- |
| Insufficient funds | Return 400, no database changes | Order rejected immediately |
| Account locked | Return 403, no database changes | User must contact support |
| Broker API timeout | Retry 3 times, then fail | Order marked failed, money refunded |
| Partial fill | Accept partial, refund difference | User gets partial investment + refund |
| Database deadlock | Retry entire transaction | Transparent to user (auto-retry) |
| Duplicate request | Return cached result | Idempotent (safe to retry) |

---

# 🔗Endpoint 2: Sell Holdings

## **POST /orders/:id/execute-sell**

Sell ETF holdings and credit account

**Request:**

```json
{
  "orderId": 457,
  "portfolioId": 101,
  "userId": 123,
  "destinationAccountId": 789,
  "sellInstructions": [
    {
      "holdingId": 201,
      "symbol": "VUSA.L",
      "quantity": 10.5,
      "reason": "user_initiated"
    }
  ],
  "idempotencyKey": "order_457_exec_1737285700"
}

```

**Response: 200 OK**

```json
{
  "orderId": 457,
  "status": "completed",
  "executions": [
    {
      "executionId": "exec_003",
      "symbol": "VUSA.L",
      "quantity": 10.5,
      "executionPrice": 85.75,
      "totalProceeds": 900.38,
      "brokerOrderId": "broker_def456",
      "executedAt": "2026-01-19T16:15:20.123Z"
    }
  ],
  "taxImplications": {
    "costBasis": 866.25,
    "proceeds": 900.38,
    "capitalGain": 34.13,
    "holdingPeriod": "long_term",
    "estimatedTax": 8.53
  },
  "summary": {
    "totalProceeds": 900.38,
    "platformFee": 0.00,
    "netCredited": 900.38,
    "newBalance": 6320.55
  },
  "completedAt": "2026-01-19T16:15:22.456Z"
}

```

**Business Logic (Similar to Buy, with Differences):**

1. **Validate Holdings Exist**

    ```go
    holding, err := tx.QueryRow(`
        SELECT id, quantity, average_buy_price_cents, total_cost_basis_cents
        FROM holdings
        WHERE id = $1 AND user_id = $2
        FOR UPDATE
    `, holdingId, userId).Scan(&holding)
    
    if holding.Quantity < quantityToSell {
        return errors.New("Insufficient holdings to sell")
    }
    
    ```

2. **Execute Sell Order with Broker**

    ```go
    execution, err := brokerClient.PlaceOrder(BrokerOrder{
        Symbol: symbol,
        Quantity: quantity,
        OrderType: "market",
        Side: "sell",  // Difference: sell instead of buy
        TimeInForce: "day",
    })
    
    ```

3. **Calculate Capital Gains (Tax)**

    ```go
    costBasis := holding.AverageBuyPrice * quantityToSell
    proceeds := executionPrice * quantityToSell
    capitalGain := proceeds - costBasis
    
    // Determine holding period
    holdingPeriod := "short_term" // < 1 year
    if time.Since(holding.FirstPurchasedAt) > 365*24*time.Hour {
        holdingPeriod = "long_term"
    }
    
    // Store for tax reporting
    _, err = tx.Exec(`
        INSERT INTO capital_gains (
            user_id,
            order_id,
            symbol,
            quantity,
            cost_basis_cents,
            proceeds_cents,
            gain_cents,
            holding_period,
            tax_year
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
    `, userId, orderId, symbol, quantity, costBasis, proceeds, capitalGain, holdingPeriod, 2026)
    
    ```

4. **Update Holdings**

    ```go
    newQuantity := holding.Quantity - quantityToSell
    
    if newQuantity == 0 {
        // Sold entire position - delete holding
        _, err = tx.Exec(`
            DELETE FROM holdings
            WHERE id = $1
        `, holdingId)
    } else {
        // Partial sell - update quantity
        newCostBasis := holding.TotalCostBasis - (holding.AverageBuyPrice * quantityToSell)
    
        _, err = tx.Exec(`
            UPDATE holdings
            SET quantity = $1,
                total_cost_basis_cents = $2,
                updated_at = NOW()
            WHERE id = $3
        `, newQuantity, newCostBasis, holdingId)
    }
    
    ```

5. **Credit Destination Account**

    ```go
    _, err = tx.Exec(`
        UPDATE accounts
        SET balance_cents = balance_cents + $1,
            updated_at = NOW()
        WHERE id = $2
    `, int64(proceeds * 100), destinationAccountId)
    
    ```

6. **Record in Ledger**

    ```go
    _, err = tx.Exec(`
        INSERT INTO ledger (
            user_id,
            account_id,
            entry_type,
            amount_cents,
            reference_type,
            reference_id,
            description
        ) VALUES ($1, $2, $3, $4, $5, $6, $7)
    `,
        userId,
        destinationAccountId,
        "investment_credit",
        int64(proceeds * 100),
        "order",
        orderId,
        fmt.Sprintf("Sale proceeds from order #%d", orderId),
    )
    
    ```


---

# 🔗Endpoint 3: Transfer Between Accounts

## **POST /transfers**

Transfer money between user's accounts

**Request:**

```json
{
  "userId": 123,
  "sourceAccountId": 789,
  "destinationAccountId": 790,
  "amount": 100.00,
  "currency": "EUR",
  "description": "Transfer to investment account",
  "idempotencyKey": "transfer_123_1737285800"
}

```

**Response: 200 OK**

```json
{
  "transferId": "transfer_xyz123",
  "userId": 123,
  "sourceAccount": {
    "id": 789,
    "name": "Main Checking",
    "newBalance": 5320.50
  },
  "destinationAccount": {
    "id": 790,
    "name": "Investment Account",
    "newBalance": 100.00
  },
  "amount": 100.00,
  "ledgerEntries": [
    {
      "id": 1002,
      "type": "transfer_debit",
      "accountId": 789,
      "amount": -100.00
    },
    {
      "id": 1003,
      "type": "transfer_credit",
      "accountId": 790,
      "amount": 100.00
    }
  ],
  "completedAt": "2026-01-19T16:30:00.123Z"
}

```

**Business Logic:**

```go
func ExecuteTransfer(ctx context.Context, req TransferRequest) (*TransferResult, error) {
    // Idempotency check
    if cached := checkIdempotency(req.IdempotencyKey); cached != nil {
        return cached, nil
    }

    // Begin transaction
    tx, err := db.BeginTx(ctx, &sql.TxOptions{Isolation: sql.LevelSerializable})
    defer tx.Rollback()

    // Validate both accounts belong to user
    sourceAccount, err := tx.QueryRow(`
        SELECT id, balance_cents, status
        FROM accounts
        WHERE id = $1 AND user_id = $2
        FOR UPDATE
    `, req.SourceAccountId, req.UserId).Scan(&sourceAccount)

    destAccount, err := tx.QueryRow(`
        SELECT id, balance_cents, status
        FROM accounts
        WHERE id = $1 AND user_id = $2
        FOR UPDATE
    `, req.DestinationAccountId, req.UserId).Scan(&destAccount)

    // Check sufficient balance
    amountCents := int64(req.Amount * 100)
    if sourceAccount.BalanceCents < amountCents {
        return nil, errors.New("Insufficient funds")
    }

    // Debit source
    _, err = tx.Exec(`
        UPDATE accounts
        SET balance_cents = balance_cents - $1
        WHERE id = $2
    `, amountCents, req.SourceAccountId)

    // Credit destination
    _, err = tx.Exec(`
        UPDATE accounts
        SET balance_cents = balance_cents + $1
        WHERE id = $2
    `, amountCents, req.DestinationAccountId)

    // Ledger entries (2 entries for double-entry bookkeeping)
    _, err = tx.Exec(`
        INSERT INTO ledger (user_id, account_id, entry_type, amount_cents, description)
        VALUES
            ($1, $2, 'transfer_debit', $3, $4),
            ($1, $5, 'transfer_credit', $6, $4)
    `,
        req.UserId,
        req.SourceAccountId,
        -amountCents,
        req.Description,
        req.DestinationAccountId,
        amountCents,
    )

    // Commit
    if err := tx.Commit(); err != nil {
        return nil, err
    }

    // Cache result
    cacheIdempotencyResult(req.IdempotencyKey, result)

    // Invalidate caches
    invalidateCaches(req.UserId, req.SourceAccountId, req.DestinationAccountId)

    return result, nil
}

```

---

# 🔗Endpoint 4: Dividend Processing

## **POST /dividends/process**

Process dividend payment from holdings

**Request** (Internal - triggered by scheduled job):

```json
{
  "dividendPaymentId": "div_abc123",
  "userId": 123,
  "portfolioId": 101,
  "holdingId": 201,
  "symbol": "VUSA.L",
  "quantity": 50.5,
  "dividendPerShare": 0.52,
  "paymentDate": "2026-01-19",
  "reinvest": true
}

```

**Response: 200 OK**

```json
{
  "dividendPaymentId": "div_abc123",
  "userId": 123,
  "symbol": "VUSA.L",
  "totalDividend": 26.26,
  "taxWithheld": 3.94,
  "netDividend": 22.32,
  "reinvested": true,
  "reinvestmentDetails": {
    "quantityPurchased": 0.26,
    "purchasePrice": 85.85,
    "totalCost": 22.32
  },
  "ledgerEntries": [
    {
      "id": 1004,
      "type": "dividend_credit",
      "amount": 26.26
    },
    {
      "id": 1005,
      "type": "dividend_tax_debit",
      "amount": -3.94
    }
  ],
  "processedAt": "2026-01-19T10:00:00.123Z"
}

```

**Business Logic:**

```go
func ProcessDividend(ctx context.Context, req DividendRequest) error {
    tx, err := db.BeginTx(ctx, nil)
    defer tx.Rollback()

    // Calculate dividend amount
    totalDividend := req.Quantity * req.DividendPerShare  // 50.5 * 0.52 = 26.26

    // Calculate withholding tax (15% for US dividends)
    taxWithheld := totalDividend * 0.15  // 3.94
    netDividend := totalDividend - taxWithheld  // 22.32

    if req.Reinvest {
        // Use net dividend to buy more shares
        currentPrice, err := getMarketPrice(req.Symbol)
        quantityToBuy := netDividend / currentPrice  // 22.32 / 85.85 = 0.26 shares

        // Update holding
        _, err = tx.Exec(`
            UPDATE holdings
            SET quantity = quantity + $1,
                total_cost_basis_cents = total_cost_basis_cents + $2
            WHERE id = $3
        `, quantityToBuy, int64(netDividend * 100), req.HoldingId)

    } else {
        // Credit to user's account
        accountId := getUserDefaultAccount(req.UserId)

        _, err = tx.Exec(`
            UPDATE accounts
            SET balance_cents = balance_cents + $1
            WHERE id = $2
        `, int64(netDividend * 100), accountId)
    }

    // Record dividend in ledger
    _, err = tx.Exec(`
        INSERT INTO ledger (user_id, entry_type, amount_cents, description)
        VALUES
            ($1, 'dividend_credit', $2, $3),
            ($1, 'dividend_tax_debit', $4, $5)
    `,
        req.UserId,
        int64(totalDividend * 100),
        fmt.Sprintf("Dividend from %s", req.Symbol),
        -int64(taxWithheld * 100),
        "Dividend withholding tax",
    )

    // Record in dividends table (for tax reporting)
    _, err = tx.Exec(`
        INSERT INTO dividends (
            user_id,
            portfolio_id,
            holding_id,
            symbol,
            payment_date,
            gross_amount_cents,
            tax_withheld_cents,
            net_amount_cents,
            reinvested
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
    `,
        req.UserId, req.PortfolioId, req.HoldingId, req.Symbol,
        req.PaymentDate,
        int64(totalDividend * 100),
        int64(taxWithheld * 100),
        int64(netDividend * 100),
        req.Reinvest,
    )

    return tx.Commit()
}

```

---

# 🔗Endpoint 5: Ledger Reconciliation

## **GET /ledger/reconcile**

Verify ledger accuracy

(internal audit tool)

**Query Parameters:**

```jsx
?userId=123
&startDate=2026-01-01
&endDate=2026-01-31

```

**Response: 200 OK**

```json
{
  "userId": 123,
  "period": {
    "start": "2026-01-01",
    "end": "2026-01-31"
  },
  "reconciliation": {
    "expectedBalance": 5420.50,
    "actualBalance": 5420.50,
    "discrepancy": 0.00,
    "status": "balanced"
  },
  "ledgerSummary": {
    "totalEntries": 156,
    "totalDebits": -3245.80,
    "totalCredits": 8666.30,
    "netChange": 5420.50
  },
  "breakdown": [
    {
      "entryType": "investment_debit",
      "count": 8,
      "totalAmount": -2500.00
    },
    {
      "entryType": "dividend_credit",
      "count": 4,
      "totalAmount": 145.50
    },
    {
      "entryType": "fee_debit",
      "count": 8,
      "totalAmount": -12.00
    }
  ]
}

```

**Business Logic:**

```go
func ReconcileLedger(userId int, startDate, endDate time.Time) (*ReconciliationResult, error) {
    // Sum all ledger entries for period
    var ledgerTotal int64
    err := db.QueryRow(`
        SELECT COALESCE(SUM(amount_cents), 0)
        FROM ledger
        WHERE user_id = $1
        AND created_at >= $2
        AND created_at <= $3
    `, userId, startDate, endDate).Scan(&ledgerTotal)

    // Get actual account balance
    var actualBalance int64
    err = db.QueryRow(`
        SELECT balance_cents
        FROM accounts
        WHERE user_id = $1 AND account_type = 'checking'
    `, userId).Scan(&actualBalance)

    // Get starting balance
    var startingBalance int64
    err = db.QueryRow(`
        SELECT balance_cents
        FROM account_snapshots
        WHERE user_id = $1 AND snapshot_date = $2
    `, userId, startDate).Scan(&startingBalance)

    // Calculate expected balance
    expectedBalance := startingBalance + ledgerTotal

    // Check discrepancy
    discrepancy := actualBalance - expectedBalance

    status := "balanced"
    if discrepancy != 0 {
        status = "discrepancy_found"
        // Alert operations team
        alertOps(userId, discrepancy)
    }

    return &ReconciliationResult{
        ExpectedBalance: float64(expectedBalance) / 100,
        ActualBalance: float64(actualBalance) / 100,
        Discrepancy: float64(discrepancy) / 100,
        Status: status,
    }, nil
}

```

**Why reconciliation matters:**

- Catch bugs in transaction processing
- Detect data corruption
- Regulatory requirement (prove accuracy)
- Run daily as automated check

---

# Performance & Reliability

### **Throughput (2 Go instances):**

- 500 order executions/second
- 2,000 transfers/second
- 10,000 ledger queries/second

### **Latency:**

- Order execution: 2-5 seconds (broker API dependent)
- Transfers: 10-30ms (database-bound)
- Ledger queries: 5-10ms (indexed)

### **Reliability Mechanisms:**

1. **Idempotency**: All endpoints check Redis before processing
2. **Database Transactions**: ACID guarantees (all-or-nothing)
3. **Retries**: Automatic retry with exponential backoff for transient failures
4. **Circuit Breaker**: If broker API fails 10 times in row, stop sending requests (cool-down period)
5. **Dead Letter Queue**: Failed orders go to RabbitMQ DLQ for manual review
6. **Audit Logging**: Every operation logged to `audit_logs` table

### **Monitoring:**

- Alert if order failure rate > 1%
- Alert if ledger reconciliation finds discrepancy
- Alert if broker API latency > 10 seconds
- Dashboard showing: orders/sec, success rate, average latency

---

This Transaction Service is the most critical component - it handles real money movement. Every design decision prioritizes **correctness over performance**, with comprehensive error handling, audit trails, and safety mechanisms to prevent financial loss.