## Understanding RabbitMQ Basics First

Before we dive into your work, let me explain RabbitMQ like you're learning it for the first time:

**What is RabbitMQ?**
Think of RabbitMQ as a post office for your application:

- **Publisher** = Person sending a letter
- **Exchange** = Post office sorting center (decides where letters go)
- **Queue** = Mailbox where letters wait
- **Consumer** = Person picking up letters from mailbox

**Why not just call the other service directly?**

```tsx
// ❌ Direct call - BAD
async createOrder() {
  await this.investmentEngine.calculateAllocation(); // What if this service is down?
  // Your API crashes! User sees error!
}

// ✅ With RabbitMQ - GOOD
async createOrder() {
  await this.rabbitmq.publish('order.created', orderData);
  return { status: 'processing' }; // User gets immediate response
  // Investment Engine processes whenever it's ready
}

```

**Benefits:**

1. **Decoupling**: Services don't need to know about each other
2. **Reliability**: If consumer is down, message waits in queue
3. **Async**: User doesn't wait for slow operations

---

# Situation 1: Budget Alert Notifications (Month 2-3)

## **Situation:**

Users were exceeding their budgets but weren't being notified. Product wanted real-time alerts when spending exceeded 80% of budget limit.

I had never used RabbitMQ before - only watched some system design videos. Sofia (senior engineer) would pair program with me to teach me.

This was my **first time working with:**

- RabbitMQ (message broker)
- Event-driven architecture
- Asynchronous communication between services

## Task:

Build notification flow:

1. Core API detects budget exceeded
2. Publish event to RabbitMQ
3. Notification Service consumes event
4. Send email/push notification

Sofia would teach me RabbitMQ concepts while we built this together.

---

## **Action:**

### Week 1: Learning RabbitMQ Concepts (With Sofia)

**Sofia's Teaching Session:**

**Me:** "Why do we need RabbitMQ? Can't Core API just call Notification Service directly?"

**Sofia:** "Let me show you the problem..."

```tsx
// ❌ What happens WITHOUT RabbitMQ (Direct call):
async checkBudget(userId: number) {
  const exceeded = await this.calculateIfExceeded(userId);

  if (exceeded) {
    // Direct call to Notification Service
    await this.notificationService.sendEmail(userId, 'budget alert');
    // ^ What if Notification Service is down?
    // ^ What if SendGrid API is slow (5 seconds)?
    // ^ User's budget check takes 5+ seconds! BAD UX!
  }

  return exceeded;
}

```

**Problems with direct calls:**

1. If Notification Service crashes, Core API crashes too
2. User waits for email to send (slow!)
3. Tight coupling - Core API needs to know about Notification Service

**Sofia:** "Now with RabbitMQ..."

```tsx
// ✅ WITH RabbitMQ (Async):
async checkBudget(userId: number) {
  const exceeded = await this.calculateIfExceeded(userId);

  if (exceeded) {
    // Just publish event and forget
    await this.rabbitmq.emit('budget.exceeded', { userId, amount: 650 });
    // ^ Takes 5 milliseconds
    // ^ User gets instant response
  }

  return exceeded; // Returns immediately!
}

```

**Me:** "So RabbitMQ is like... dropping a letter in a mailbox and walking away?"

**Sofia:** "Exactly! You don't wait for the postman to deliver it."

---

**Understanding Exchange and Queue**

**Sofia drew this on whiteboard:**

```
Core API                    RabbitMQ                    Notification Service
   |                          |                               |
   |  1. Publish message      |                               |
   |------------------------->|                               |
   |  "budget.exceeded"       |                               |
   |                          |                               |
   |                          | 2. Store in queue             |
   |                          |    (waiting...)               |
   |                          |                               |
   |                          |  3. Deliver when ready        |
   |                          |------------------------------>|
   |                          |                               |
   |                          |                               | 4. Process
   |                          |                               |    (send email)
   |                          |                               |
   |                          |  5. Acknowledge (ACK)         |
   |                          |<------------------------------|
   |                          |                               |
   |                          | 6. Remove from queue          |

```

**Key concepts Sofia taught me:**

**1. Exchange:**
Think of it as a sorting center.

```tsx
// In FinVerse, we have different exchanges:
notifications_exchange  // For all notification-related events
orders_exchange        // For order-related events
accounts_exchange      // For account sync events

```

**2. Queue:**
Think of it as a mailbox where messages wait.

```tsx
// Different queues for different purposes:
email_notifications_queue  // Email worker picks from here
push_notifications_queue   // Push worker picks from here
budget_alerts_queue       // Special budget alert handler

```

**3. Routing Key:**
Think of it as an address label on the letter.

```tsx
// Examples of routing keys:
'notification.budget.exceeded.email'  // Goes to email queue
'notification.budget.exceeded.push'   // Goes to push queue
'order.created'                       // Goes to order processing queue

```

---

### Week 2: Implementation - Part 1 (Core API - Publishing Events)

**Step 1: Install and Configure RabbitMQ in Core API**

Sofia helped me add RabbitMQ to our NestJS project:

```tsx
// budget.module.ts
import { Module } from '@nestjs/common';
import { ClientsModule, Transport } from '@nestjs/microservices';

@Module({
  imports: [
    ClientsModule.register([
      {
        name: 'RABBITMQ_SERVICE',  // Give it a name
        transport: Transport.RMQ,   // RabbitMQ transport
        options: {
          urls: [process.env.RABBITMQ_URL], // Connection URL
          queue: 'notifications_queue',      // Which queue to use
          queueOptions: {
            durable: true,  // Queue survives server restart
          },
        },
      },
    ]),
  ],
  // ... rest of module
})
export class BudgetModule {}

```

**Me:** "What does `durable: true` mean?"

**Sofia:** "If RabbitMQ server restarts, the queue doesn't disappear. Messages are saved to disk."

---

**Step 2: Inject RabbitMQ Client in Service**

```tsx
// budget.service.ts
import { Injectable, Inject } from '@nestjs/common';
import { ClientProxy } from '@nestjs/microservices';

@Injectable()
export class BudgetService {
  constructor(
    @Inject('RABBITMQ_SERVICE') private rabbitmqClient: ClientProxy,
    // ^ This gives us access to RabbitMQ
  ) {}

  async checkBudgetThreshold(userId: number, budgetId: number) {
    // ... (your budget checking logic)

    if (percentageSpent >= 80) {
      // Publish event to RabbitMQ
      await this.publishBudgetExceededEvent(userId, budgetData);
    }
  }
}

```

**Me:** "What's `@Inject()`?"

**Sofia:** "It's dependency injection. NestJS creates the RabbitMQ client and gives it to your service. You don't create it yourself."

---

**Step 3: Publishing the Event**

This was my first time writing code to publish to RabbitMQ:

```tsx
async publishBudgetExceededEvent(userId: number, budget: any) {
  // Create event payload
  const event = {
    userId: userId,
    budgetId: budget.id,
    category: budget.category,      // e.g., "food"
    spent: budget.spent_cents / 100,    // €650
    limit: budget.limit_cents / 100,    // €600
    percentage: 108,  // 108% of budget
    timestamp: new Date(),
  };

  // Publish to RabbitMQ
  await this.rabbitmqClient.emit('budget.exceeded', event);
  //                          ^ routing key  ^ payload

  console.log('Published budget exceeded event:', event);
}

```

**Breaking down the `emit()` method:**

```tsx
this.rabbitmqClient.emit(
  'budget.exceeded',  // Routing key (like an address)
  event               // The actual message (like letter content)
);

```

**Me:** "Where does this message go?"

**Sofia:** "To the exchange! The exchange looks at the routing key `'budget.exceeded'` and routes it to queues that match this pattern."

---

**My First Bug: Nothing Happened!**

When I first ran this code:

1. Message was published (I saw the console.log)
2. But Notification Service didn't receive anything!

**Me (panicking):** "Sofia, it's not working! The message disappeared!"

**Sofia:** "Let's check RabbitMQ Management UI..."

We opened RabbitMQ dashboard at `https://b-xxxx.mq.us-east-1.amazonaws.com`

**What we saw:**

- Exchange `notifications_exchange` existed ✅
- Queue `notifications_queue` existed ✅
- But... **NO BINDING between them!** ❌

**Sofia explained:** "Think of it like this:"

```
Post Office (Exchange)    Mailbox (Queue)
       |                      |
       |                      |
       X  NO CONNECTION  X

```

"Messages arrive at exchange, but exchange doesn't know where to send them. They're lost!"

---

**The Fix: Creating Binding**

We needed to tell RabbitMQ: "When a message with routing key `budget.exceeded` arrives at `notifications_exchange`, send it to `budget_alerts_queue`"

This was done in infrastructure configuration (AWS console or Terraform):

```tsx
// Conceptually (not actual code I wrote, DevOps did this):
exchange: 'notifications_exchange'
queue: 'budget_alerts_queue'
binding_key: 'budget.exceeded'

```

Now the flow worked:

```
Core API
  → publishes 'budget.exceeded'
    → arrives at notifications_exchange
      → exchange checks binding rules
        → finds match: 'budget.exceeded' → budget_alerts_queue
          → message delivered to queue ✅

```

---

### Week 3: Implementation - Part 2 (Notification Service - Consuming Events)

Now I moved to Notification Service codebase to consume messages.

**Step 1: Creating RabbitMQ Consumer**

This was my **first time writing a consumer**:

```tsx
// budget-alert.consumer.ts
import { Controller } from '@nestjs/common';
import { MessagePattern, Payload } from '@nestjs/microservices';

@Controller()
export class BudgetAlertConsumer {
  constructor(
    private readonly notificationService: NotificationService,
  ) {}

  @MessagePattern('budget.exceeded')
  //             ^ This matches the routing key!
  async handleBudgetExceeded(@Payload() data: any) {
    console.log('Received budget exceeded event:', data);

    try {
      // Process the event
      await this.notificationService.sendBudgetAlert(data);
    } catch (error) {
      console.error('Failed to process budget alert:', error);
      // Don't throw! Throwing causes infinite retries
    }
  }
}

```

**Me:** "What's `@MessagePattern()`?"

**Sofia:** "It's like a route handler, but for RabbitMQ messages instead of HTTP requests."

```tsx
// HTTP Controller:
@Get('/users')  // Handles GET /users
async getUsers() { }

// RabbitMQ Consumer:
@MessagePattern('budget.exceeded')  // Handles messages with this routing key
async handleBudgetExceeded() { }

```

---

**Step 2: Processing the Event**

```tsx
async sendBudgetAlert(event: any) {
  // Extract data from event
  const { userId, category, spent, limit } = event;

  // Fetch user details from database
  const user = await this.db.query(
    'SELECT email, first_name FROM users WHERE id = $1',
    [userId]
  );

  // Fetch email template
  const template = await this.getTemplate('budget_exceeded');

  // Add job to BullMQ (we'll cover this next!)
  await this.emailQueue.add('send-budget-alert', {
    to: user.email,
    subject: `Budget Alert: ${category}`,
    body: this.renderTemplate(template, {
      name: user.first_name,
      category,
      spent,
      limit
    }),
  });

  console.log('Budget alert email queued for', user.email);
}

```

---

### Understanding ACK (Acknowledgment)

**Sofia's explanation:**

"When Notification Service receives a message, RabbitMQ waits for confirmation that it was processed successfully. This is called ACK (acknowledgment)."

**Three outcomes:**

**1. Success (ACK):**

```tsx
@MessagePattern('budget.exceeded')
async handleBudgetExceeded(@Payload() data: any) {
  await this.processAlert(data);
  // If no error thrown, NestJS automatically sends ACK
  // Message removed from queue ✅
}

```

**2. Failure (NACK):**

```tsx
@MessagePattern('budget.exceeded')
async handleBudgetExceeded(@Payload() data: any) {
  throw new Error('Database is down!');
  // Error thrown = NACK sent
  // Message stays in queue and will be retried 🔄
}

```

**3. REJECT:**

```tsx
@MessagePattern('budget.exceeded')
async handleBudgetExceeded(@Payload() data: any) {
  if (invalidData(data)) {
    // This message is malformed, don't retry
    return { status: 'rejected' };
    // Message sent to dead letter queue ⚠️
  }
}

```

---

### Week 4: Testing and Debugging

**Local Testing:**

I ran both services locally:

1. Core API (publishes events)
2. Notification Service (consumes events)
3. RabbitMQ running in Docker

**Testing flow:**

```bash
# Terminal 1: Start RabbitMQ
docker-compose up rabbitmq

# Terminal 2: Start Core API
npm run start:dev

# Terminal 3: Start Notification Service
cd ../notification-service
npm run start:dev

# Terminal 4: Trigger budget exceeded
curl -X POST http://localhost:3000/test/trigger-budget-exceeded

```

**What I saw in logs:**

```
[Core API] Published budget exceeded event: { userId: 123, ... }
[RabbitMQ] Message received in budget_alerts_queue
[Notification Service] Received budget exceeded event: { userId: 123, ... }
[Notification Service] Budget alert email queued

```

It worked! 🎉

---

**Staging Deployment Bug:**

When we deployed to staging, notifications stopped working!

**Me:** "Sofia, it worked locally but not in staging!"

**Debugging steps:**

1. Checked RabbitMQ Management UI
    - Exchange: exists ✅
    - Queue: exists ✅
    - Messages: **accumulating in queue!** 📈 (Not being consumed)
2. Checked Notification Service logs
    - **ERROR: Connection refused to RabbitMQ**

**Problem:** Environment variable wrong!

```tsx
// .env.staging (WRONG)
RABBITMQ_URL=localhost:5672

// Should be:
RABBITMQ_URL=b-xxxx.mq.us-east-1.amazonaws.com:5672

```

**Sofia:** "Localhost works on your machine because RabbitMQ is running locally. In staging, services are on different servers!"

Fixed the environment variable → deployed → worked! ✅

---

## Result:

- Feature delivered: Budget alerts working end-to-end
- User feedback: 15% reduction in overspending in first month
- Technical growth:
    - Understood async messaging with RabbitMQ
    - Learned about exchanges, queues, routing keys
    - Debugged distributed systems
    - Understood importance of environment configuration
- Time spent: 3 weeks (with lots of learning and pair programming)

---

# Situation 2: Savings Goals - Milestone Notifications (Month 4-6)

Situation:
I was building the Savings Goals feature from scratch. When users contribute money to their goals, they should receive congratulatory notifications at milestones (25%, 50%, 75%, 100% completion).

By now, I had basic RabbitMQ understanding from the budget alerts work, but this was my **first time designing the event flow myself** (not just following Sofia's instructions).

## Task:

Design and implement milestone notification system:

1. Detect when user crosses a milestone (25% → 30% = crossed 25% milestone)
2. Publish event to RabbitMQ
3. Notification Service sends congratulatory message

## Action:

### My Design (Learning to Think Event-Driven)

I sketched this flow:

```
User contributes €100 to goal
  ↓
Core API: Update goal amount in database
  ↓
Core API: Check if milestone crossed
  ↓
IF milestone crossed:
  Publish 'goal.milestone.reached' event
  ↓
RabbitMQ: Route to notification queues
  ↓
Notification Service: Send email + push

```

**Anna reviewed my design:** "Good! You're thinking event-driven now. But what routing key will you use?"

**Me:** "Um... just `goal.milestone`?"

**Anna:** "Think about this - what if we want separate handling for email vs push? What if analytics wants to track milestones?"

**Me:** "Oh! Should I publish separate events for each channel?"

**Anna:** "Not separate events - one event, but use routing key pattern that allows flexible routing."

---

### Understanding Routing Key Patterns

**Anna's teaching:**

"Use a pattern: `notification.goal.milestone.{channel}`"

**Examples:**

```tsx
'notification.goal.milestone.email'  // Goes to email queue
'notification.goal.milestone.push'   // Goes to push queue

```

**But wait - do I publish twice?**

**Anna:** "No! You publish ONCE to the exchange with routing key `notification.goal.milestone`, and the exchange routes it to MULTIPLE queues based on bindings."

**How it works:**

```tsx
// I publish ONCE:
await this.rabbitmq.emit('goal.milestone.reached', {
  userId: 123,
  goalName: 'Emergency Fund',
  milestone: 25,
  currentAmount: 2500,
  targetAmount: 10000,
});

```

**RabbitMQ routing magic:**

```
Message arrives at notifications_exchange
  ↓
Exchange checks ALL bindings:

  Binding 1: pattern 'notification.*.email' → email_notifications_queue
  Does 'goal.milestone.reached' match? NO ❌

  Binding 2: pattern 'goal.*' → goal_tracking_queue
  Does 'goal.milestone.reached' match? YES ✅ → Send to this queue

  Binding 3: pattern '*' → analytics_events_queue
  Does 'goal.milestone.reached' match? YES ✅ → Send to this queue

```

**One message → Multiple queues!**

---

### Implementation: Publishing Milestone Events

**In my savings goals service:**

```tsx
// goals.service.ts
async contribute(goalId: number, amount: number) {
  const goal = await this.findOne(goalId);

  // Calculate progress BEFORE update
  const previousProgress = (goal.currentAmount / goal.targetAmount) * 100;

  // Update database
  const newAmount = goal.currentAmount + amount;
  await this.db.query(
    'UPDATE savings_goals SET current_amount_cents = $1 WHERE id = $2',
    [newAmount * 100, goalId]
  );

  // Calculate progress AFTER update
  const newProgress = (newAmount / goal.targetAmount) * 100;

  // Check which milestones were crossed
  const milestones = [25, 50, 75, 100];

  for (const milestone of milestones) {
    if (previousProgress < milestone && newProgress >= milestone) {
      // Milestone crossed! Publish event
      await this.publishMilestoneEvent(goal, milestone, newAmount);
    }
  }
}

```

**Me thinking through the logic:**

"If user was at 24% and now at 30%, they crossed the 25% milestone. But not 50%."

```tsx
// Example calculation:
previousProgress = 24%
newProgress = 30%

milestone 25: previousProgress (24) < 25 AND newProgress (30) >= 25 → TRUE ✅
milestone 50: previousProgress (24) < 50 AND newProgress (30) >= 50 → FALSE ❌

```

---

**Publishing the event:**

```tsx
async publishMilestoneEvent(
  goal: any,
  milestone: number,
  currentAmount: number,
) {
  const event = {
    userId: goal.user_id,
    goalId: goal.id,
    goalName: goal.name,
    milestone: milestone,           // 25, 50, 75, or 100
    currentAmount: currentAmount,
    targetAmount: goal.target_amount_cents / 100,
    percentageComplete: milestone,
    timestamp: new Date(),
  };

  // Publish to RabbitMQ
  await this.rabbitmqClient.emit('goal.milestone.reached', event);

  console.log(`Published milestone event: ${goal.name} reached ${milestone}%`);
}

```

---

### Configuration: Setting Up Multiple Queues

**I learned that one event can go to multiple queues through bindings.**

**In RabbitMQ configuration (done by DevOps, but I needed to understand it):**

```
Exchange: notifications_exchange
  ↓
Bindings:

  1. Routing pattern: 'goal.*'
     → Queue: goal_notifications_queue
     → Consumer: Notification Service (specialized goal handler)

  2. Routing pattern: 'notification.*.email'
     → Queue: email_notifications_queue
     → Consumer: Notification Service (email worker)

  3. Routing pattern: 'notification.*.push'
     → Queue: push_notifications_queue
     → Consumer: Notification Service (push worker)

  4. Routing pattern: '#' (matches everything)
     → Queue: analytics_events_queue
     → Consumer: Analytics Service

```

**When I publish `goal.milestone.reached`:**

- Goes to `goal_notifications_queue` (matches `goal.*`) ✅
- Does NOT go to `email_notifications_queue` (doesn't match `notification.*.email`) ❌
- Goes to `analytics_events_queue` (matches `#`) ✅

**But wait!** For notifications, I also wanted email AND push.

**Anna:** "You need to publish to the notifications exchange too, with proper routing key."

---

**Revised approach:**

```tsx
async publishMilestoneEvent(goal: any, milestone: number, currentAmount: number) {
  const baseEvent = {
    userId: goal.user_id,
    goalId: goal.id,
    goalName: goal.name,
    milestone: milestone,
    currentAmount: currentAmount,
    targetAmount: goal.target_amount_cents / 100,
  };

  // Publish to notifications exchange for email
  await this.rabbitmqClient.emit(
    'notification.goal.milestone.email',
    //             ↑      ↑        ↑
    //          type   detail  channel
    baseEvent
  );

  // Publish to notifications exchange for push
  await this.rabbitmqClient.emit(
    'notification.goal.milestone.push',
    baseEvent
  );

  // Publish to analytics
  await this.rabbitmqClient.emit(
    'goal.milestone.reached',
    baseEvent
  );
}

```

**Me:** "Wait, why three separate publishes? Can't one message go to all?"

**Anna:** "Good question! You CAN, but then you need to be careful with routing keys and exchange configuration. For now, explicit is clearer. Later, we can optimize to single publish with multiple bindings."

---

### Testing Milestone Detection

**I wrote tests to ensure milestone logic was correct:**

```tsx
describe('GoalsService - Milestone Detection', () => {
  it('should detect 25% milestone crossed', async () => {
    // Setup: Goal at 20% (€2000 of €10000)
    const goal = {
      id: 1,
      currentAmount: 2000,
      targetAmount: 10000,
    };

    // User contributes €1000
    // New amount = €3000 (30%)
    await service.contribute(goal.id, 1000);

    // Expect: Milestone event published
    expect(rabbitmqClient.emit).toHaveBeenCalledWith(
      'goal.milestone.reached',
      expect.objectContaining({
        milestone: 25,
        goalId: 1,
      })
    );
  });

  it('should NOT publish milestone if already passed', async () => {
    // Setup: Goal at 30% (already passed 25%)
    const goal = {
      id: 1,
      currentAmount: 3000,
      targetAmount: 10000,
    };

    // User contributes €500
    // New amount = €3500 (35%)
    // No new milestone crossed
    await service.contribute(goal.id, 500);

    // Expect: NO milestone event
    expect(rabbitmqClient.emit).not.toHaveBeenCalled();
  });

  it('should detect multiple milestones in one contribution', async () => {
    // Setup: Goal at 20%
    const goal = {
      id: 1,
      currentAmount: 2000,
      targetAmount: 10000,
    };

    // User contributes €4000 (big contribution!)
    // New amount = €6000 (60%)
    // Crosses 25%, 50%
    await service.contribute(goal.id, 4000);

    // Expect: TWO milestone events
    expect(rabbitmqClient.emit).toHaveBeenCalledTimes(2);
    expect(rabbitmqClient.emit).toHaveBeenCalledWith(
      'goal.milestone.reached',
      expect.objectContaining({ milestone: 25 })
    );
    expect(rabbitmqClient.emit).toHaveBeenCalledWith(
      'goal.milestone.reached',
      expect.objectContaining({ milestone: 50 })
    );
  });
});

```

---

## Result:

- Feature delivered: Milestone notifications working
- User feedback: Users loved getting encouragement messages
- Technical growth:
    - Designed event flow independently
    - Understood routing key patterns
    - Learned how one event can go to multiple queues
    - Wrote comprehensive tests for event logic
- Confidence boost: "I can design event-driven features now!"
- Time: 2 weeks for milestone notification part (within larger 6-week savings goals feature)

# Situation 3: Recurring Investments - Complex Multi-Service Event Orchestration (Month 7-12)

## Situation:

Users wanted automated investing - set up a plan to invest €200 every month automatically. This required complex coordination across THREE services:

1. **Core API** (scheduling, orchestration)
2. **Investment Engine** (Go service - calculates allocation)
3. **Transaction Service** (Go service - executes trades)

This was **the first time I designed a multi-service event-driven flow**. Previously, I only worked within Core API or published to Notification Service. Now I needed to coordinate services I didn't even write code for (Go services owned by Liam and Dmitri).

I had 9 months of experience now but had never:

- Orchestrated events across multiple services
- Designed complex event chains
- Dealt with failure scenarios in distributed systems

## Task:

Design and implement end-to-end recurring investment system using event-driven architecture with RabbitMQ.

---

## Action:

### Week 1: Design Phase - Initial (Wrong) Approach

**My first attempt (synchronous thinking):**

```tsx
// ❌ BAD - Thinking synchronously
@Processor('recurring-investments-queue')
export class RecurringInvestmentWorker {
  @Process('execute-recurring-investment')
  async handleExecution(job: Job) {
    const { planId } = job.data;

    // Step 1: Call Investment Engine API directly
    const allocation = await this.investmentEngineApi.calculateAllocation({
      planId,
      amount: 200,
    });

    // Step 2: Call Transaction Service API directly
    const result = await this.transactionServiceApi.executeOrder({
      allocation,
    });

    // Step 3: Send notification
    await this.notificationApi.sendConfirmation(result);
  }
}

```

**Me presenting this to Marcus (Tech Lead):**

**Marcus:** "What if Investment Engine is down when this runs?"

**Me:** "Um... the job fails?"

**Marcus:** "Then user misses their monthly investment. Not acceptable. What else?"

**Me:** "We could retry the job?"

**Marcus:** "For how long? What if it's down for 2 hours? The BullMQ worker is blocked waiting. Meanwhile, 100 other recurring investments are queued up behind it."

**Me (realizing):** "Oh... so direct API calls create dependencies..."

**Marcus:** "Exactly. Plus, what if Investment Engine calculates allocation successfully, but then Transaction Service is down? You've calculated but can't execute. How do you remember where you left off?"

---

### Understanding Event-Driven Orchestration

**Marcus drew this on whiteboard:**

**❌ Synchronous (tight coupling):**

```
Worker
  ↓ (waits...)
Investment Engine (30 seconds)
  ↓ (waits...)
Transaction Service (60 seconds)
  ↓ (waits...)
Notification Service (5 seconds)
  ↓
Done (95 seconds total)

Problems:
- Worker blocked for 95 seconds
- If any service fails, entire chain fails
- No visibility into which step failed

```

**✅ Event-Driven (loose coupling):**

```
Worker → Publish event → Done (5ms)
           ↓
       RabbitMQ Queue
           ↓
   Investment Engine consumes → Calculates → Publishes next event → Done
                                                      ↓
                                                  RabbitMQ Queue
                                                      ↓
                                     Transaction Service consumes → Executes → Publishes completion → Done
                                                                                         ↓
                                                                                     RabbitMQ Queue
                                                                                         ↓
                                                                        Notification Service consumes → Sends email → Done

Benefits:
- Each service independent
- Automatic retries via RabbitMQ
- Clear audit trail (can see which step completed)
- Services don't wait for each other

```

---

### Week 2: Revised Design (Event-Driven)

**Event flow I designed with Marcus's guidance:**

```
1. BullMQ Scheduler triggers at scheduled time (1st of month)
   ↓
2. Core API Worker publishes: 'recurring_investment.due'
   ↓
3. RabbitMQ routes to: investment_orders_queue
   ↓
4. Investment Engine consumes, calculates allocation
   ↓
5. Investment Engine publishes: 'allocation.calculated'
   ↓
6. RabbitMQ routes to: transaction_orders_queue
   ↓
7. Transaction Service consumes, executes trade
   ↓
8. Transaction Service publishes: 'recurring_investment.completed'
   ↓
9. RabbitMQ routes to: notification_orders_queue
   ↓
10. Notification Service sends confirmation email

```

**Key insight:** Each service does its job and passes the baton (event) to the next service. No one waits for anyone.

---

### Implementation: Database Schema First

**I designed execution tracking table:**

```sql
CREATE TABLE recurring_investment_executions (
  id BIGSERIAL PRIMARY KEY,
  recurring_investment_id BIGINT REFERENCES recurring_investments(id),
  scheduled_date DATE NOT NULL,
  executed_date TIMESTAMP,
  order_id BIGINT REFERENCES orders(id),
  status VARCHAR(20) NOT NULL,  -- pending, completed, failed, skipped
  failure_reason TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);

```

**Why this table?**

Because events are asynchronous, I need to track state:

- **pending**: Event published, waiting for processing
- **completed**: All steps done successfully
- **failed**: Something went wrong
- **skipped**: Markets closed (weekend)

This allows me to answer: "Did user's recurring investment for January execute?"

---

### Implementation: Publishing Initial Event

```tsx
// recurring-investment.worker.ts (in Core API)
@Processor('recurring-investments-queue')
export class RecurringInvestmentWorker {

  @Process('execute-recurring-investment')
  async handleExecution(job: Job) {
    const { planId } = job.data;

    // Fetch plan details
    const plan = await this.db.query(
      'SELECT * FROM recurring_investments WHERE id = $1',
      [planId]
    );

    // Check if plan is still active
    if (plan.status !== 'active') {
      console.log('Plan cancelled, stopping recurring job');
      await job.remove(); // Stop future executions
      return;
    }

    // Check if it's a weekend (markets closed)
    const today = new Date();
    const dayOfWeek = today.getDay();
    if (dayOfWeek === 0 || dayOfWeek === 6) {
      console.log('Weekend - markets closed');

      // Create execution record as 'skipped'
      await this.db.query(
        `INSERT INTO recurring_investment_executions
         (recurring_investment_id, scheduled_date, status, failure_reason)
         VALUES ($1, $2, $3, $4)`,
        [planId, today, 'skipped', 'Markets closed (weekend)']
      );

      return; // Don't publish event
    }

    // Create execution record as 'pending'
    const execution = await this.db.query(
      `INSERT INTO recurring_investment_executions
       (recurring_investment_id, scheduled_date, status)
       VALUES ($1, $2, $3)
       RETURNING id`,
      [planId, today, 'pending']
    );

    // Publish event to RabbitMQ
    await this.rabbitmqClient.emit('recurring_investment.due', {
      planId: plan.id,
      executionId: execution.id,  // Track this execution
      userId: plan.user_id,
      portfolioId: plan.portfolio_id,
      amount: plan.amount_cents / 100,  // €200
      timestamp: new Date(),
    });

    console.log(`Published recurring investment event for plan ${planId}`);
  }
}

```

**Key learning:** The execution record's `id` becomes our tracking mechanism through the entire event chain.

---

### Week 5: Collaboration with Liam (Investment Engine)

**Pair programming session with Liam:**

**Me:** "Liam, my Core API will publish `recurring_investment.due` events to RabbitMQ. Can you consume them in Investment Engine?"

**Liam:** "Sure, what's the event structure?"

**Me:** "Here's the schema..."

```tsx
{
  "planId": 123,
  "executionId": 456,  // Track this through all services
  "userId": 789,
  "portfolioId": 101,
  "amount": 200.00,
  "timestamp": "2024-02-01T00:00:00Z"
}

```

**Liam:** "Got it. After I calculate allocation, what event should I publish?"

**Me:** "Publish `allocation.calculated` with the allocation details plus the executionId."

**Liam:** "Why executionId?"

**Me:** "So Transaction Service knows which execution record to update when it completes."

---

**Liam's implementation (Go):**

```go
// investment-engine/consumer.go
func (s *InvestmentEngineService) HandleRecurringInvestment(event RecurringInvestmentEvent) error {
    log.Printf("Received recurring investment event: %+v", event)

    // Calculate allocation (same logic as regular orders)
    allocation, err := s.CalculateAllocation(event.PortfolioID, event.Amount)
    if err != nil {
        log.Printf("Failed to calculate allocation: %v", err)
        return err // NACK - will retry
    }

    // Publish next event in chain
    allocationEvent := AllocationEvent{
        ExecutionID: event.ExecutionID,  // Pass along for tracking
        PlanID: event.PlanID,
        UserID: event.UserID,
        PortfolioID: event.PortfolioID,
        Allocation: allocation,
        Timestamp: time.Now(),
    }

    err = s.publishEvent("allocation.calculated", allocationEvent)
    if err != nil {
        log.Printf("Failed to publish allocation event: %v", err)
        return err // NACK - will retry
    }

    log.Printf("Published allocation.calculated for execution %d", event.ExecutionID)
    return nil // ACK - success
}

```

**What I learned from reading Liam's code:**

1. **Error handling in Go is explicit:** Every operation checks `err`
2. **Returning error = NACK:** Message will retry automatically
3. **Returning nil = ACK:** Message processed successfully
4. **executionId is passed through:** This is how we track the entire flow

---

### Understanding Message Flow in RabbitMQ

**Me:** "Liam, which RabbitMQ queue are you consuming from?"

**Liam:** "`investment_orders_queue`"

**Me:** "How does RabbitMQ know to send my event there?"

**Liam explained the routing:**

```
My publish:
  await this.rabbitmqClient.emit('recurring_investment.due', event);

RabbitMQ receives:
  Exchange: orders_exchange (configured in Core API module)
  Routing Key: 'recurring_investment.due'

RabbitMQ checks bindings on orders_exchange:

  Binding 1:
    Pattern: 'order.created'
    Queue: investment_orders_queue
    Match? NO ❌

  Binding 2:
    Pattern: 'recurring_investment.*'
    Queue: investment_orders_queue
    Match? YES ✅ → Message goes here

  Binding 3:
    Pattern: '#' (all events)
    Queue: analytics_events_queue
    Match? YES ✅ → Message also goes here

```

**Me:** "Oh! So ONE message can go to MULTIPLE queues if multiple bindings match?"

**Liam:** "Exactly. That's the power of topic exchanges."

---

### Week 6: Collaboration with Dmitri (Transaction Service)

**Similar pattern with Dmitri:**

**Me:** "Dmitri, after Liam publishes `allocation.calculated`, can you consume it and execute the trade?"

**Dmitri:** "Yes, but what about failure scenarios?"

**Me:** "What do you mean?"

**Dmitri:** "What if user has insufficient funds? What if broker API is down?"

**Me (thinking):** "Um... should we retry?"

**Dmitri:** "Insufficient funds should NOT retry - user doesn't magically get money. But broker API down SHOULD retry."

---

**We designed failure handling together:**

```go
// transaction-service/consumer.go
func (s *TransactionService) ExecuteRecurringInvestment(event AllocationEvent) error {
    log.Printf("Received allocation event: %+v", event)

    // Check user balance
    balance, err := s.GetAccountBalance(event.UserID)
    if err != nil {
        return err // Database error - NACK and retry
    }

    totalAmount := calculateTotalAmount(event.Allocation)

    if balance < totalAmount {
        // Insufficient funds - don't retry!
        log.Printf("User %d has insufficient funds", event.UserID)

        // Publish failure event
        s.publishEvent("recurring_investment.failed", FailureEvent{
            ExecutionID: event.ExecutionID,
            Reason: "insufficient_funds",
            Timestamp: time.Now(),
        })

        return nil // ACK - we handled it (by publishing failure event)
    }

    // Execute trade with broker
    orderID, err := s.ExecuteOrder(event.UserID, event.Allocation)
    if err != nil {
        // Broker error - this is transient, should retry
        log.Printf("Broker API error: %v", err)
        return err // NACK - will retry (with exponential backoff)
    }

    // Success! Publish completion event
    s.publishEvent("recurring_investment.completed", SuccessEvent{
        ExecutionID: event.ExecutionID,
        OrderID: orderID,
        Amount: totalAmount,
        Timestamp: time.Now(),
    })

    return nil // ACK - success
}

```

**Key insight:** Not all errors should retry!

- **Transient errors** (network glitch, API temporarily down) → NACK and retry
- **Permanent errors** (insufficient funds, invalid data) → ACK but publish failure event

---

### Week 7: Consuming Completion Events (Back in Core API)

Now I needed to consume events published by Transaction Service to update my execution records.

**Creating consumers for success and failure:**

```tsx
// recurring-investment-event.consumer.ts (in Core API)
import { Controller } from '@nestjs/common';
import { MessagePattern, Payload } from '@nestjs/microservices';

@Controller()
export class RecurringInvestmentEventConsumer {
  constructor(
    private readonly db: DatabaseService,
    private readonly rabbitmqClient: ClientProxy,
  ) {}

  @MessagePattern('recurring_investment.completed')
  async handleCompleted(@Payload() event: any) {
    console.log('Received completion event:', event);

    const { executionId, orderId, amount } = event;

    // Update execution record in database
    await this.db.query(
      `UPDATE recurring_investment_executions
       SET status = 'completed',
           executed_date = NOW(),
           order_id = $1
       WHERE id = $2`,
      [orderId, executionId]
    );

    console.log(`Execution ${executionId} marked as completed`);

    // Publish notification event
    await this.rabbitmqClient.emit('notification.recurring_investment.success', {
      userId: event.userId,
      amount: amount,
      executionId: executionId,
    });
  }

  @MessagePattern('recurring_investment.failed')
  async handleFailed(@Payload() event: any) {
    console.log('Received failure event:', event);

    const { executionId, reason } = event;

    // Update execution record
    await this.db.query(
      `UPDATE recurring_investment_executions
       SET status = 'failed',
           failure_reason = $1
       WHERE id = $2`,
      [reason, executionId]
    );

    console.log(`Execution ${executionId} marked as failed: ${reason}`);

    // Publish notification event (alert user)
    await this.rabbitmqClient.emit('notification.recurring_investment.failed', {
      userId: event.userId,
      reason: reason,
      executionId: executionId,
    });
  }
}

```

**Me thinking through this:**

"The executionId is like a thread connecting all the events. Each service adds information and passes it along."

```
executionId: 456

Core API creates → status: 'pending'
   ↓
Investment Engine processes → (no DB update, just calculates)
   ↓
Transaction Service executes → (no direct DB update either)
   ↓
Core API receives completion → status: 'completed', order_id: 789

```

---

### Understanding Configuration: Multiple Consumers

**Me:** "Anna, my Core API is both publishing AND consuming events now. How do I configure this?"

**Anna:** "You need to register both a publisher (ClientProxy) and consumers (MessagePattern handlers)."

```tsx
// recurring-investment.module.ts
import { Module } from '@nestjs/common';
import { ClientsModule, Transport } from '@nestjs/microservices';

@Module({
  imports: [
    // For PUBLISHING events
    ClientsModule.register([
      {
        name: 'RABBITMQ_SERVICE',
        transport: Transport.RMQ,
        options: {
          urls: [process.env.RABBITMQ_URL],
          queue: 'orders_queue',  // Doesn't matter much for publisher
          queueOptions: { durable: true },
        },
      },
    ]),
  ],
  providers: [
    RecurringInvestmentWorker,      // Publishes events
    RecurringInvestmentEventConsumer, // Consumes events
  ],
})
export class RecurringInvestmentModule {}

```

**And in main.ts, enable microservices:**

```tsx
// main.ts
async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  // Enable HTTP server
  await app.listen(3000);

  // ALSO enable RabbitMQ consumer
  app.connectMicroservice({
    transport: Transport.RMQ,
    options: {
      urls: [process.env.RABBITMQ_URL],
      queue: 'core_api_events_queue',  // This service consumes from here
      queueOptions: { durable: true },
    },
  });

  await app.startAllMicroservices();
}

```

**Me:** "So the same service can both publish AND consume?"

**Anna:** "Yes! Core API publishes to `orders_exchange` and consumes from `core_api_events_queue`."

---

### Week 8: Testing - The Complex Part

**Testing event chains is HARD because:**

1. Multiple services involved
2. Asynchronous - can't test everything in one test
3. Need to test failure scenarios

**My testing strategy:**

**Unit tests (mock RabbitMQ):**

```tsx
describe('RecurringInvestmentWorker', () => {
  it('should publish event on weekday', async () => {
    jest.useFakeTimers().setSystemTime(new Date('2024-02-05')); // Monday

    const job = { data: { planId: 123 } };
    await worker.handleExecution(job);

    // Verify event was published
    expect(rabbitmqClient.emit).toHaveBeenCalledWith(
      'recurring_investment.due',
      expect.objectContaining({
        planId: 123,
        amount: 200,
      })
    );

    // Verify execution record created
    expect(db.query).toHaveBeenCalledWith(
      expect.stringContaining('INSERT INTO recurring_investment_executions'),
      expect.arrayContaining(['pending'])
    );
  });

  it('should skip execution on weekend', async () => {
    jest.useFakeTimers().setSystemTime(new Date('2024-02-03')); // Saturday

    const job = { data: { planId: 123 } };
    await worker.handleExecution(job);

    // Should NOT publish event
    expect(rabbitmqClient.emit).not.toHaveBeenCalled();

    // Should create 'skipped' execution record
    expect(db.query).toHaveBeenCalledWith(
      expect.stringContaining('INSERT INTO recurring_investment_executions'),
      expect.arrayContaining(['skipped', 'Markets closed (weekend)'])
    );
  });
});

```

**Integration tests (staging environment):**

1. Created test recurring plan scheduled for tomorrow
2. Waited 24 hours for BullMQ to trigger
3. Checked execution_record: status = 'pending' ✅
4. Checked Investment Engine logs: "Received recurring investment event" ✅
5. Checked Transaction Service logs: "Executed order" ✅
6. Checked execution_record again: status = 'completed', order_id filled ✅
7. User received email notification ✅

**Full flow worked!** 🎉

---

### Production Deployment: Real Bug (Day 3)

**Production issue found:**

```
5 recurring investments failed with:
  Status: 'skipped'
  Reason: 'Markets closed (weekend)'

But it was TUESDAY!

```

**Me (confused):** "How is Tuesday a weekend??"

**Debugging:**

1. Checked BullMQ logs:

    ```
    Job executed at: 2024-02-06 00:00:00 UTC
    Day of week calculated: 0 (Sunday)
    
    ```

2. **Aha!** The issue:

    ```tsx
    // My code:
    const today = new Date(); // This creates date in UTC!
    const dayOfWeek = today.getDay();
    
    // What actually happened:
    // BullMQ cron: "0 0 1 * *" runs at midnight UTC
    // For German user (UTC+1):
    //   - Local time: Feb 6, 1:00 AM (Tuesday)
    //   - UTC time: Feb 6, 0:00 AM (Tuesday)
    //   - But previous day was Monday...
    // For US markets:
    //   - UTC midnight = Previous day evening in US
    //   - Markets ARE actually closed!
    
    ```


**Root cause:** Timezone mismatch between BullMQ schedule and market hours.

**Fix:**

```tsx
// Change BullMQ cron schedule
// Before: "0 0 1 * *" (midnight UTC)
// After: "0 10 1 * *" (10 AM CET, when US markets are open)

async scheduleRecurringJob(plan: RecurringInvestment) {
  await this.recurringInvestmentsQueue.add(
    'execute-recurring-investment',
    { planId: plan.id },
    {
      repeat: {
        cron: '0 10 1 * *', // 10 AM CET on 1st of month
        tz: 'Europe/Berlin', // Specify timezone!
      },
    }
  );
}

```

**Learning:** Timezones matter in distributed systems! Always be explicit about timezone when scheduling.

---

### Visualizing the Complete Event Chain

```
Time: Feb 1, 10:00 AM CET

[BullMQ Scheduler]
        ↓
    Triggers job
        ↓
[Core API Worker]
        ↓
  Creates execution record (status: pending)
        ↓
  Publishes 'recurring_investment.due'
        ↓
[RabbitMQ orders_exchange]
        ↓
  Routes to investment_orders_queue
        ↓
[Investment Engine (Go) - Liam's code]
        ↓
  Consumes event
        ↓
  Calculates allocation (€200 → 60% stocks, 40% bonds)
        ↓
  Publishes 'allocation.calculated'
        ↓
[RabbitMQ orders_exchange]
        ↓
  Routes to transaction_orders_queue
        ↓
[Transaction Service (Go) - Dmitri's code]
        ↓
  Consumes event
        ↓
  Checks balance (sufficient ✅)
        ↓
  Executes trade with broker
        ↓
  Publishes 'recurring_investment.completed'
        ↓
[RabbitMQ orders_exchange]
        ↓
  Routes to core_api_events_queue
        ↓
[Core API Consumer]
        ↓
  Consumes event
        ↓
  Updates execution record (status: completed, order_id: 789)
        ↓
  Publishes 'notification.recurring_investment.success'
        ↓
[RabbitMQ notifications_exchange]
        ↓
  Routes to email_notifications_queue
        ↓
[Notification Service]
        ↓
  Sends confirmation email

Total time: ~30 seconds (asynchronous)

```

---

## Result:

- Feature delivered: Recurring investments working across 3 services
- Success rate: 98.5% (only 1.5% failed due to insufficient funds - expected)
- User adoption: 800 users in first month
- Technical achievement:
    - Designed complex event-driven orchestration
    - Coordinated with engineers writing Go code (even though I don't write Go)
    - Handled failure scenarios (insufficient funds, broker down, weekend detection)
    - Debugged timezone issues in production
    - Used executionId to track state across multiple services
- Personal growth:
    - "I can architect complex distributed systems!"
    - Event-driven thinking became natural
    - Learned to collaborate across service boundaries
    - Understood importance of tracking mechanisms (executionId)
- Time: 8 weeks

---

# Situation 4: Production Incident - RabbitMQ Queue Backup (Month 9)

## Situation:

One Wednesday afternoon, our monitoring system (DataDog) started alerting:

```
🚨 ALERT: investment_orders_queue depth: 2,500 messages (threshold: 100)
🚨 ALERT: transaction_orders_queue depth: 1,800 messages (threshold: 100)
🚨 Users reporting: "My investment order is stuck in 'processing' for 30 minutes"

```

I was on support rotation that week. This was my **first time handling a RabbitMQ production incident**.

## Task:

Identify why RabbitMQ queues are backing up, fix the issue, and prevent future occurrences.

---

## Action:

### 10 minutes: Initial Panic and Information Gathering

**Me (internal monologue):** "Oh no oh no oh no... queues are backing up! Is RabbitMQ down? Did I break something with recent deployment?"

**What I did:**

1. **Checked RabbitMQ Management UI**

    ```
    URL: https://b-xxxx.mq.us-east-1.amazonaws.com
    
    Status:
    - RabbitMQ server: Running ✅
    - Connection: Healthy ✅
    - Memory: 60% (normal) ✅
    
    Queue: investment_orders_queue
    - Ready: 2,500 messages 📈 (GROWING!)
    - Rate: +50 msg/sec
    - Consumers: 0 ❌❌❌ (PROBLEM!)
    
    ```

2. **The issue became clear:**
    - Messages are being published normally (Core API working)
    - But NO CONSUMERS connected to consume them!
    - Messages piling up like letters in a mailbox with no one checking

**Me:** "Why are there no consumers? Investment Engine service must be down!"

---

### 20 minutes: Checking Investment Engine Service

**Checked AWS ECS (where Investment Engine runs):**

```
Service: investment-engine
Status: RUNNING ✅
Tasks: 2/2 running ✅
Health checks: Passing ✅

```

**Me (confused):** "Service is running... but not consuming from RabbitMQ?"

**Checked Investment Engine logs:**

```
[10:15:32] Investment Engine started successfully
[10:15:32] HTTP server listening on port 8080
[10:15:33] Health check endpoint responding
[10:15:35] ... (no RabbitMQ consumer logs!)

```

**The problem:** Investment Engine's HTTP server was running, but RabbitMQ consumer never started!

---

### 30 minutes: Escalating to Liam

**Me (in Slack):** "@liam Investment Engine is running but not consuming from RabbitMQ. Queue has 2,500+ messages backed up!"

**Liam:** "Let me check... oh no! I deployed a config change yesterday that accidentally disabled the RabbitMQ consumer!"

**What happened:**

```go
// investment-engine/main.go
func main() {
    // Start HTTP server
    startHTTPServer()

    // Start RabbitMQ consumer
    // if os.Getenv("ENABLE_RABBITMQ") == "true" { // ← Liam added this conditional
    //     startRabbitMQConsumer()
    // }

    // But forgot to set ENABLE_RABBITMQ=true in production environment!
}

```

**Liam:** "I'll fix it right now. Deploying..."

---

### 45 minutes: Understanding Message Recovery

While Liam fixed Investment Engine, I learned about message recovery:

**Anna explained to me:**

"The good news: No messages are lost. They're all safely stored in RabbitMQ queue."

"The bad news: 2,500 messages to process. Will take time."

**Me:** "How long?"

**Anna:** "Let's calculate..."

```
Current state:
- Queue depth: 2,500 messages
- Investment Engine processes: ~10 messages/second (with 2 instances)

Recovery time:
2,500 messages ÷ 10 msg/sec = 250 seconds = ~4 minutes

But messages are STILL being published (+50/sec)
So actual recovery: ~10 minutes

```

---

### 50 minutes: Investment Engine Fixed

**Liam:** "Deployed! Consumer is now connected."

**I watched RabbitMQ Management UI:**

```
Time: 14:50
  Queue depth: 2,500
  Consumers: 2 ✅ (Investment Engine reconnected)
  Consume rate: 10 msg/sec

Time: 14:52
  Queue depth: 2,300 (decreasing!)
  Consume rate: 15 msg/sec

Time: 14:55
  Queue depth: 1,800

Time: 15:00
  Queue depth: 850

Time: 15:05
  Queue depth: 50 (almost normal!)

Time: 15:10
  Queue depth: 5 (back to normal!)

```

**Queue drained successfully!** 🎉

---

### Understanding Why Messages Weren't Lost

**Me:** "Anna, why didn't the messages disappear when Investment Engine was down?"

**Anna's explanation:**

**1. Durable Queue:**

```tsx
// When we configured the queue:
queueOptions: {
  durable: true,  // ← Messages saved to disk
}

```

"If `durable: false`, messages only in memory. Server restart = lost."

"With `durable: true`, messages persist to disk. Server restart = messages still there."

---

**2. No Auto-Delete:**

```tsx
queueOptions: {
  durable: true,
  autoDelete: false,  // ← Queue doesn't delete itself when no consumers
}

```

"If `autoDelete: true`, queue disappears when last consumer disconnects."

"With `autoDelete: false`, queue stays even with 0 consumers. Messages wait."

---

**3. Message Acknowledgment (ACK):**

"Messages are only removed from queue after consumer sends ACK."

```
Flow:
1. RabbitMQ delivers message to consumer
2. Consumer processes message
3. Consumer sends ACK (acknowledgment)
4. RabbitMQ removes message from queue

If consumer crashes before ACK:
- RabbitMQ sees no ACK received
- Message stays in queue (or requeued)
- Another consumer can pick it up

```

**In our case:**

- No consumers connected = No delivery = Messages stay in queue ✅
- This is GOOD! Better to wait than lose messages.

---

### What I Learned: Monitoring RabbitMQ

**After incident, Anna taught me monitoring best practices:**

**1. Queue Depth Alerts:**

```
Normal: 0-50 messages in queue
Warning: 100+ messages (consumer slow or down)
Critical: 1,000+ messages (consumer definitely down)

```

**2. Consumer Count Alerts:**

```
Expected: investment_orders_queue should have 2 consumers (2 Investment Engine instances)
Alert if: consumer count = 0 or 1 (instance crashed)

```

**3. Message Rates:**

```
Publish rate: 50 msg/sec
Consume rate: 50 msg/sec
→ Balanced (healthy) ✅

Publish rate: 50 msg/sec
Consume rate: 10 msg/sec
→ Queue growing (problem!) ❌

```

**4. Message Age:**

```
Average age: 5 seconds (normal)
Average age: 10 minutes (consumer too slow!)
Oldest message: 30 minutes (something's stuck!)

```

---

### Debugging Techniques I Learned

**Anna showed me RabbitMQ Management UI features I didn't know:**

**1. Queue Details Page:**

```
Click on queue name → Shows:
- Consumer details (which service connected)
- Consumer tags (identifies which instance)
- Message rates (in/out)
- Bindings (which exchanges route here)

```

**2. Get Messages (Manual Inspection):**

```
In Queue page:
"Get Messages" button
→ Peek at message payload without consuming
→ Helped me verify messages were valid

```

**Example message I inspected:**

```json
{
  "eventId": "evt_12345",
  "eventType": "recurring_investment.due",
  "data": {
    "planId": 789,
    "userId": 123,
    "amount": 200
  }
}

```

"Message looks valid, so problem is consumer not running, not bad data."

---

**3. Purge Queue (Emergency Only):**

```
"Purge Queue" button
→ Deletes ALL messages in queue
→ Use ONLY if messages are corrupted or test data
→ Never for production financial data!

```

Anna warned: "Never purge investment orders! That's real user money!"

---

### Post-Incident: What Could Go Wrong

**Anna quiz:**

**Anna:** "Prasenjit, what if Investment Engine crashed WHILE processing a message?"

**Me:** "Um... message is lost?"

**Anna:** "Think about ACK. When is message removed from queue?"

**Me:** "Oh! After ACK is sent. So if crashes before ACK..."

**Anna:** "Exactly. Message stays in queue or gets requeued. Another instance picks it up."

---

**Example scenario:**

```
1. Investment Engine instance-1 receives order message
2. Starts calculating allocation
3. CRASH! (out of memory)
4. No ACK sent to RabbitMQ
5. After 30 seconds, RabbitMQ times out waiting for ACK
6. Message requeued
7. Investment Engine instance-2 picks up message
8. Processes successfully
9. Sends ACK
10. Message removed from queue

```

**Me:** "But won't this cause duplicate processing?"

**Anna:** "Good question! That's why we use idempotency."

---

### Learning: Idempotency in Event Processing

**Anna explained:**

"What if same message processed twice?"

**Example without idempotency:**

```tsx
// ❌ BAD - Not idempotent
@MessagePattern('order.created')
async handleOrder(event: any) {
  // Process order
  await this.createInvestment(event.orderId, event.amount);
  // If this runs twice, user charged twice! 😱
}

```

**With idempotency:**

```tsx
// ✅ GOOD - Idempotent using Redis
@MessagePattern('order.created')
async handleOrder(event: any) {
  const messageId = event.eventId; // Unique ID

  // Check if already processed
  const alreadyProcessed = await this.redis.get(`processed:${messageId}`);
  if (alreadyProcessed) {
    console.log(`Message ${messageId} already processed, skipping`);
    return; // Still ACK, but don't process again
  }

  // Process order
  await this.createInvestment(event.orderId, event.amount);

  // Mark as processed (keep for 24 hours)
  await this.redis.setex(`processed:${messageId}`, 86400, '1');
}

```

**Me:** "So even if message delivered twice, we only process once?"

**Anna:** "Exactly! Redis tracks what we've already done."

---

### Post-Mortem Meeting (2 hours after incident)

**Team gathered for blameless post-mortem:**

**Timeline:**

```
14:30 - Liam deploys config change to Investment Engine
14:31 - Investment Engine restarts, consumer doesn't start
14:32 - Messages start accumulating in queue
14:35 - First alert: Queue depth > 100
14:40 - Prasenjit (me) starts investigating
14:45 - Identified: No consumers connected
14:50 - Escalated to Liam
14:53 - Liam identifies config issue
14:55 - Fix deployed
15:00 - Consumers reconnected, queue draining
15:10 - Queue back to normal
15:15 - Incident resolved

```

**Duration:** 45 minutes (acceptable for non-critical incident)

---

**Root Cause:**

```
Conditional logic added to Investment Engine:
  if ENABLE_RABBITMQ=true:
    start consumer
  else:
    skip consumer

Environment variable ENABLE_RABBITMQ not set in production
→ Consumer didn't start
→ Queue backed up

```

---

**What Went Well:**

- ✅ Messages not lost (durable queue worked)
- ✅ Monitoring detected issue quickly (5 minutes)
- ✅ I escalated promptly when stuck
- ✅ Fix deployed quickly (15 minutes)
- ✅ Recovery automatic (queue drained itself)

**What Could Be Better:**

- ❌ No alert for "consumer count = 0" (would've caught immediately)
- ❌ Config change not tested on staging first
- ❌ No automated check: "Is RabbitMQ consumer running?"

---

**Action Items:**

| Who | Action | Deadline |
| --- | --- | --- |
| Prasenjit (me) | Add DataDog alert: consumer count = 0 | This week |
| Liam | Remove conditional logic (consumer always starts) | This week |
| DevOps (Klaus) | Add health check: verify RabbitMQ connection | Next sprint |
| Anna | Document: "RabbitMQ troubleshooting guide" | Next sprint |

---

### My Action Item: Adding Consumer Count Alert

**I implemented in DataDog:**

```yaml
# datadog-monitors.yaml
- name: "RabbitMQ - Investment Orders Queue - No Consumers"
  type: metric alert
  query: |
    avg(last_5m):
    rabbitmq.queue.consumers{queue:investment_orders_queue} < 1
  message: |
    ⚠️ No consumers connected to investment_orders_queue!

    This means Investment Engine is not processing orders.

    Actions:
    1. Check Investment Engine service status in AWS ECS
    2. Check Investment Engine logs for RabbitMQ connection errors
    3. Verify RABBITMQ_URL environment variable is set

    Runbook: https://wiki.finverse.com/rabbitmq-no-consumers

    @pagerduty-engineering

  escalation: critical
  notify: ["@slack-incidents", "@pagerduty-on-call"]

```

**Result:** If consumers drop to 0, we get alerted within 5 minutes instead of waiting for queue to back up.

---

### Documentation I Wrote

**RabbitMQ Troubleshooting Guide**

```markdown
# RabbitMQ Incident Response

## Symptom: Queue Depth Growing

1. **Check RabbitMQ Management UI**
   - URL: https://b-xxxx.mq.us-east-1.amazonaws.com
   - Login with credentials in 1Password

2. **Identify which queue is backing up**
   - investment_orders_queue → Investment Engine issue
   - transaction_orders_queue → Transaction Service issue
   - email_notifications_queue → Notification Service issue

3. **Check consumer count**
   - Expected: 2 consumers (one per service instance)
   - If 0: Service not running or not connected to RabbitMQ
   - If 1: One instance down

4. **If consumers = 0:**
   - Check service status in AWS ECS
   - Check service logs for errors
   - Verify environment variable RABBITMQ_URL is correct
   - Restart service if needed

5. **If consumers > 0 but queue still growing:**
   - Consumer is slow (can't keep up with message rate)
   - Check consumer service CPU/memory usage
   - May need to scale up (add more instances)

## Symptom: Messages in Dead Letter Queue

1. **Check dead_letter_queue in RabbitMQ UI**

2. **Inspect message payload** (click "Get Messages")
   - Look for error patterns
   - Check if data is valid

3. **Common reasons:**
   - Invalid data (malformed JSON)
   - Business logic rejection (insufficient funds)
   - Downstream service error (broker API down)

4. **Resolution:**
   - Fix underlying issue
   - Manually requeue messages from DLQ
   - Or purge if test data

## Emergency Contacts

- RabbitMQ owner: @anna
- Investment Engine: @liam
- Transaction Service: @dmitri
- On-call: Check PagerDuty schedule

```

---

### Visualization: What Happened

```
Normal State:
┌─────────────┐
│  Core API   │
│  (Publisher)│
└──────┬──────┘
       │ Publishes 50 msg/sec
       ↓
┌─────────────────────┐
│   RabbitMQ Queue    │
│  investment_orders  │
│                     │
│  Depth: 5 messages  │ ← Small, healthy
└──────┬──────────────┘
       │ Consumes 50 msg/sec
       ↓
┌─────────────────────┐
│ Investment Engine   │
│  (Consumer)         │
│  2 instances ✅      │
└─────────────────────┘

After Liam's Deploy:
┌─────────────┐
│  Core API   │
│  (Publisher)│
└──────┬──────┘
       │ Still publishing 50 msg/sec
       ↓
┌─────────────────────────┐
│   RabbitMQ Queue        │
│  investment_orders      │
│                         │
│  Depth: 2,500 msgs 📈   │ ← GROWING!
└──────┬──────────────────┘
       │
       X  NO CONSUMERS! ❌
       │
┌─────────────────────┐
│ Investment Engine   │
│  (HTTP running ✅)   │
│  (Consumer not      │
│   started ❌)        │
└─────────────────────┘

After Fix:
┌─────────────┐
│  Core API   │
│  (Publisher)│
└──────┬──────┘
       │ Publishing 50 msg/sec
       ↓
┌─────────────────────────┐
│   RabbitMQ Queue        │
│  investment_orders      │
│                         │
│  Depth: 50 msgs ✅      │ ← Back to normal
└──────┬──────────────────┘
       │ Consuming 50 msg/sec
       ↓
┌─────────────────────┐
│ Investment Engine   │
│  (Consumer)         │
│  2 instances ✅      │ ← FIXED!
└─────────────────────┘

```

---

**Result:**

**User Impact:**

- 200 users experienced delayed order processing (15-45 minutes)
- NO money lost (all orders eventually processed)
- NO duplicate orders (idempotency worked)

**Technical Learning:**

- **First production RabbitMQ incident handled successfully**
- Learned to use RabbitMQ Management UI for debugging
- Understood importance of consumer monitoring
- Learned why durable queues prevent data loss
- Understood message acknowledgment (ACK) mechanism
- Learned idempotency patterns to prevent duplicate processing
- Wrote documentation to help future engineers

**Personal Growth:**

- Stayed calm under pressure (after initial panic!)
- Knew when to escalate (didn't waste time trying to fix Liam's Go code)
- Learned debugging methodology for distributed systems
- Contributed to preventing future incidents (monitoring alerts)

**Team Recognition:**

- Anna in 1-on-1: "You handled this well. Quick diagnosis, good escalation, and you improved our monitoring."
- Liam: "Thanks for catching this quickly. Could've been much worse if queue filled up for hours."

**Time:**

- Active incident: 45 minutes
- Post-mortem meeting: 1 hour
- Documentation and monitoring setup: 4 hours

---

## Summary: My RabbitMQ Journey

**Progression over 15 months:**

**Month 2-3: Budget Alerts**

- First time touching RabbitMQ
- Learned: Exchanges, queues, routing keys, publish/consume
- Heavy pair programming with Sofia
- Published my first event, wrote my first consumer

**Month 4-6: Savings Goals Milestones**

- Designed event flow independently
- Learned: Routing key patterns, multiple queues
- Understood how one event goes to multiple consumers

**Month 7-12: Recurring Investments**

- Complex multi-service orchestration
- Learned: Event chains, failure handling, cross-service coordination
- Used executionId to track state across services
- Debugged timezone issues in production

**Month 9: Production Incident**

- Handled queue backup incident
- Learned: Queue durability, consumer monitoring, idempotency
- Improved monitoring and documentation
- Gained confidence in production debugging

---

**Key Concepts Mastered:**

1. **Publishing Events:**

    ```tsx
    await this.rabbitmqClient.emit('routing.key', payload);
    
    ```

2. **Consuming Events:**

    ```tsx
    @MessagePattern('routing.key')
    async handleEvent(@Payload() data: any) { }
    
    ```

3. **Exchanges & Routing:**
    - Topic exchange for flexible routing
    - Fanout for broadcasting
    - Bindings connect exchange to queue
4. **Reliability:**
    - Durable queues (survive restarts)
    - Message ACK (ensure processing)
    - Idempotency (prevent duplicates)
    - Dead letter queues (handle failures)
5. **Monitoring:**
    - Queue depth (are consumers keeping up?)
    - Consumer count (are services connected?)
    - Message rates (balanced publish/consume?)
    - Message age (how long waiting?)

---

**From nervous beginner to confident practitioner in 15 months!**