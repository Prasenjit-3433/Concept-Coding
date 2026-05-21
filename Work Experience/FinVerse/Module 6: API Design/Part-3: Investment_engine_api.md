Base URL: `https://api.finverse.com/api/v1/investments`

**Purpose**: Handle portfolio calculations, rebalancing, tax optimization. Computationally intensive operations requiring high performance and true concurrency.

**Technology Choice**: Go is used because:

- Fast floating-point math (critical for financial calculations)
- True concurrency with goroutines (calculate 1000s of portfolios in parallel)
- Low memory footprint
- Compiled language = predictable performance

---

## 🔗Endpoint 1: Calculate Allocation

### **POST /calculate-allocation**

Calculate ETF allocation for new investment order

**Authentication**: Internal service-to-service (called by Core API after user creates order)

**Request:**

```json
{
  "orderId": 456,
  "portfolioId": 101,
  "portfolioType": "growth",
  "amount": 500.00,
  "currency": "EUR"
}

```

**Response: 200 OK**

```json
{
  "orderId": 456,
  "portfolioId": 101,
  "allocation": [
    {
      "symbol": "VUSA.L",
      "name": "Vanguard S&P 500 UCITS ETF",
      "assetClass": "stocks",
      "targetPercentage": 70.0,
      "allocatedAmount": 350.00,
      "currentPrice": 85.50,
      "estimatedQuantity": 4.093567,
      "actualQuantity": 4.09,
      "actualAmount": 349.70
    },
    {
      "symbol": "VGOV.L",
      "name": "Vanguard Global Aggregate Bond UCITS ETF",
      "assetClass": "bonds",
      "targetPercentage": 30.0,
      "allocatedAmount": 150.00,
      "currentPrice": 23.98,
      "estimatedQuantity": 6.255213,
      "actualQuantity": 6.26,
      "actualAmount": 150.10
    }
  ],
  "summary": {
    "requestedAmount": 500.00,
    "investedAmount": 499.80,
    "cashRemainder": 0.20,
    "platformFee": 1.50,
    "totalDeduction": 501.50
  },
  "calculatedAt": "2026-01-19T15:05:02.345Z",
  "processingTimeMs": 12
}

```

**Business Logic (Detailed):**

1. **Fetch Portfolio Configuration**

    ```go
    portfolio, err := db.GetPortfolioByID(portfolioId)
    // Returns: targetAllocation = {"stocks": 70, "bonds": 30}
    
    ```

2. **Get Current Market Prices**

    ```go
    // Try Redis first (fast path)
    prices := redis.MGet("etf:price:VUSA.L", "etf:price:VGOV.L")
    
    // If cache miss, query PostgreSQL
    if prices == nil {
        prices = db.GetMarketPrices([]string{"VUSA.L", "VGOV.L"})
        // Populate Redis for next time
        redis.SetMulti(prices, 900) // 15 min TTL
    }
    
    ```

3. **Calculate Allocation**

    ```go
    totalAmount := 500.00
    
    for asset, percentage := range targetAllocation {
        allocatedAmount := totalAmount * (percentage / 100.0)
        // stocks: 500 * 0.70 = 350.00
        // bonds: 500 * 0.30 = 150.00
    
        price := prices[asset.Symbol]
        quantity := allocatedAmount / price
        // VUSA.L: 350.00 / 85.50 = 4.093567
    
        // Round to 2 decimals (broker precision)
        roundedQuantity := math.Round(quantity * 100) / 100
        // 4.09 shares
    
        actualAmount := roundedQuantity * price
        // 4.09 * 85.50 = 349.695 ≈ 349.70
    }
    
    ```

4. **Handle Cash Remainder**

    ```go
    totalInvested := 349.70 + 150.10 // = 499.80
    remainder := 500.00 - 499.80     // = 0.20
    
    // Store remainder as CASH holding in portfolio
    // User gets €0.20 back or it stays as uninvested cash
    
    ```

5. **Update Order in Database**

    ```sql
    UPDATE orders
    SET status = 'calculated',
        allocation = '{"VUSA.L": {"quantity": 4.09, "amount": 349.70}, ...}'::jsonb,
        updated_at = NOW()
    WHERE id = 456;
    
    ```

6. **Publish Event to RabbitMQ**

    ```go
    event := OrderCalculatedEvent{
        OrderID: 456,
        Allocation: allocation,
    }
    
    rabbitmq.Publish("orders_exchange", "order.calculated", event)
    
    ```

7. **Return Response**
    - Include calculated allocation
    - Show actual vs estimated quantities (transparency)
    - Processing time for monitoring

**Error Handling:**

- `404 Not Found`: Portfolio doesn't exist
- `400 Bad Request`: Invalid amount (≤ 0 or > max limit)
- `503 Service Unavailable`: Market data API down, can't fetch prices
- `500 Internal Server Error`: Calculation failure

**Performance Considerations:**

- Response time: 10-20ms (with Redis cache hit)
- Can process 1000 calculations/second on single instance
- Goroutines handle concurrent requests efficiently

---

## 🔗Endpoint 2: Rebalance Portfolio

### **POST /rebalance**

Calculate trades needed to rebalance portfolio to target allocation

**Authentication**: Internal service (called by BullMQ scheduled job every Saturday 2 AM)

**Request:**

```json
{
  "portfolioId": 101
}

```

**Response: 200 OK (No Rebalancing Needed)**

```json
{
  "portfolioId": 101,
  "portfolioType": "growth",
  "rebalanceNeeded": false,
  "currentAllocation": {
    "stocks": 72.5,
    "bonds": 27.5
  },
  "targetAllocation": {
    "stocks": 70.0,
    "bonds": 30.0
  },
  "drift": 2.5,
  "driftThreshold": 5.0,
  "message": "Portfolio within acceptable drift threshold",
  "nextCheckDate": "2026-01-26T02:00:00Z"
}

```

**Response: 200 OK (Rebalancing Needed)**

```json
{
  "portfolioId": 101,
  "portfolioType": "growth",
  "rebalanceNeeded": true,
  "currentAllocation": {
    "stocks": 76.5,
    "bonds": 23.5
  },
  "targetAllocation": {
    "stocks": 70.0,
    "bonds": 30.0
  },
  "drift": 6.5,
  "driftThreshold": 5.0,
  "currentValue": 12350.75,
  "trades": [
    {
      "action": "sell",
      "symbol": "VUSA.L",
      "name": "Vanguard S&P 500 UCITS ETF",
      "currentQuantity": 50.5,
      "currentValue": 4317.75,
      "currentPercentage": 35.0,
      "targetValue": 4322.76,
      "quantityToSell": 3.2,
      "estimatedProceeds": 273.60,
      "reason": "Reduce stocks allocation from 76.5% to 70%"
    },
    {
      "action": "sell",
      "symbol": "VEUR.L",
      "name": "Vanguard FTSE Europe UCITS ETF",
      "currentQuantity": 120.0,
      "currentValue": 4416.00,
      "currentPercentage": 35.7,
      "targetValue": 4322.76,
      "quantityToSell": 2.5,
      "estimatedProceeds": 92.00,
      "reason": "Reduce stocks allocation from 76.5% to 70%"
    },
    {
      "action": "buy",
      "symbol": "VGOV.L",
      "name": "Vanguard Global Aggregate Bond UCITS ETF",
      "currentQuantity": 140.0,
      "currentValue": 3357.20,
      "currentPercentage": 27.2,
      "targetValue": 3705.23,
      "quantityToBuy": 14.5,
      "estimatedCost": 347.71,
      "reason": "Increase bonds allocation from 23.5% to 30%"
    }
  ],
  "summary": {
    "totalSellValue": 365.60,
    "totalBuyValue": 347.71,
    "netCashFlow": 17.89,
    "estimatedFees": 0.00,
    "estimatedTaxImpact": 0.00
  },
  "calculatedAt": "2026-01-19T02:00:15.234Z"
}

```

**Business Logic (Detailed):**

1. **Fetch Current Holdings**

    ```go
    holdings, err := db.GetPortfolioHoldings(portfolioId)
    // Returns array of holdings with quantity, current price, value
    
    ```

2. **Calculate Current Allocation**

    ```go
    totalValue := 12350.75
    
    stocksValue := 0.0
    bondsValue := 0.0
    
    for _, holding := range holdings {
        value := holding.Quantity * holding.CurrentPrice
    
        if holding.AssetClass == "stocks" {
            stocksValue += value
        } else if holding.AssetClass == "bonds" {
            bondsValue += value
        }
    }
    
    currentAllocation := map[string]float64{
        "stocks": (stocksValue / totalValue) * 100,  // 76.5%
        "bonds": (bondsValue / totalValue) * 100,     // 23.5%
    }
    
    ```

3. **Calculate Drift**

    ```go
    targetAllocation := map[string]float64{"stocks": 70, "bonds": 30}
    
    drift := math.Abs(currentAllocation["stocks"] - targetAllocation["stocks"])
    // 76.5 - 70 = 6.5%
    
    driftThreshold := 5.0  // From portfolio settings
    
    if drift <= driftThreshold {
        return RebalanceNotNeeded()
    }
    
    ```

4. **Calculate Target Values**

    ```go
    targetValues := map[string]float64{
        "stocks": totalValue * (70.0 / 100.0),  // 8645.53
        "bonds": totalValue * (30.0 / 100.0),   // 3705.23
    }
    
    ```

5. **Determine Trades**

    ```go
    trades := []Trade{}
    
    // Stocks: need to reduce from 9450.75 to 8645.53
    stocksToSell := 9450.75 - 8645.53  // 805.22
    
    // Distribute sell across stock ETFs proportionally
    for _, holding := range stockHoldings {
        proportion := holding.Value / stocksValue
        sellValue := stocksToSell * proportion
        sellQuantity := sellValue / holding.CurrentPrice
    
        trades = append(trades, Trade{
            Action: "sell",
            Symbol: holding.Symbol,
            Quantity: roundToTwoDecimals(sellQuantity),
        })
    }
    
    // Bonds: need to increase from 2900.00 to 3705.23
    bondsToB buy := 3705.23 - 2900.00  // 805.23
    
    // Buy bond ETFs
    for _, bondETF := range bondETFs {
        proportion := bondETF.TargetProportion
        buyValue := bondsToBuy * proportion
        buyQuantity := buyValue / bondETF.CurrentPrice
    
        trades = append(trades, Trade{
            Action: "buy",
            Symbol: bondETF.Symbol,
            Quantity: roundToTwoDecimals(buyQuantity),
        })
    }
    
    ```

6. **Optimize for Tax Efficiency (Advanced)**

    ```go
    // If user has tax loss harvesting enabled
    if portfolio.TaxLossHarvestingEnabled {
        // Prioritize selling holdings with losses first
        // Reduces capital gains tax liability
        sortedHoldings := sortByUnrealizedLoss(holdings)
        // Sell losers first, winners last
    }
    
    ```

7. **Publish Rebalance Order Event**

    ```go
    if rebalanceNeeded {
        event := RebalanceOrderEvent{
            PortfolioID: portfolioId,
            Trades: trades,
        }
    
        rabbitmq.Publish("orders_exchange", "order.rebalance", event)
        // Transaction Service will execute trades
    }
    
    ```

8. **Update Portfolio Metadata**

    ```sql
    UPDATE portfolios
    SET last_rebalanced_at = NOW(),
        needs_rebalancing = false
    WHERE id = 101;
    
    ```


**Why Rebalancing Matters:**

- Market movements cause drift (stocks grow faster than bonds)
- Drift increases risk beyond user's comfort level
- Regular rebalancing maintains target allocation
- "Sell high, buy low" automatically (contrarian strategy)

**Frequency:**

- Checked weekly (every Saturday 2 AM)
- Only rebalance if drift > threshold (minimize trading costs)
- User can manually trigger via app

---

## 🔗Endpoint 3: Portfolio Performance

### **GET /portfolios/:id/performance**

Calculate detailed portfolio performance metrics

**Authentication**: Internal (called by Core API when user views portfolio)

**Query Parameters:**

```
?period=1year
&includeHoldings=true

```

**Response: 200 OK**

```json
{
  "portfolioId": 101,
  "period": "1year",
  "performance": {
    "totalReturn": {
      "amount": 1350.75,
      "percentage": 12.28
    },
    "timeWeightedReturn": 12.15,
    "moneyWeightedReturn": 11.98,
    "benchmarkComparison": {
      "benchmark": "MSCI World Index",
      "benchmarkReturn": 11.50,
      "alpha": 0.78,
      "message": "Outperformed benchmark by 0.78%"
    },
    "volatility": {
      "standardDeviation": 8.45,
      "sharpeRatio": 1.42,
      "maxDrawdown": -5.23,
      "maxDrawdownDate": "2025-08-15"
    },
    "dividends": {
      "totalDividends": 145.50,
      "dividendYield": 1.32,
      "reinvested": true
    }
  },
  "holdingsPerformance": [
    {
      "symbol": "VUSA.L",
      "name": "Vanguard S&P 500 UCITS ETF",
      "totalReturn": 3.64,
      "contribution": 151.50,
      "contributionPercentage": 11.2
    },
    {
      "symbol": "VEUR.L",
      "name": "Vanguard FTSE Europe UCITS ETF",
      "totalReturn": 5.14,
      "contribution": 216.00,
      "contributionPercentage": 16.0
    },
    {
      "symbol": "VGOV.L",
      "name": "Vanguard Global Aggregate Bond UCITS ETF",
      "totalReturn": -4.08,
      "contribution": -142.80,
      "contributionPercentage": -10.6
    }
  ],
  "calculatedAt": "2026-01-19T15:30:00Z",
  "cacheExpiry": "2026-01-19T15:45:00Z"
}

```

**Business Logic:**

1. **Calculate Total Return**

    ```go
    totalInvested := 11000.00
    currentValue := 12350.75
    
    totalReturn := currentValue - totalInvested  // 1350.75
    returnPercentage := (totalReturn / totalInvested) * 100  // 12.28%
    
    ```

2. **Time-Weighted Return (TWR)**

    ```go
    // Measures portfolio manager performance
    // Eliminates impact of deposits/withdrawals timing
    
    // Divide period into sub-periods at each cash flow
    subPeriods := []Period{
        {Start: "2025-01-19", End: "2025-03-01", Value: 11150.00},
        {Start: "2025-03-01", End: "2025-06-01", Value: 11680.00}, // +500 deposit
        {Start: "2025-06-01", End: "2026-01-19", Value: 12350.75},
    }
    
    twr := 1.0
    for _, period := range subPeriods {
        periodReturn := (period.EndValue - period.StartValue) / period.StartValue
        twr *= (1 + periodReturn)
    }
    
    twr = (twr - 1) * 100  // 12.15%
    
    ```

3. **Money-Weighted Return (MWR)**

    ```go
    // Measures investor's actual return
    // Considers timing of deposits/withdrawals
    
    // Uses XIRR (Extended Internal Rate of Return) algorithm
    cashFlows := []CashFlow{
        {Date: "2025-01-19", Amount: -11000.00},  // Initial investment
        {Date: "2025-03-01", Amount: -500.00},    // Additional deposit
        {Date: "2026-01-19", Amount: 12350.75},   // Current value
    }
    
    mwr := calculateXIRR(cashFlows)  // 11.98%
    
    ```

4. **Sharpe Ratio**

    ```go
    // Risk-adjusted return metric
    // Higher = better return per unit of risk
    
    avgReturn := 12.28
    riskFreeRate := 3.0  // EU government bonds yield
    standardDeviation := 8.45
    
    sharpeRatio := (avgReturn - riskFreeRate) / standardDeviation
    // (12.28 - 3.0) / 8.45 = 1.10
    
    // Interpretation:
    // > 1.0 = Good risk-adjusted return
    // > 2.0 = Excellent
    // < 1.0 = Taking too much risk for return
    
    ```

5. **Max Drawdown**

    ```go
    // Largest peak-to-trough decline
    // Measures worst-case loss scenario
    
    historicalValues := getHistoricalValues(portfolioId, period)
    
    maxValue := 0.0
    maxDrawdown := 0.0
    
    for _, value := range historicalValues {
        if value > maxValue {
            maxValue = value
        }
    
        drawdown := (value - maxValue) / maxValue * 100
    
        if drawdown < maxDrawdown {
            maxDrawdown = drawdown  // -5.23%
        }
    }
    
    ```

6. **Benchmark Comparison**

    ```go
    // Compare to market index
    benchmarkReturn := fetchBenchmarkReturn("MSCI_WORLD", period)  // 11.50%
    
    alpha := portfolioReturn - benchmarkReturn  // 12.28 - 11.50 = 0.78%
    
    // Positive alpha = outperforming market
    // Negative alpha = underperforming market
    
    ```


**Caching:**

- Performance metrics cached in Redis (15-minute TTL)
- Recalculated only when needed (market data updates)
- Heavy computation, avoid recalculating on every request

---

## 🔗Endpoint 4: Tax Report Generation

### **POST /tax-reports/generate**

Generate tax report for user's investments

**Authentication**: Internal (called by Core API or BullMQ scheduled job)

**Request:**

```json
{
  "userId": 123,
  "taxYear": 2025,
  "country": "DE"
}

```

**Response: 202 Accepted**

```json
{
  "reportId": "tax_report_abc123",
  "userId": 123,
  "taxYear": 2025,
  "status": "generating",
  "estimatedCompletion": "2026-01-19T16:05:00Z",
  "message": "Tax report generation started. You'll be notified when ready."
}

```

**Background Processing:**

1. **Fetch All Investment Transactions**

    ```go
    transactions := db.GetUserTransactions(userId, taxYear)
    // Returns: buys, sells, dividends for entire year
    
    ```

2. **Calculate Capital Gains**

    ```go
    capitalGains := []CapitalGain{}
    
    for _, sell := range sellTransactions {
        // Find matching buy (FIFO method)
        buy := findMatchingBuy(sell.Symbol, sell.Quantity)
    
        costBasis := buy.Price * sell.Quantity
        proceeds := sell.Price * sell.Quantity
    
        gain := proceeds - costBasis
    
        holdingPeriod := sell.Date.Sub(buy.Date)
        taxType := "short_term"
        if holdingPeriod > 365*24*time.Hour {
            taxType = "long_term"
        }
    
        capitalGains = append(capitalGains, CapitalGain{
            Symbol: sell.Symbol,
            Quantity: sell.Quantity,
            BuyDate: buy.Date,
            SellDate: sell.Date,
            CostBasis: costBasis,
            Proceeds: proceeds,
            Gain: gain,
            TaxType: taxType,
        })
    }
    
    ```

3. **Calculate Dividend Income**

    ```go
    dividends := db.GetDividends(userId, taxYear)
    
    totalDividends := 0.0
    for _, dividend := range dividends {
        totalDividends += dividend.Amount
    }
    
    ```

4. **Apply Country-Specific Tax Rules**

    ```go
    switch country {
    case "DE":
        // Germany: 25% capital gains tax + 5.5% solidarity surcharge
        // First €1000 tax-free (Sparerpauschbetrag)
    
        taxableGains := totalGains - 1000.00
        if taxableGains < 0 {
            taxableGains = 0
        }
    
        capitalGainsTax := taxableGains * 0.25
        solidaritySurcharge := capitalGainsTax * 0.055
    
        totalTax := capitalGainsTax + solidaritySurcharge
    
    case "FR":
        // France: 30% flat tax (PFU) or progressive income tax
        // Calculate both, user chooses lower
    
    // ... other countries
    }
    
    ```

5. **Generate PDF Report**

    ```go
    report := TaxReport{
        Year: 2025,
        CapitalGains: capitalGains,
        Dividends: dividends,
        TotalTax: totalTax,
    }
    
    pdfBytes := generatePDF(report)
    
    // Upload to S3
    s3Key := fmt.Sprintf("tax-reports/%d/%s.pdf", userId, reportId)
    s3.Upload(s3Key, pdfBytes)
    
    ```

6. **Store Report Metadata**

    ```sql
    INSERT INTO tax_reports (id, user_id, tax_year, status, s3_key, generated_at)
    VALUES ('tax_report_abc123', 123, 2025, 'completed', 's3://...', NOW());
    
    ```

7. **Notify User**

    ```go
    rabbitmq.Publish("notifications_exchange", "notification.tax_report.ready.email", event)
    
    ```


**User Can Download:**

```
GET /tax-reports/tax_report_abc123/download
→ Returns PDF from S3

```

---

## 🔗Endpoint 5: Optimization Suggestions

### **GET /portfolios/:id/optimize**

Get AI-powered portfolio optimization suggestions

**Authentication**: Internal (called by Core API)

**Response: 200 OK**

```json
{
  "portfolioId": 101,
  "currentAllocation": {
    "stocks": 72.5,
    "bonds": 27.5
  },
  "suggestions": [
    {
      "type": "rebalance",
      "priority": "medium",
      "title": "Rebalance recommended",
      "description": "Your portfolio has drifted 2.5% from target allocation",
      "impact": "Maintain desired risk level",
      "action": {
        "label": "Rebalance now",
        "endpoint": "/portfolios/101/rebalance"
      }
    },
    {
      "type": "tax_loss_harvest",
      "priority": "high",
      "title": "Tax loss harvesting opportunity",
      "description": "VGOV.L is down 4.08%. Sell now to offset capital gains.",
      "impact": "Potential tax savings: €35.70",
      "action": {
        "label": "Review opportunity",
        "endpoint": "/tax-optimization/123"
      }
    },
    {
      "type": "increase_contribution",
      "priority": "low",
      "title": "Consider increasing contributions",
      "description": "You're saving €200/month. Increasing to €250 would help you reach your goal 4 months earlier.",
      "impact": "Reach goal by Oct 2027 instead of Feb 2028",
      "action": null
    }
  ],
  "calculatedAt": "2026-01-19T16:00:00Z"
}

```

**Business Logic:**

- Rules-based system (if X, then suggest Y)
- Future: Machine learning model trained on user behavior
- Suggestions prioritized by potential impact

---

## Performance & Scalability

**Current Capacity (2 Go instances):**

- 1,000 allocation calculations/second
- 500 rebalance checks/second
- 100 performance calculations/second (more intensive)

**Bottlenecks:**

- Market data API rate limits
- Database query performance (mitigated by caching)

**Scaling Strategy:**

- Horizontal: Add more Go instances (stateless, easy to scale)
- Caching: Redis reduces database load by 80%
- Batching: Process multiple portfolios in single database query

---

This Investment Engine API handles all complex financial calculations, keeping the Core API lean and focused on CRUD operations. The separation allows independent scaling and specialized optimization for computational workloads.