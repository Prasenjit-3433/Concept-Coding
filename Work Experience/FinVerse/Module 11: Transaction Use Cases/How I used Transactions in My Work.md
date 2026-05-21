# Database Transactions in Your FinVerse Journey (TypeORM Reality)

Let me rewrite this showing how you ACTUALLY would have used transactions with TypeORM in a real NestJS production environment.

---

# Understanding Transactions First (Java Spring Boot vs NestJS TypeORM)

### In Java/Spring Boot (What You Know)

```java
@Service
public class GoalsService {

    @Transactional  // ← Spring manages everything automatically
    public void contribute(Long goalId, BigDecimal amount) {
        // If ANY operation throws exception, ALL rollback
        Account account = accountRepository.findById(userId);
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        Goal goal = goalRepository.findById(goalId);
        goal.setCurrentAmount(goal.getCurrentAmount().add(amount));
        goalRepository.save(goal);

        Contribution contribution = new Contribution(goalId, amount);
        contributionRepository.save(contribution);

        // Spring automatically:
        // - Begins transaction before method
        // - Commits if successful
        // - Rolls back if exception thrown
    }
}

```

---

### In NestJS with TypeORM (Similar Pattern!)

```tsx
// goals.service.ts
import { Injectable } from '@nestjs/common';
import { InjectDataSource } from '@nestjs/typeorm';
import { DataSource } from 'typeorm';

@Injectable()
export class GoalsService {
    constructor(
        @InjectDataSource() private dataSource: DataSource,
    ) {}

    async contribute(goalId: number, amount: number) {
        // TypeORM transaction - similar to Spring's @Transactional
        return await this.dataSource.transaction(async (manager) => {
            // Everything inside this callback is in one transaction

            // 1. Deduct from account
            const account = await manager.findOne(Account, { where: { userId } });
            account.balanceCents -= amount * 100;
            await manager.save(account);

            // 2. Update goal
            const goal = await manager.findOne(SavingsGoal, { where: { id: goalId } });
            goal.currentAmountCents += amount * 100;
            await manager.save(goal);

            // 3. Create contribution record
            const contribution = manager.create(GoalContribution, {
                goalId,
                userId,
                amountCents: amount * 100,
                type: 'manual',
            });
            await manager.save(contribution);

            // TypeORM automatically:
            // - Begins transaction before callback
            // - Commits if callback completes successfully
            // - Rolls back if any error thrown
        });
    }
}

```

**Key similarity:** Both Spring and TypeORM manage BEGIN/COMMIT/ROLLBACK automatically!

---

## Your Entities (Database Models)

First, let me show you the TypeORM entities you'd have defined:

```tsx
// entities/account.entity.ts
import { Entity, Column, PrimaryGeneratedColumn } from 'typeorm';

@Entity('accounts')
export class Account {
    @PrimaryGeneratedColumn()
    id: number;

    @Column()
    userId: number;

    @Column({ type: 'bigint' })
    balanceCents: number;

    @Column({ default: 'EUR' })
    currency: string;

    @Column({ type: 'timestamp', default: () => 'CURRENT_TIMESTAMP' })
    lastSyncedAt: Date;
}

```

```tsx
// entities/savings-goal.entity.ts
import { Entity, Column, PrimaryGeneratedColumn } from 'typeorm';

@Entity('savings_goals')
export class SavingsGoal {
    @PrimaryGeneratedColumn()
    id: number;

    @Column()
    userId: number;

    @Column()
    name: string;

    @Column({ type: 'bigint' })
    targetAmountCents: number;

    @Column({ type: 'bigint', default: 0 })
    currentAmountCents: number;

    @Column({ default: 'active' })
    status: string;

    @Column({ type: 'timestamp', default: () => 'CURRENT_TIMESTAMP' })
    createdAt: Date;
}

```

```tsx
// entities/goal-contribution.entity.ts
import { Entity, Column, PrimaryGeneratedColumn } from 'typeorm';

@Entity('goal_contributions')
export class GoalContribution {
    @PrimaryGeneratedColumn()
    id: number;

    @Column()
    goalId: number;

    @Column()
    userId: number;

    @Column({ type: 'bigint' })
    amountCents: number;

    @Column()
    type: string; // 'manual' or 'auto'

    @Column({ type: 'timestamp', default: () => 'CURRENT_TIMESTAMP' })
    createdAt: Date;
}

```

```tsx
// entities/transaction.entity.ts
import { Entity, Column, PrimaryGeneratedColumn } from 'typeorm';

@Entity('transactions')
export class Transaction {
    @PrimaryGeneratedColumn()
    id: number;

    @Column()
    userId: number;

    @Column()
    accountId: number;

    @Column({ unique: true })
    externalId: string; // Plaid transaction ID

    @Column({ type: 'bigint' })
    amountCents: number;

    @Column({ nullable: true })
    merchant: string;

    @Column()
    category: string;

    @Column({ type: 'date' })
    transactionDate: Date;

    @Column({ type: 'timestamp', default: () => 'CURRENT_TIMESTAMP' })
    createdAt: Date;
}

```

---

# ✨Situation 1: Savings Goals - Contributing Money (Month 4-6)

### STAR Format

## **Situation:**

You were building the Savings Goals feature. When a user contributes money to their goal, multiple database operations must happen atomically:

1. Deduct €200 from user's account
2. Add €200 to savings goal
3. Create contribution record
4. Check if milestone reached (25%, 50%, 75%, 100%)

**Critical requirement:** These operations MUST succeed together or fail together. If the server crashes after deducting money but before updating the goal, €200 disappears!

**In Spring Boot, you'd just use `@Transactional`. What's the equivalent in NestJS with TypeORM?**

---

## **Task:**

Implement the `contribute()` method using TypeORM transactions to ensure data consistency.

---

## **Action:**

### Week 1: Initial Implementation (WITHOUT Transaction - Bug!)

**My first attempt (WRONG):**

```tsx
// goals.service.ts
import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';

@Injectable()
export class GoalsService {
    constructor(
        @InjectRepository(Account)
        private accountRepository: Repository<Account>,

        @InjectRepository(SavingsGoal)
        private goalRepository: Repository<SavingsGoal>,

        @InjectRepository(GoalContribution)
        private contributionRepository: Repository<GoalContribution>,
    ) {}

    async contribute(goalId: number, userId: number, amount: number) {
        // ❌ NO TRANSACTION - each save() is a separate transaction!

        // Step 1: Deduct from account
        const account = await this.accountRepository.findOne({
            where: { userId }
        });
        account.balanceCents -= amount * 100;
        await this.accountRepository.save(account);

        // ⚠️ WHAT IF SERVER CRASHES HERE?
        // Money deducted but goal not updated!

        // Step 2: Update goal
        const goal = await this.goalRepository.findOne({
            where: { id: goalId }
        });
        goal.currentAmountCents += amount * 100;
        await this.goalRepository.save(goal);

        // Step 3: Create contribution record
        const contribution = this.contributionRepository.create({
            goalId,
            userId,
            amountCents: amount * 100,
            type: 'manual',
        });
        await this.contributionRepository.save(contribution);
    }
}

```

**Problem:** Each `save()` is a separate database transaction!

```
Transaction 1: UPDATE accounts... (commits immediately)
    ↓ [Server could crash here]
Transaction 2: UPDATE savings_goals... (never executes)
Transaction 3: INSERT INTO goal_contributions... (never executes)

Result: €200 disappeared! 💸

```

---

### Week 2: QA Found the Bug

**QA report:** "User contributed €100 to goal. Account shows €100 deducted, but goal still shows €0!"

**What happened:**

```
09:15:32 User clicks "Contribute €100"
09:15:33 Account updated: €1000 → €900 ✅
09:15:33 [Network timeout / Server restart]
09:15:45 User refreshes page
09:15:45 Account: €900 ✅
09:15:45 Goal: €0 ❌ (not updated!)
09:15:45 Contributions table: empty ❌

Where's the €100? Gone! 😱

```

**Me (panicking):** "Anna, money is missing! The account was debited but goal wasn't updated!"

---

### Week 3: Anna Teaches TypeORM Transactions

**Anna:** "This is exactly what transactions are for. In Spring Boot, you'd use `@Transactional`. TypeORM has a similar pattern."

**Anna showed me the official TypeORM docs:**

```tsx
// TypeORM Transaction Pattern
await dataSource.transaction(async (manager) => {
    // All operations here are in ONE transaction
    // Either ALL succeed or ALL rollback
});

```

**Anna explained:**

"The `transaction()` method takes a callback. Everything inside the callback runs in a single database transaction. TypeORM automatically handles BEGIN, COMMIT, and ROLLBACK."

---

### Understanding Transaction Flow

**Visual diagram:**

```
await dataSource.transaction(async (manager) => {
    ↓
    TypeORM executes: BEGIN;
    ↓
    await manager.save(account);    // UPDATE accounts...
    await manager.save(goal);       // UPDATE savings_goals...
    await manager.save(contribution); // INSERT INTO goal_contributions...
    ↓
    [If no error thrown]
    TypeORM executes: COMMIT;
    ✅ All changes persisted

    [If error thrown anywhere]
    TypeORM executes: ROLLBACK;
    ❌ All changes undone
});

```

---

### Week 4: Corrected Implementation (WITH Transaction)

```tsx
// goals.service.ts
import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, DataSource } from 'typeorm';

@Injectable()
export class GoalsService {
    constructor(
        @InjectRepository(SavingsGoal)
        private goalRepository: Repository<SavingsGoal>,

        @InjectRepository(Account)
        private accountRepository: Repository<Account>,

        @InjectRepository(GoalContribution)
        private contributionRepository: Repository<GoalContribution>,

        @InjectDataSource()
        private dataSource: DataSource, // ← Need this for transactions
    ) {}

    async contribute(goalId: number, userId: number, amount: number) {
        // ✅ ALL operations in ONE transaction
        return await this.dataSource.transaction(async (manager) => {
            // Use 'manager' instead of repositories inside transaction

            // Step 1: Fetch and validate account
            const account = await manager.findOne(Account, {
                where: { userId }
            });

            if (!account) {
                throw new Error('Account not found');
            }

            if (account.balanceCents < amount * 100) {
                throw new Error('Insufficient funds');
            }

            // Step 2: Fetch goal
            const goal = await manager.findOne(SavingsGoal, {
                where: { id: goalId }
            });

            if (!goal || goal.userId !== userId) {
                throw new Error('Goal not found');
            }

            if (goal.status !== 'active') {
                throw new Error('Goal is not active');
            }

            // Calculate progress for milestone detection
            const previousAmount = goal.currentAmountCents / 100;
            const newAmount = previousAmount + amount;
            const previousProgress = (previousAmount / goal.targetAmountCents * 100) * 100;
            const newProgress = (newAmount / goal.targetAmountCents * 100) * 100;

            // Step 3: Update account (deduct money)
            account.balanceCents -= amount * 100;
            await manager.save(Account, account);

            // Step 4: Update goal (add money)
            goal.currentAmountCents += amount * 100;

            // If goal completed
            if (goal.currentAmountCents >= goal.targetAmountCents) {
                goal.status = 'completed';
            }

            await manager.save(SavingsGoal, goal);

            // Step 5: Create contribution record
            const contribution = manager.create(GoalContribution, {
                goalId,
                userId,
                amountCents: amount * 100,
                type: 'manual',
                createdAt: new Date(),
            });
            await manager.save(GoalContribution, contribution);

            // Check milestones
            const milestones = [25, 50, 75, 100];
            const reachedMilestones: number[] = [];

            for (const milestone of milestones) {
                if (previousProgress < milestone && newProgress >= milestone) {
                    reachedMilestones.push(milestone);
                }
            }

            // Return result (transaction commits automatically here)
            return {
                success: true,
                newAmount: goal.currentAmountCents / 100,
                newBalance: account.balanceCents / 100,
                milestonesReached: reachedMilestones,
            };

            // TypeORM automatically COMMITS here if no error
        });
        // After transaction completes, publish events
        // (We'll do this outside transaction - explained later)
    }
}

```

---

### Key Points About TypeORM Transactions

**1. Use `manager`, not `repositories` inside transaction:**

```tsx
// ❌ WRONG inside transaction:
await this.goalRepository.save(goal);

// ✅ CORRECT inside transaction:
await manager.save(SavingsGoal, goal);

```

**Why?** The `manager` is transaction-aware. Using the injected repository would create a SEPARATE transaction!

---

**2. Transaction auto-commits on success:**

```tsx
await this.dataSource.transaction(async (manager) => {
    await manager.save(...);
    await manager.save(...);
    return result;
    // ← TypeORM commits automatically here
});

```

**No need to call `COMMIT` manually!**

---

**3. Transaction auto-rolls back on error:**

```tsx
await this.dataSource.transaction(async (manager) => {
    await manager.save(account);
    await manager.save(goal);

    throw new Error('Something failed!');
    // ← TypeORM rolls back automatically
});

// After this, account and goal are UNCHANGED

```

**No need to call `ROLLBACK` manually!**

---

### Testing the Transaction

**Test 1: Normal flow (success):**

```tsx
// User has €1000, goal at €500
await goalsService.contribute(goalId, userId, 100);

// What TypeORM does:
BEGIN;
  UPDATE accounts SET balance_cents = 90000 WHERE id = 123;
  -- Balance: €1000 → €900 ✅

  UPDATE savings_goals SET current_amount_cents = 60000 WHERE id = 456;
  -- Goal: €500 → €600 ✅

  INSERT INTO goal_contributions (goal_id, user_id, amount_cents, type)
  VALUES (456, 123, 10000, 'manual');
  -- Record created ✅
COMMIT;

// Result: All 3 operations persisted atomically ✅

```

---

**Test 2: Insufficient funds (rollback):**

```tsx
// User has €50, tries to contribute €100
try {
    await goalsService.contribute(goalId, userId, 100);
} catch (error) {
    console.log(error.message); // "Insufficient funds"
}

// What TypeORM does:
BEGIN;
  -- Fetch account
  -- Check: balance (5000) < amount (10000)
  -- Throw error
ROLLBACK;

// Result: Nothing changed ✅
// Account still €50 ✅
// Goal unchanged ✅

```

---

**Test 3: Server crash mid-transaction:**

```tsx
BEGIN;
  UPDATE accounts...  ✅ (executed)
  UPDATE savings_goals...  ✅ (executed)
  INSERT INTO goal_contributions... ✅ (executed in memory)
  [SERVER CRASH] 💥
  -- COMMIT never reached

// PostgreSQL behavior:
// Uncommitted transaction automatically rolled back
// Account: unchanged ✅
// Goal: unchanged ✅
// Contribution: not inserted ✅

```

---

### Visual Flow Diagram

```
User clicks "Contribute €100"
    ↓
Controller receives request
    ↓
GoalsService.contribute(goalId, userId, 100)
    ↓
┌─────────────────────────────────────────────────────────────┐
│ dataSource.transaction(async (manager) => {                │
│                                                             │
│   TypeORM: BEGIN TRANSACTION                               │
│                                                             │
│   ┌───────────────────────────────────────────────┐       │
│   │ manager.findOne(Account, ...)                 │       │
│   │ Query: SELECT * FROM accounts WHERE user_id=123│       │
│   │ Result: { id: 1, balanceCents: 100000 }      │       │
│   └───────────────────────────────────────────────┘       │
│                                                             │
│   ┌───────────────────────────────────────────────┐       │
│   │ Validate: balance (100000) >= amount (10000)  │       │
│   │ Result: ✅ Pass                               │       │
│   └───────────────────────────────────────────────┘       │
│                                                             │
│   ┌───────────────────────────────────────────────┐       │
│   │ manager.findOne(SavingsGoal, ...)             │       │
│   │ Query: SELECT * FROM savings_goals WHERE id=456│       │
│   │ Result: { id: 456, currentAmountCents: 50000 }│       │
│   └───────────────────────────────────────────────┘       │
│                                                             │
│   ┌───────────────────────────────────────────────┐       │
│   │ account.balanceCents -= 10000                 │       │
│   │ manager.save(Account, account)                │       │
│   │ Query: UPDATE accounts                         │       │
│   │        SET balance_cents = 90000               │       │
│   │        WHERE id = 1                            │       │
│   └───────────────────────────────────────────────┘       │
│                                                             │
│   ┌───────────────────────────────────────────────┐       │
│   │ goal.currentAmountCents += 10000              │       │
│   │ manager.save(SavingsGoal, goal)               │       │
│   │ Query: UPDATE savings_goals                    │       │
│   │        SET current_amount_cents = 60000        │       │
│   │        WHERE id = 456                          │       │
│   └───────────────────────────────────────────────┘       │
│                                                             │
│   ┌───────────────────────────────────────────────┐       │
│   │ contribution = manager.create(...)             │       │
│   │ manager.save(GoalContribution, contribution)   │       │
│   │ Query: INSERT INTO goal_contributions          │       │
│   │        (goal_id, user_id, amount_cents, type)  │       │
│   │        VALUES (456, 123, 10000, 'manual')      │       │
│   └───────────────────────────────────────────────┘       │
│                                                             │
│   return { success: true, newAmount: 600 };               │
│                                                             │
│   TypeORM: COMMIT TRANSACTION ✅                           │
│                                                             │
│ });                                                        │
└─────────────────────────────────────────────────────────────┘
    ↓
Transaction successful!
    ↓
Publish RabbitMQ events (outside transaction)
    ↓
Return response to user

```

---

## **Result:**

- **Bug fixed:** Money can't disappear anymore
- **Data consistency:** Account and goal always in sync
- **Atomic operations:** All updates succeed together or fail together
- **Code quality:** Clean, readable (similar to Spring Boot's `@Transactional`)
- **Learning:** "TypeORM transactions are like Spring's @Transactional!"
- **Time:** 4 weeks (design, implement, test, fix bug)

---

# ✨Situation 2: Bank Account Sync - Saving Transactions (Month 4-6)

### STAR Format

## **Situation:**

In the bank sync feature, you fetch 50-100 transactions from Plaid API and save them to the database.

**Problem:** If the save process fails halfway (e.g., database connection drops on transaction #25), you'd have:

- First 24 transactions saved ✅
- Remaining 26 transactions lost ❌
- On retry, first 24 would be duplicates! ❌

## **Task:**

Save all transactions atomically using TypeORM - either all 50 succeed or none are saved.

---

## **Action:**

### Implementation with TypeORM Transaction

```tsx
// bank-sync.worker.ts
import { Worker, Job } from 'bullmq';
import { Injectable } from '@nestjs/common';
import { InjectDataSource } from '@nestjs/typeorm';
import { DataSource } from 'typeorm';
import { Transaction } from './entities/transaction.entity';

@Injectable()
export class BankSyncWorker {
    constructor(
        @InjectDataSource() private dataSource: DataSource,
        private plaidService: PlaidService,
        private rabbitmqClient: ClientProxy,
    ) {
        this.startWorker();
    }

    private startWorker() {
        const worker = new Worker('bank-sync-queue', async (job: Job) => {
            const { accountId, userId } = job.data;

            console.log(`[Worker] Syncing account ${accountId}`);

            try {
                // Step 1: Fetch transactions from Plaid API (outside transaction)
                const plaidTransactions = await this.plaidService.getTransactions({
                    accountId,
                    startDate: '2024-01-01',
                    endDate: new Date().toISOString().split('T')[0],
                });

                console.log(`[Worker] Fetched ${plaidTransactions.length} transactions`);

                // Step 2: Save all transactions in ONE transaction
                await this.saveTransactionsInTransaction(userId, accountId, plaidTransactions);

                console.log(`[Worker] Successfully saved ${plaidTransactions.length} transactions`);

                // Step 3: Publish event (after successful save)
                await this.rabbitmqClient.emit('account.synced', {
                    userId,
                    accountId,
                    transactionCount: plaidTransactions.length,
                });

            } catch (error) {
                console.error(`[Worker] Failed to sync account ${accountId}:`, error.message);
                throw error; // BullMQ will retry
            }

        }, {
            connection: { host: process.env.REDIS_HOST, port: 6379 },
            concurrency: 20,
        });
    }

    private async saveTransactionsInTransaction(
        userId: number,
        accountId: number,
        plaidTransactions: any[]
    ) {
        // ✅ ALL 50 transactions saved atomically
        return await this.dataSource.transaction(async (manager) => {
            for (const txn of plaidTransactions) {
                // Check if transaction already exists (idempotency)
                const existing = await manager.findOne(Transaction, {
                    where: { externalId: txn.transaction_id }
                });

                if (existing) {
                    console.log(`Transaction ${txn.transaction_id} already exists, skipping`);
                    continue;
                }

                // Create new transaction entity
                const transaction = manager.create(Transaction, {
                    userId,
                    accountId,
                    externalId: txn.transaction_id,
                    amountCents: Math.round(txn.amount * 100),
                    merchant: txn.merchant_name || 'Unknown',
                    category: txn.category?.[0] || 'uncategorized',
                    transactionDate: new Date(txn.date),
                    createdAt: new Date(),
                });

                // Save transaction
                await manager.save(Transaction, transaction);
            }

            // Update account's last synced timestamp
            await manager.query(
                'UPDATE bank_accounts SET last_synced_at = NOW() WHERE id = $1',
                [accountId]
            );

            // TypeORM commits automatically here
        });
        // If any error thrown inside, TypeORM rolls back ALL inserts
    }
}

```

---

### Why Transaction is Critical Here

**Without transaction:**

```
Syncing 50 transactions:
BEGIN;
  INSERT transaction 1  ✅
COMMIT;

BEGIN;
  INSERT transaction 2  ✅
COMMIT;

...

BEGIN;
  INSERT transaction 24 ✅
COMMIT;

BEGIN;
  INSERT transaction 25 ❌ (Database connection drops)

Result:
  24 transactions saved ✅
  26 transactions lost ❌

Job retries:
  BEGIN;
    INSERT transaction 1 ❌ (Duplicate key error - already exists!)

```

---

**With transaction:**

```
BEGIN;
  INSERT transaction 1
  INSERT transaction 2
  ...
  INSERT transaction 50
  UPDATE bank_accounts SET last_synced_at = NOW()
COMMIT;  ← All or nothing!

If error at transaction 25:
  ROLLBACK;
  -- ALL 50 inserts undone ✅
  -- Database unchanged ✅

Job retries:
  BEGIN;
    INSERT transaction 1 (fresh attempt, no duplicates)
    ...
    INSERT transaction 50
  COMMIT; ✅

```

---

### Optimized Version (Batch Insert with TypeORM)

**Anna suggested:** "50 individual saves is slow. Use bulk insert!"

```tsx
private async saveTransactionsInTransaction(
    userId: number,
    accountId: number,
    plaidTransactions: any[]
) {
    return await this.dataSource.transaction(async (manager) => {
        // Filter out existing transactions first
        const newTransactions: Transaction[] = [];

        for (const txn of plaidTransactions) {
            const existing = await manager.findOne(Transaction, {
                where: { externalId: txn.transaction_id }
            });

            if (!existing) {
                const transaction = manager.create(Transaction, {
                    userId,
                    accountId,
                    externalId: txn.transaction_id,
                    amountCents: Math.round(txn.amount * 100),
                    merchant: txn.merchant_name || 'Unknown',
                    category: txn.category?.[0] || 'uncategorized',
                    transactionDate: new Date(txn.date),
                    createdAt: new Date(),
                });

                newTransactions.push(transaction);
            }
        }

        if (newTransactions.length > 0) {
            // ✅ Bulk insert - ONE database round trip!
            await manager.save(Transaction, newTransactions);
            // TypeORM generates:
            // INSERT INTO transactions (user_id, account_id, ...)
            // VALUES (123, 456, ...), (123, 456, ...), (123, 456, ...)
        }

        // Update last synced
        await manager.query(
            'UPDATE bank_accounts SET last_synced_at = NOW() WHERE id = $1',
            [accountId]
        );
    });
}

```

**Performance:**

```
Before (individual saves): 50 database round trips = 600ms
After (bulk insert): 1 database round trip = 100ms
6x faster! ✅

```

---

## **Result:**

- **Atomicity:** All 50 transactions saved together or none
- **Performance:** Bulk insert 6x faster
- **Idempotency:** Duplicate check prevents re-inserting
- **Reliability:** On retry, clean slate (no partial data)
- **Learning:** "TypeORM makes batch operations easy!"

---

# ✨Situation 3: Recurring Investment Auto-Save (Month 7-12)

### STAR Format

## **Situation:**

When BullMQ triggers auto-save (monthly investment from savings goal), multiple database operations occur:

1. Deduct €200 from user's account
2. Add €200 to savings goal
3. Create contribution record
4. Publish RabbitMQ event

If any step fails, you could have money deducted but no investment recorded!

## **Task:**

Implement auto-save with TypeORM transaction to ensure atomicity.

---

## **Action:**

```tsx
// auto-save.worker.ts
import { Worker, Job } from 'bullmq';
import { Injectable } from '@nestjs/common';
import { InjectDataSource } from '@nestjs/typeorm';
import { DataSource } from 'typeorm';

@Injectable()
export class AutoSaveWorker {
    constructor(
        @InjectDataSource() private dataSource: DataSource,
        private rabbitmqClient: ClientProxy,
    ) {
        this.startWorker();
    }

    private startWorker() {
        const worker = new Worker('auto-save-queue', async (job: Job) => {
            const { goalId, userId, amount } = job.data;

            console.log(`[Worker] Processing auto-save: €${amount / 100} to goal ${goalId}`);

            try {
                // Execute auto-save in transaction
                const result = await this.executeAutoSave(goalId, userId, amount);

                if (result.success) {
                    // Publish event after successful transaction
                    await this.rabbitmqClient.emit('goal.auto_save.completed', {
                        goalId,
                        userId,
                        amount: amount / 100,
                    });

                    console.log(`[Worker] Auto-save successful for goal ${goalId}`);
                } else {
                    console.log(`[Worker] Auto-save skipped: ${result.reason}`);
                }

            } catch (error) {
                console.error(`[Worker] Auto-save failed:`, error.message);
                throw error; // BullMQ will retry
            }

        }, {
            connection: { host: process.env.REDIS_HOST, port: 6379 },
            concurrency: 2,
        });
    }

    private async executeAutoSave(goalId: number, userId: number, amount: number) {
        // ✅ ALL operations in ONE transaction
        return await this.dataSource.transaction(async (manager) => {

            // Step 1: Fetch goal and check if active
            const goal = await manager.findOne(SavingsGoal, {
                where: { id: goalId }
            });

            if (!goal) {
                throw new Error('Goal not found');
            }

            if (goal.status !== 'active') {
                // Goal completed/cancelled - should stop recurring job
                return {
                    success: false,
                    reason: 'goal_not_active'
                };
            }

            // Step 2: Fetch account
            const account = await manager.findOne(Account, {
                where: { userId }
            });

            if (!account) {
                throw new Error('Account not found');
            }

            // Step 3: Check sufficient funds
            if (account.balanceCents < amount) {
                // Insufficient funds - NOT an error, just skip this month
                // Transaction will rollback but job succeeds
                return {
                    success: false,
                    reason: 'insufficient_funds',
                    balance: account.balanceCents / 100,
                    required: amount / 100
                };
            }

            // Step 4: Deduct from account
            account.balanceCents -= amount;
            await manager.save(Account, account);

            // Step 5: Add to goal
            goal.currentAmountCents += amount;
            goal.lastAutoSaveAt = new Date();

            // Check if goal completed
            if (goal.currentAmountCents >= goal.targetAmountCents) {
                goal.status = 'completed';
                goal.completedAt = new Date();
            }

            await manager.save(SavingsGoal, goal);

            // Step 6: Create contribution record
            const contribution = manager.create(GoalContribution, {
                goalId,
                userId,
                amountCents: amount,
                type: 'auto',
                createdAt: new Date(),
            });
            await manager.save(GoalContribution, contribution);

            // Transaction commits automatically here
            return {
                success: true,
                newBalance: account.balanceCents / 100,
                newGoalAmount: goal.currentAmountCents / 100,
                goalCompleted: goal.status === 'completed'
            };
        });
        // TypeORM commits if callback succeeds
        // TypeORM rolls back if error thrown
    }
}

```

---

### Key Decision: When to Return vs When to Throw

**Scenario 1: Insufficient funds (NOT an error):**

```tsx
if (account.balanceCents < amount) {
    // Don't throw - just return failure
    return { success: false, reason: 'insufficient_funds' };
}

```

**What happens:**

```
BEGIN;
  SELECT * FROM accounts WHERE user_id = 123
  -- Balance: €50, Required: €200
  -- Check fails
  return { success: false }
ROLLBACK; (automatic - nothing changed)

Job succeeds (doesn't throw)
→ BullMQ marks job complete
→ Will try again next month

```

**Why not throw?**

- This isn't a system error
- User just doesn't have money this month
- No point retrying in 1 minute (won't have money then either)
- Better to wait for next month's scheduled run

---

**Scenario 2: Database error (IS an error):**

```tsx
catch (error) {
    console.error(`Auto-save failed:`, error.message);
    throw error; // ← Propagate error
}

```

**What happens:**

```
BEGIN;
  UPDATE accounts ...
  [Database connection timeout]
  throw error
ROLLBACK; (automatic)

Job fails (throws error)
→ BullMQ will retry (1 min, 2 min, 4 min)
→ Might succeed on retry if connection restored

```

---

### Visual Flow Diagram

```
┌──────────────────────────────────────────────────────────────┐
│ Feb 1, 10:00 AM - BullMQ Triggers Auto-Save                 │
└──────────────────────────────────────────────────────────────┘
BullMQ scheduler creates job
    ↓
AutoSaveWorker picks up job
    data: { goalId: 123, userId: 456, amount: 20000 }
    ↓
executeAutoSave(123, 456, 20000)
    ↓
┌─────────────────────────────────────────────────────────────┐
│ dataSource.transaction(async (manager) => {                │
│                                                             │
│   TypeORM: BEGIN TRANSACTION                               │
│                                                             │
│   ┌───────────────────────────────────────────────┐       │
│   │ Fetch goal from database                      │       │
│   │ SELECT * FROM savings_goals WHERE id = 123    │       │
│   │ Result: {                                      │       │
│   │   id: 123,                                     │       │
│   │   userId: 456,                                 │       │
│   │   currentAmountCents: 50000,  (€500)          │       │
│   │   targetAmountCents: 1000000, (€10,000)       │       │
│   │   status: 'active'                             │       │
│   │ }                                              │       │
│   └───────────────────────────────────────────────┘       │
│                                                             │
│   ┌───────────────────────────────────────────────┐       │
│   │ Check: status === 'active' ?                   │       │
│   │ Result: ✅ Yes, continue                       │       │
│   └───────────────────────────────────────────────┘       │
│                                                             │
│   ┌───────────────────────────────────────────────┐       │
│   │ Fetch account from database                    │       │
│   │ SELECT * FROM accounts WHERE user_id = 456     │       │
│   │ Result: {                                      │       │
│   │   id: 789,                                     │       │
│   │   userId: 456,                                 │       │
│   │   balanceCents: 100000  (€1000)                │       │
│   │ }                                              │       │
│   └───────────────────────────────────────────────┘       │
│                                                             │
│   ┌───────────────────────────────────────────────┐       │
│   │ Check: balance (100000) >= amount (20000) ?    │       │
│   │ Result: ✅ Yes (€1000 >= €200)                 │       │
│   └───────────────────────────────────────────────┘       │
│                                                             │
│   ┌───────────────────────────────────────────────┐       │
│   │ Deduct from account                            │       │
│   │ account.balanceCents = 100000 - 20000 = 80000 │       │
│   │ manager.save(Account, account)                 │       │
│   │ UPDATE accounts                                │       │
│   │ SET balance_cents = 80000                      │       │
│   │ WHERE id = 789                                 │       │
│   └───────────────────────────────────────────────┘       │
│                                                             │
│   ┌───────────────────────────────────────────────┐       │
│   │ Add to goal                                    │       │
│   │ goal.currentAmountCents = 50000 + 20000 = 70000│      │
│   │ goal.lastAutoSaveAt = NOW()                    │       │
│   │ manager.save(SavingsGoal, goal)                │       │
│   │ UPDATE savings_goals                           │       │
│   │ SET current_amount_cents = 70000,              │       │
│   │     last_auto_save_at = NOW()                  │       │
│   │ WHERE id = 123                                 │       │
│   └───────────────────────────────────────────────┘       │
│                                                             │
│   ┌───────────────────────────────────────────────┐       │
│   │ Create contribution record                     │       │
│   │ contribution = manager.create(GoalContribution)│       │
│   │ manager.save(GoalContribution, contribution)   │       │
│   │ INSERT INTO goal_contributions                 │       │
│   │ (goal_id, user_id, amount_cents, type)         │       │
│   │ VALUES (123, 456, 20000, 'auto')               │       │
│   └───────────────────────────────────────────────┘       │
│                                                             │
│   return { success: true, newBalance: 800, ... };         │
│                                                             │
│   TypeORM: COMMIT TRANSACTION ✅                           │
│                                                             │
│ });                                                        │
└─────────────────────────────────────────────────────────────┘
    ↓
Transaction successful!
    ↓
Publish RabbitMQ event:
    rabbitmq.emit('goal.auto_save.completed', {
        goalId: 123,
        userId: 456,
        amount: 200
    })
    ↓
Job marked complete in BullMQ
    ↓
Next month (Mar 1), BullMQ triggers again automatically

```

---

### Insufficient Funds Scenario

```
┌──────────────────────────────────────────────────────────────┐
│ Mar 1, 10:00 AM - BullMQ Triggers Auto-Save Again           │
└──────────────────────────────────────────────────────────────┘
AutoSaveWorker picks up job
    data: { goalId: 123, userId: 456, amount: 20000 }
    ↓
executeAutoSave(123, 456, 20000)
    ↓
┌─────────────────────────────────────────────────────────────┐
│ dataSource.transaction(async (manager) => {                │
│                                                             │
│   TypeORM: BEGIN TRANSACTION                               │
│                                                             │
│   Fetch goal: ✅ (status: 'active')                        │
│   Fetch account: ✅ (balanceCents: 5000 = €50)             │
│                                                             │
│   ┌───────────────────────────────────────────────┐       │
│   │ Check: balance (5000) >= amount (20000) ?      │       │
│   │ Result: ❌ No (€50 < €200)                      │       │
│   │                                                 │       │
│   │ return {                                        │       │
│   │   success: false,                               │       │
│   │   reason: 'insufficient_funds',                 │       │
│   │   balance: 50,                                  │       │
│   │   required: 200                                 │       │
│   │ };                                              │       │
│   └───────────────────────────────────────────────┘       │
│                                                             │
│   TypeORM: ROLLBACK TRANSACTION                            │
│   (Nothing changed in database) ✅                         │
│                                                             │
│ });                                                        │
└─────────────────────────────────────────────────────────────┘
    ↓
Worker logs: "Auto-save skipped: insufficient_funds"
    ↓
Job succeeds (no error thrown)
    ↓
BullMQ marks job complete
    ↓
Next month (Apr 1), BullMQ tries again
    (User might have money by then)

```

---

## **Result:**

- **Money safety:** Can't deduct from account without updating goal
- **Smart handling:**
    - Insufficient funds → Skip month, try next month
    - Database error → Retry in 1 minute
- **Event publishing:** Only after successful COMMIT
- **Learning:** "Different failures need different strategies"
- **Time:** 2 weeks (as part of larger recurring investments feature)

---

## Comparison: Your Three Transaction Use Cases

| Use Case | Operations in Transaction | Why Transaction Needed | Rollback Scenarios |
| --- | --- | --- | --- |
| **Goal Contribution** | 1. Deduct from account<br>2. Update goal<br>3. Insert contribution | Money transfer | - Insufficient funds<br>- Database error |
| **Bank Sync** | Insert 50 transactions<br>Update last_synced_at | Atomic batch save | - Duplicate key<br>- Connection timeout |
| **Auto-Save** | 1. Deduct from account<br>2. Update goal<br>3. Insert contribution | Automated money transfer | - Insufficient funds (no retry)<br>- Database error (retry) |

---

## TypeORM Transaction Patterns Summary

### Pattern 1: Basic Transaction

```tsx
async someOperation() {
    return await this.dataSource.transaction(async (manager) => {
        await manager.save(Entity1, data1);
        await manager.save(Entity2, data2);
        // Commits automatically if no error
    });
    // Rolls back automatically if error thrown
}

```

---

### Pattern 2: Transaction with Conditional Return

```tsx
async conditionalOperation() {
    return await this.dataSource.transaction(async (manager) => {
        const entity = await manager.findOne(Entity, { where: { id } });

        if (!entity) {
            // Return early - transaction rolls back
            return { success: false, reason: 'not_found' };
        }

        await manager.save(Entity, entity);
        return { success: true };
    });
}

```

---

### Pattern 3: Transaction with External API (After Commit)

```tsx
async operationWithAPI() {
    // ✅ CORRECT: External call AFTER transaction

    // Step 1: Database transaction (fast)
    const result = await this.dataSource.transaction(async (manager) => {
        await manager.save(Entity, data);
        return data;
    });

    // Step 2: External API call (after commit)
    await this.externalAPI.call(result);
    // If this fails, transaction is already committed
    // Handle separately (retry, log, alert)
}

```

**Why not inside transaction?**

```tsx
// ❌ WRONG: External call inside transaction
await this.dataSource.transaction(async (manager) => {
    await manager.save(Entity, data);
    await this.externalAPI.call();  // Could take 5 seconds!
    // Problem: Transaction held open, database locks held
});

```

**Problems:**

- External APIs are slow (100ms - 5s)
- Transaction holds database locks
- Other queries blocked
- Connection pool exhausted

**Rule:** Keep transactions SHORT. Only database operations inside!

---

### Pattern 4: Bulk Operations

```tsx
async bulkSave(items: any[]) {
    return await this.dataSource.transaction(async (manager) => {
        // ✅ GOOD: Bulk insert in ONE query
        await manager.save(Entity, items);
        // TypeORM generates:
        // INSERT INTO table VALUES (...), (...), (...)

        // ❌ BAD: Loop with individual saves
        // for (const item of items) {
        //     await manager.save(Entity, item);
        // }
    });
}

```

**Performance:**

- Bulk: 1 database round trip
- Loop: N database round trips (N = number of items)

---

## TypeORM vs Spring Boot Comparison

| Aspect | Spring Boot | NestJS + TypeORM |
| --- | --- | --- |
| **Transaction Start** | `@Transactional` annotation | `dataSource.transaction()` method |
| **Automatic Management** | ✅ Yes | ✅ Yes |
| **Explicit BEGIN** | ❌ Not needed | ❌ Not needed |
| **Explicit COMMIT** | ❌ Not needed | ❌ Not needed |
| **Explicit ROLLBACK** | ❌ Not needed | ❌ Not needed |
| **Rollback on Exception** | ✅ Automatic | ✅ Automatic |
| **Propagation** | Configurable | Less flexible |
| **Isolation Levels** | Configurable | Configurable |
| **Learning Curve** | Low (annotation-based) | Medium (callback-based) |

---

## Advanced: Transaction Isolation Levels

**TypeORM supports different isolation levels:**

```tsx
// Default: READ COMMITTED
await this.dataSource.transaction(async (manager) => {
    // Operations here
});

// Serializable (highest isolation)
await this.dataSource.transaction(
    'SERIALIZABLE',  // ← Isolation level
    async (manager) => {
        // Critical financial operations
        await manager.save(Account, account);
    }
);

```

**Isolation levels in PostgreSQL:**

| Level | Dirty Reads | Non-Repeatable Reads | Phantom Reads | Use Case |
| --- | --- | --- | --- | --- |
| READ UNCOMMITTED | ❌ Not supported by PostgreSQL | - | - | - |
| **READ COMMITTED** (default) | ✅ Prevented | ⚠️ Possible | ⚠️ Possible | Most operations |
| REPEATABLE READ | ✅ Prevented | ✅ Prevented | ⚠️ Possible | Reports, analytics |
| SERIALIZABLE | ✅ Prevented | ✅ Prevented | ✅ Prevented | Critical financial transfers |

**For FinVerse:**

- **READ COMMITTED (default):** Sufficient for 99% of operations
- **SERIALIZABLE:** Only for critical money transfers between accounts

---

# 🧠Interview Questions About Transactions

### Q1: "How do you handle database transactions in NestJS vs Spring Boot?"

**Your Answer:**

"In Spring Boot, I use the `@Transactional` annotation and Spring handles everything automatically. In NestJS with TypeORM, the pattern is similar but uses a callback-based approach.

For example, in our savings goals feature:

**Spring Boot:**

```java
@Transactional
public void contribute(Long goalId, BigDecimal amount) {
    accountRepository.save(account);
    goalRepository.save(goal);
    contributionRepository.save(contribution);
}

```

**NestJS with TypeORM:**

```tsx
async contribute(goalId: number, amount: number) {
    return await this.dataSource.transaction(async (manager) => {
        await manager.save(Account, account);
        await manager.save(SavingsGoal, goal);
        await manager.save(GoalContribution, contribution);
    });
}

```

Both automatically handle BEGIN, COMMIT, and ROLLBACK. The key difference is Spring uses annotations (declarative), while TypeORM uses callbacks (programmatic). Both achieve the same result - atomic operations with automatic rollback on errors."

---

### Q2: "What happens if your server crashes mid-transaction?"

**Your Answer:**

"PostgreSQL automatically rolls back uncommitted transactions. For example, in our auto-save feature:

```tsx
await this.dataSource.transaction(async (manager) => {
    await manager.save(Account, account);    // ✅ Executed
    await manager.save(SavingsGoal, goal);   // ✅ Executed
    // [SERVER CRASH] 💥
    // return statement never reached
});

```

When the server crashes:

1. The transaction never commits (COMMIT never executed)
2. PostgreSQL detects the connection dropped
3. Automatically rolls back the uncommitted transaction
4. When server restarts, database is in pre-transaction state

This is guaranteed by PostgreSQL's WAL (Write-Ahead Logging). Every operation is first written to the WAL, and only committed transactions are considered permanent. Uncommitted transactions are simply ignored during recovery."

---

### Q3: "Why shouldn't you put RabbitMQ publishing inside a transaction?"

**Your Answer:**

"I learned this the hard way! Initially, I tried:

```tsx
// ❌ WRONG
await this.dataSource.transaction(async (manager) => {
    await manager.save(Account, account);
    await this.rabbitmqClient.emit('event');  // External system!
    await manager.save(Goal, goal);
});

```

Problems:

1. **RabbitMQ call could take 100ms+** → Transaction held open
2. **Database locks held during external API call** → Other queries blocked
3. **If RabbitMQ fails, should we rollback database?** → Usually no!
4. **Connection pool exhaustion** → Long transactions = fewer available connections

The correct approach:

```tsx
// ✅ CORRECT
const result = await this.dataSource.transaction(async (manager) => {
    await manager.save(Account, account);
    await manager.save(Goal, goal);
    return { goalId, userId, amount };
});

// External calls AFTER commit
await this.rabbitmqClient.emit('event', result);

```

Rule: Transactions should contain ONLY database operations. Keep them short (< 100ms). External API calls happen after the transaction commits."

---

### Q4: "How do you handle insufficient funds - rollback or commit?"

**Your Answer:**

"It depends on whether it's a business rule failure or a system error.

**Insufficient funds (business rule):**

```tsx
await this.dataSource.transaction(async (manager) => {
    const account = await manager.findOne(Account, { where: { userId } });

    if (account.balanceCents < amount) {
        // Don't throw - just return failure
        return { success: false, reason: 'insufficient_funds' };
    }

    // Transaction rolls back, but job doesn't fail
});

```

**Database error (system error):**

```tsx
await this.dataSource.transaction(async (manager) => {
    await manager.save(Account, account);
    // Database connection timeout
    throw error; // Propagate error
});
// Transaction rolls back AND job fails → BullMQ retries

```

The distinction is important:

- **Business failures:** User can't control (no money) → Skip, try later, don't retry
- **System failures:** Temporary issues → Retry might succeed

In our auto-save feature, insufficient funds meant 'wait until next month', not 'retry in 1 minute'."

---

## Summary

**Where you used TypeORM transactions:**

1. ✅ **Savings Goals - contribute():**
    - Deduct from account + update goal + insert record
    - Pattern: Basic transaction with validation
2. ✅ **Bank Sync - saveTransactions():**
    - Batch insert 50 transactions
    - Pattern: Bulk operations with idempotency
3. ✅ **Auto-Save - executeAutoSave():**
    - Deduct from account + update goal + insert record
    - Pattern: Transaction with conditional return

**Key learnings:**

- ✅ TypeORM transactions similar to Spring's `@Transactional`
- ✅ Automatic BEGIN/COMMIT/ROLLBACK management
- ✅ Use `manager` inside transactions, not repositories
- ✅ Keep transactions short (only database operations)
- ✅ External API calls AFTER transaction commits
- ✅ Different errors need different handling strategies
- ✅ Bulk operations for performance

**Why critical in fintech:**

- Money can't disappear (atomic transfers)
- Data must be consistent (account ↔ goal always in sync)
- Partial updates are dangerous
- ACID properties ensure reliability

**Production readiness:**

With TypeORM, your code is production-ready and follows industry best practices. The pattern is clean, maintainable, and similar to what you'd write in Spring Boot!