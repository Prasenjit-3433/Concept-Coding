## Why MongoDB for These Collections?

**Decision criteria:**

- **Flexible schema**: Content structure evolves (add new fields without migration)
- **Nested documents**: Articles/quizzes have complex nested structures
- **High write volume**: Activity logs = thousands of writes per second
- **Aggregate queries**: Analytics (group by date, count by action type)

---

## 1. Activity Logs Collection

```jsx
// Collection: activity_logs
{
    _id: ObjectId("507f1f77bcf86cd799439011"),
    userId: 123,  // Reference to PostgreSQL users.id

    // Action details
    action: "investment_completed",  // investment_completed, budget_updated, page_viewed
    category: "investment",  // investment, budget, goal, education, navigation

    // Page/context
    page: "/dashboard",
    referrer: "/investments/create",

    // Device info
    device: {
        type: "mobile",  // mobile, desktop, tablet
        os: "iOS",
        osVersion: "16.2",
        browser: "Safari",
        browserVersion: "16.2"
    },

    // Location
    geo: {
        country: "DE",
        city: "Berlin",
        ip: "192.168.1.1"  // Hashed for privacy
    },

    // Action-specific data
    data: {
        orderId: 789,
        amount: 500,
        portfolioType: "growth"
    },

    // Session
    sessionId: "sess_abc123",

    // Timing
    timestamp: ISODate("2026-01-19T10:30:00Z"),
    serverProcessedAt: ISODate("2026-01-19T10:30:00.123Z"),

    // Performance tracking
    loadTimeMs: 450,

    // Indexes
    // db.activity_logs.createIndex({ userId: 1, timestamp: -1 })
    // db.activity_logs.createIndex({ action: 1, timestamp: -1 })
    // db.activity_logs.createIndex({ timestamp: -1 })
}

```

**Why MongoDB for activity logs?**

- **Schema flexibility**: Different actions have different `data` fields
    - Page view: `{page, duration}`
    - Investment: `{orderId, amount, portfolio}`
    - Quiz: `{quizId, score, timeTaken}`
- **Write performance**: MongoDB handles 10K+ writes/sec easily
- **Time-series queries**: Fast aggregations by date range
- **TTL index**: Auto-delete logs older than 90 days (GDPR compliance)

**Example queries:**

```jsx
// User's actions in last 7 days
db.activity_logs.find({
    userId: 123,
    timestamp: { $gte: ISODate("2026-01-12") }
}).sort({ timestamp: -1 });

// Count investments per day (last 30 days)
db.activity_logs.aggregate([
    {
        $match: {
            action: "investment_completed",
            timestamp: { $gte: ISODate("2025-12-20") }
        }
    },
    {
        $group: {
            _id: { $dateToString: { format: "%Y-%m-%d", date: "$timestamp" } },
            count: { $sum: 1 },
            totalAmount: { $sum: "$data.amount" }
        }
    },
    { $sort: { _id: 1 } }
]);

```

**Auto-delete old logs (GDPR):**

```jsx
// TTL index: delete documents 90 days after timestamp
db.activity_logs.createIndex(
    { "timestamp": 1 },
    { expireAfterSeconds: 7776000 }  // 90 days
);

```

---

## 2. Educational Content Collection

### **articles** collection

```jsx
// Collection: articles
{
    _id: ObjectId("507f1f77bcf86cd799439012"),
    lessonId: 456,  // Reference to PostgreSQL lessons.id

    // Content metadata
    title: "Understanding Compound Interest",
    author: {
        id: 789,  // Reference to PostgreSQL users.id (admin/content creator)
        name: "Sarah Johnson",
        avatar: "https://cdn.finverse.com/avatars/sarah.jpg"
    },

    // Rich text content (using blocks structure like Notion/Medium)
    content: [
        {
            type: "paragraph",
            text: "Compound interest is the eighth wonder of the world.",
            formatting: []
        },
        {
            type: "heading",
            level: 2,
            text: "How It Works"
        },
        {
            type: "paragraph",
            text: "When you invest €1,000 at 5% annual interest:",
            formatting: [
                { type: "bold", start: 10, end: 16 }  // Bold "invest"
            ]
        },
        {
            type: "list",
            listType: "bullet",
            items: [
                "Year 1: €1,000 + €50 = €1,050",
                "Year 2: €1,050 + €52.50 = €1,102.50",
                "Year 3: €1,102.50 + €55.13 = €1,157.63"
            ]
        },
        {
            type: "image",
            url: "https://cdn.finverse.com/images/compound-interest-graph.png",
            alt: "Compound interest growth over 30 years",
            caption: "Growth of €1,000 over 30 years at 7% annual return"
        },
        {
            type: "callout",
            style: "info",
            text: "💡 Tip: Start investing early to maximize compound growth!"
        },
        {
            type: "code",
            language: "javascript",
            code: "const futureValue = principal * Math.pow(1 + rate, years);"
        }
    ],

    // Reading statistics
    estimatedReadingMinutes: 5,
    wordCount: 850,

    // SEO
    metaDescription: "Learn how compound interest can grow your wealth over time",
    keywords: ["compound interest", "investing", "wealth building"],

    // Status
    status: "published",  // draft, published, archived
    publishedAt: ISODate("2026-01-15T09:00:00Z"),

    // Versioning (for content updates)
    version: 2,
    previousVersionId: ObjectId("507f1f77bcf86cd799439011"),

    // Timestamps
    createdAt: ISODate("2026-01-10T14:00:00Z"),
    updatedAt: ISODate("2026-01-15T09:00:00Z"),

    // Indexes
    // db.articles.createIndex({ lessonId: 1 })
    // db.articles.createIndex({ status: 1, publishedAt: -1 })
}

```

**Why MongoDB for articles?**

- **Flexible content blocks**: Can add new block types (video, interactive calculator) without schema change
- **Nested structure**: Content is hierarchical (paragraphs, lists, images)
- **Versioning**: Easy to store multiple versions as separate documents
- **Fast reads**: Entire article loaded with single query

---

### **quizzes** collection

```jsx
// Collection: quizzes
{
    _id: ObjectId("507f1f77bcf86cd799439013"),
    lessonId: 457,  // Reference to PostgreSQL lessons.id

    title: "Test Your Knowledge: Investment Basics",
    description: "10-question quiz on fundamental investing concepts",

    // Passing criteria
    passingScore: 70,  // 70% correct to pass
    timeLimit Seconds: 300,  // 5 minutes
    allowRetries: true,
    maxAttempts: 3,

    // Questions
    questions: [
        {
            id: "q1",
            type: "multiple_choice",
            question: "What does ETF stand for?",
            options: [
                { id: "a", text: "Exchange Traded Fund", isCorrect: true },
                { id: "b", text: "European Trading Firm", isCorrect: false },
                { id: "c", text: "Equity Transfer Fee", isCorrect: false },
                { id: "d", text: "External Tax Form", isCorrect: false }
            ],
            points: 10,
            explanation: "ETF stands for Exchange Traded Fund, a type of investment fund traded on stock exchanges."
        },
        {
            id: "q2",
            type: "true_false",
            question: "Diversification reduces investment risk.",
            correctAnswer: true,
            points: 10,
            explanation: "True. Diversification spreads risk across multiple assets, reducing overall portfolio risk."
        },
        {
            id: "q3",
            type: "fill_in_blank",
            question: "An investor with low risk tolerance should choose a ____ portfolio.",
            correctAnswers: ["conservative", "Conservative"],  // Case variations
            points: 10,
            explanation: "Conservative portfolios have lower risk with more bonds and fewer stocks."
        }
    ],

    // Statistics (calculated from user attempts)
    stats: {
        totalAttempts: 2450,
        averageScore: 78.5,
        passRate: 0.82,  // 82% pass
        averageTimeSeconds: 240,
        questionDifficulty: {
            q1: 0.95,  // 95% answer correctly (easy)
            q2: 0.88,
            q3: 0.62   // 62% answer correctly (harder)
        }
    },

    // Metadata
    createdBy: 789,
    status: "published",
    publishedAt: ISODate("2026-01-15T09:00:00Z"),
    createdAt: ISODate("2026-01-12T10:00:00Z"),
    updatedAt: ISODate("2026-01-15T09:00:00Z"),

    // Indexes
    // db.quizzes.createIndex({ lessonId: 1 })
}

```

**Why MongoDB for quizzes?**

- **Variable question types**: Multiple choice, true/false, fill-in-blank, essay (future)
- **Nested options**: Each question has array of options with varying structures
- **Easy updates**: Add/remove questions without touching database schema
- **Analytics**: Store question-level statistics in same document

---

### **quiz_attempts** collection

```jsx
// Collection: quiz_attempts
{
    _id: ObjectId("507f1f77bcf86cd799439014"),
    userId: 123,  // Reference to PostgreSQL users.id
    quizId: ObjectId("507f1f77bcf86cd799439013"),
    lessonId: 457,

    // Attempt details
    attemptNumber: 2,  // Second attempt

    // Answers
    answers: [
        {
            questionId: "q1",
            selectedAnswer: "a",  // Chose option 'a'
            isCorrect: true,
            pointsEarned: 10,
            timeSpentSeconds: 12
        },
        {
            questionId: "q2",
            selectedAnswer: false,
            isCorrect: false,
            pointsEarned: 0,
            timeSpentSeconds: 18
        },
        {
            questionId: "q3",
            userAnswer: "moderate",
            isCorrect: false,  // Correct was "conservative"
            pointsEarned: 0,
            timeSpentSeconds: 35
        }
    ],

    // Results
    score: 33,  // 33 out of 100
    scorePercent: 33,
    passed: false,  // Did not reach 70%
    totalTimeSeconds: 195,

    // Status
    status: "completed",  // in_progress, completed, abandoned

    // Timestamps
    startedAt: ISODate("2026-01-19T11:00:00Z"),
    completedAt: ISODate("2026-01-19T11:03:15Z"),

    // Indexes
    // db.quiz_attempts.createIndex({ userId: 1, quizId: 1 })
    // db.quiz_attempts.createIndex({ lessonId: 1, completedAt: -1 })
}

```

**Why separate attempts collection?**

- **Multiple attempts**: Users can retake quizzes, each attempt is separate document
- **Analytics**: Analyze improvement over attempts ("80% of users pass on 2nd attempt")
- **History**: User can review their past attempts and answers

---

## 3. Notification Templates Collection

```jsx
// Collection: notification_templates
{
    _id: ObjectId("507f1f77bcf86cd799439015"),
    templateId: "budget_exceeded",  // Unique identifier

    // Template metadata
    name: "Budget Exceeded Alert",
    description: "Sent when user exceeds their budget for a category",
    category: "budget",

    // Channels
    channels: {
        email: {
            enabled: true,
            subject: "⚠️ Budget Alert: You've exceeded your {{category}} budget",
            htmlBody: `
                <h2>Budget Alert</h2>
                <p>Hi {{userName}},</p>
                <p>You've spent <strong>€{{spentAmount}}</strong> on {{category}} this month,
                   which is {{percentage}}% of your €{{budgetLimit}} budget.</p>
                <p><a href="{{dashboardUrl}}">View your budget</a></p>
            `,
            textBody: `
                Hi {{userName}},
                You've spent €{{spentAmount}} on {{category}} this month ({{percentage}}% of €{{budgetLimit}} budget).
                View your budget: {{dashboardUrl}}
            `
        },
        push: {
            enabled: true,
            title: "Budget Alert: {{category}}",
            body: "You've spent €{{spentAmount}} ({{percentage}}% of limit)"
        },
        sms: {
            enabled: false,
            body: null
        }
    },

    // Variables (for validation and documentation)
    variables: [
        { name: "userName", type: "string", required: true },
        { name: "category", type: "string", required: true },
        { name: "spentAmount", type: "number", required: true },
        { name: "budgetLimit", type: "number", required: true },
        { name: "percentage", type: "number", required: true },
        { name: "dashboardUrl", type: "string", required: true }
    ],

    // Version control
    version: 3,

    // Status
    status: "active",  // active, inactive, archived

    // A/B testing
    variants: [
        {
            variantId: "A",
            weight: 0.5,  // 50% of users get this version
            subject: "⚠️ Budget Alert: You've exceeded your {{category}} budget"
        },
        {
            variantId: "B",
            weight: 0.5,
            subject: "💰 Heads up: Your {{category}} spending is over budget"
        }
    ],

    // Timestamps
    createdAt: ISODate("2026-01-01T00:00:00Z"),
    updatedAt: ISODate("2026-01-15T10:00:00Z"),

    // Indexes
    // db.notification_templates.createIndex({ templateId: 1 }, { unique: true })
    // db.notification_templates.createIndex({ status: 1 })
}

```

**Why MongoDB for templates?**

- **Multi-channel**: Email, push, SMS each have different structure
- **Flexible content**: HTML templates can be complex, no fixed schema needed
- **A/B testing**: Easy to add variants without database migration
- **Versioning**: Keep historical versions of templates

**Usage example in Notification Service:**

```jsx
// Fetch template
const template = await db.notification_templates.findOne({
    templateId: "budget_exceeded",
    status: "active"
});

// Render with user data
const rendered = renderTemplate(template.channels.email.htmlBody, {
    userName: "John",
    category: "food",
    spentAmount: 650,
    budgetLimit: 600,
    percentage: 108,
    dashboardUrl: "https://app.finverse.com/dashboard"
});

// Send via SendGrid
await sendEmail({
    to: user.email,
    subject: renderTemplate(template.channels.email.subject, data),
    html: rendered
});

```

---

## 4. Analytics Events Collection

```jsx
// Collection: analytics_events
{
    _id: ObjectId("507f1f77bcf86cd799439016"),

    // Event identification
    eventName: "investment_order_created",
    eventCategory: "investment",  // investment, budget, goal, user, education

    // User context
    userId: 123,  // Reference to PostgreSQL users.id
    sessionId: "sess_abc123",
    anonymousId: "anon_xyz789",  // For tracking before login

    // Event properties
    properties: {
        orderId: 789,
        amount: 500,
        currency: "EUR",
        portfolioType: "growth",
        paymentMethod: "bank_account",
        isFirstOrder: false,
        orderSource: "mobile_app"  // mobile_app, web, api
    },

    // Page context
    page: {
        path: "/investments/create",
        referrer: "/dashboard",
        title: "Create Investment Order",
        url: "https://app.finverse.com/investments/create"
    },

    // Device context
    device: {
        type: "mobile",
        os: "iOS",
        osVersion: "16.2",
        appVersion: "2.1.5",
        screenResolution: "1170x2532"
    },

    // Location
    geo: {
        country: "DE",
        city: "Berlin",
        region: "Berlin",
        timezone: "Europe/Berlin"
    },

    // Timestamps
    timestamp: ISODate("2026-01-19T10:30:00.123Z"),
    receivedAt: ISODate("2026-01-19T10:30:00.456Z"),

    // Indexes
    // db.analytics_events.createIndex({ userId: 1, timestamp: -1 })
    // db.analytics_events.createIndex({ eventName: 1, timestamp: -1 })
    // db.analytics_events.createIndex({ timestamp: -1 })
    // db.analytics_events.createIndex({ sessionId: 1 })
}

```

**Why separate from activity_logs?**

- **Purpose**: `activity_logs` = user actions for debugging/support
- **Purpose**: `analytics_events` = business metrics, funnel analysis, growth
- **Retention**: Activity logs deleted after 90 days, analytics kept 2+ years
- **Schema**: Analytics events have richer context (device, geo, referrer)

**Example analytics queries:**

```jsx
// Conversion funnel: Dashboard -> Create Order -> Complete Order
db.analytics_events.aggregate([
    {
        $match: {
            eventName: { $in: ["page_viewed", "investment_order_created", "investment_order_completed"] },
            timestamp: { $gte: ISODate("2026-01-01"), $lte: ISODate("2026-01-31") }
        }
    },
    {
        $group: {
            _id: "$eventName",
            uniqueUsers: { $addToSet: "$userId" },
            count: { $sum: 1 }
        }
    },
    {
        $project: {
            eventName: "$_id",
            uniqueUsers: { $size: "$uniqueUsers" },
            totalEvents: "$count"
        }
    }
]);

// Result:
// page_viewed: 45,000 users
// investment_order_created: 8,500 users (18.9% conversion)
// investment_order_completed: 7,200 users (84.7% completion rate)

```

**TTL for GDPR compliance:**

```jsx
// Keep analytics for 2 years
db.analytics_events.createIndex(
    { timestamp: 1 },
    { expireAfterSeconds: 63072000 }  // 2 years
);

```

---

## 5. User Feature Flags Collection

```jsx
// Collection: user_feature_flags
{
    _id: ObjectId("507f1f77bcf86cd799439017"),
    userId: 123,  // Reference to PostgreSQL users.id

    // Active flags
    flags: {
        "new_dashboard_ui": {
            enabled: true,
            variant: "B",  // A/B test variant
            enrolledAt: ISODate("2026-01-15T00:00:00Z")
        },
        "tax_loss_harvesting": {
            enabled: false,
            reason: "not_premium_user"
        },
        "crypto_investments": {
            enabled: true,
            variant: "beta",
            enrolledAt: ISODate("2026-01-10T00:00:00Z")
        },
        "ai_financial_coach": {
            enabled: true,
            variant: "A",
            enrolledAt: ISODate("2026-01-19T00:00:00Z")
        }
    },

    // Metadata
    lastUpdatedAt: ISODate("2026-01-19T10:00:00Z"),

    // Indexes
    // db.user_feature_flags.createIndex({ userId: 1 }, { unique: true })
}

```

**Why MongoDB for feature flags?**

- **Flexible flags**: Each user can have different flags
- **Fast reads**: Single query gets all flags for user
- **Easy updates**: Add new flags without schema change
- **A/B testing**: Store variant assignments

**Usage in application:**

```jsx
// Check if user has access to feature
const flags = await db.user_feature_flags.findOne({ userId: 123 });

if (flags?.flags?.["new_dashboard_ui"]?.enabled) {
    // Show new dashboard
} else {
    // Show old dashboard
}

```

---