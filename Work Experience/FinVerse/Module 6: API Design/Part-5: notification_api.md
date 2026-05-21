Base URL: `https://api.finverse.com/api/v1/notifications` (Internal only)

**Purpose**: Handle all user notifications across multiple channels (email, push, SMS). Consumes events from RabbitMQ, renders templates from MongoDB, queues delivery via BullMQ.

**Technology Choice**: NestJS (Node.js) is used because:

- I/O-heavy workload (sending notifications, not computation)
- Easy integration with third-party APIs (SendGrid, Twilio, AWS SNS)
- Excellent async/await support for concurrent operations
- Built-in decorators for RabbitMQ consumers and BullMQ processors
- Same stack as Core API (code reuse, easier hiring)

**Architecture Pattern:**

```
RabbitMQ Event → Consumer → Render Template → Add to BullMQ → Worker → External API

```

---

## Service Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    Notification Service (NestJS)                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              RabbitMQ Consumers (Listeners)              │   │
│  │                                                          │   │
│  │  • EmailNotificationConsumer                             │   │
│  │  • PushNotificationConsumer                              │   │
│  │  • BudgetAlertConsumer                                   │   │
│  │  • OrderCompletionConsumer                               │   │
│  │  • WelcomeEmailConsumer                                  │   │
│  │                                                          │   │
│  │  Listens to RabbitMQ queues, processes events            │   │
│  └─────────────────┬────────────────────────────────────────┘   │
│                    │                                            │
│                    ▼                                            │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │            Notification Orchestrator                     │   │
│  │                                                          │   │
│  │  • Check user preferences (opt-in/out)                   │   │
│  │  • Fetch templates from MongoDB                          │   │
│  │  • Render templates with user data                       │   │
│  │  • Add jobs to BullMQ queues                             │   │
│  └─────────────────┬────────────────────────────────────────┘   │
│                    │                                            │
│                    ▼                                            │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │               BullMQ Queues (Redis)                      │   │
│  │                                                          │   │
│  │  • email-queue (pending emails)                          │   │
│  │  • push-queue (pending push notifications)               │   │
│  │  • sms-queue (pending SMS)                               │   │
│  └─────────────────┬────────────────────────────────────────┘   │
│                    │                                            │
│                    ▼                                            │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              BullMQ Workers (Processors)                 │   │
│  │                                                          │   │
│  │  • EmailWorker (sends via SendGrid)                      │   │
│  │  • PushWorker (sends via AWS SNS)                        │   │
│  │  • SmsWorker (sends via Twilio)                          │   │
│  │                                                          │   │
│  │  Polls queues, executes delivery, handles retries        │   │
│  └─────────────────┬────────────────────────────────────────┘   │
│                    │                                            │
│                    ▼                                            │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │           External APIs                                  │   │
│  │                                                          │   │
│  │  • Twilio SendGrid (email)                               │   │
│  │  • AWS SNS (push notifications)                          │   │
│  │  • Twilio SMS (SMS messages)                             │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

```

---

## Module 1: RabbitMQ Event Consumers

### **Consumer 1: Order Completion Notifications**

**Listens to:** `notification_orders_queue` (from `orders_exchange`)

**Code Structure:**

```tsx
@Controller()
export class OrderNotificationConsumer {
  constructor(
    private readonly notificationService: NotificationService,
    private readonly templateService: TemplateService,
    private readonly userPreferencesService: UserPreferencesService,
  ) {}

  @RabbitSubscribe({
    exchange: 'orders_exchange',
    routingKey: 'order.completed',
    queue: 'notification_orders_queue',
  })
  async handleOrderCompleted(message: OrderCompletedEvent) {
    const { orderId, userId, portfolioId, totalAmount } = message;

    try {
      // 1. Fetch user preferences
      const preferences = await this.userPreferencesService.getPreferences(userId);

      if (!preferences.investmentUpdates) {
        // User opted out of investment notifications
        return;
      }

      // 2. Fetch user data from PostgreSQL
      const user = await this.db.query(
        'SELECT email, first_name FROM users WHERE id = $1',
        [userId]
      );

      const portfolio = await this.db.query(
        'SELECT name, total_value_cents FROM portfolios WHERE id = $1',
        [portfolioId]
      );

      // 3. Prepare template data
      const templateData = {
        userName: user.first_name,
        orderId: orderId,
        portfolioName: portfolio.name,
        investmentAmount: (totalAmount / 100).toFixed(2),
        portfolioValue: (portfolio.total_value_cents / 100).toFixed(2),
        dashboardUrl: `https://app.finverse.com/portfolios/${portfolioId}`,
      };

      // 4. Send via enabled channels
      if (preferences.emailEnabled) {
        await this.notificationService.sendEmail({
          userId,
          templateId: 'order_completed',
          data: templateData,
          priority: 2, // Medium priority
        });
      }

      if (preferences.pushEnabled) {
        await this.notificationService.sendPush({
          userId,
          templateId: 'order_completed',
          data: templateData,
          priority: 2,
        });
      }

      // 5. Log notification event
      await this.db.query(
        `INSERT INTO notification_queue (user_id, notification_type, status, created_at)
         VALUES ($1, $2, $3, NOW())`,
        [userId, 'order_completed', 'queued']
      );

    } catch (error) {
      // Log error but don't throw (prevents RabbitMQ message rejection)
      this.logger.error('Failed to process order completion notification', {
        orderId,
        userId,
        error: error.message,
      });

      // Publish to dead letter queue if critical
      if (error.code === 'USER_NOT_FOUND') {
        throw error; // Reject message, goes to DLQ
      }
    }
  }
}

```

**Error Handling:**

- Non-critical errors (template rendering fails): Log and skip
- Critical errors (user not found): Reject message → Dead Letter Queue
- Transient errors (SendGrid timeout): Don't reject, BullMQ will retry

---

### **Consumer 2: Budget Alert Notifications**

**Listens to:** `budget_alerts_queue` (from `notifications_exchange`)

**Code Structure:**

```tsx
@Controller()
export class BudgetAlertConsumer {
  @RabbitSubscribe({
    exchange: 'notifications_exchange',
    routingKey: 'notification.budget.exceeded',
    queue: 'budget_alerts_queue',
  })
  async handleBudgetExceeded(message: BudgetExceededEvent) {
    const { userId, category, spent, limit, percentage } = message;

    // Check quiet hours (don't send alerts at 2 AM)
    const userPrefs = await this.userPreferencesService.getPreferences(userId);

    if (this.isQuietHours(userPrefs)) {
      // Delay notification until quiet hours end
      const delayMs = this.calculateDelayUntilQuietHoursEnd(userPrefs);

      await this.notificationService.sendEmailDelayed({
        userId,
        templateId: 'budget_exceeded',
        data: { category, spent, limit, percentage },
        delayMs,
      });

      return;
    }

    // Fetch spending insights
    const insights = await this.getSpendingInsights(userId, category);

    const templateData = {
      userName: await this.getUserName(userId),
      category,
      spent: (spent / 100).toFixed(2),
      limit: (limit / 100).toFixed(2),
      percentage: percentage.toFixed(1),
      overspent: ((spent - limit) / 100).toFixed(2),
      topMerchants: insights.topMerchants,
      comparisonToPrevMonth: insights.comparisonToPrevMonth,
      budgetUrl: `https://app.finverse.com/budgets`,
    };

    // Budget alerts are high priority (user needs to know)
    await this.notificationService.sendEmail({
      userId,
      templateId: 'budget_exceeded',
      data: templateData,
      priority: 3, // High priority
    });

    await this.notificationService.sendPush({
      userId,
      templateId: 'budget_exceeded',
      data: templateData,
      priority: 3,
    });

    // Mark budget alert as sent (prevent duplicate alerts)
    await this.db.query(
      'UPDATE budgets SET alert_sent = true, alert_sent_at = NOW() WHERE user_id = $1 AND category = $2',
      [userId, category]
    );
  }

  private isQuietHours(preferences: UserPreferences): boolean {
    if (!preferences.quietHoursEnabled) return false;

    const now = new Date();
    const currentHour = now.getHours();
    const startHour = parseInt(preferences.quietHoursStart.split(':')[0]);
    const endHour = parseInt(preferences.quietHoursEnd.split(':')[0]);

    // Example: 22:00 - 08:00 quiet hours
    if (startHour > endHour) {
      // Crosses midnight
      return currentHour >= startHour || currentHour < endHour;
    } else {
      return currentHour >= startHour && currentHour < endHour;
    }
  }

  private calculateDelayUntilQuietHoursEnd(preferences: UserPreferences): number {
    const now = new Date();
    const endHour = parseInt(preferences.quietHoursEnd.split(':')[0]);
    const endMinute = parseInt(preferences.quietHoursEnd.split(':')[1]);

    const endTime = new Date(now);
    endTime.setHours(endHour, endMinute, 0, 0);

    if (endTime < now) {
      // Next day
      endTime.setDate(endTime.getDate() + 1);
    }

    return endTime.getTime() - now.getTime();
  }
}

```

**Why quiet hours matter:**

- User experience: Don't wake user at 3 AM with budget alert
- Regulatory: Some regions prohibit marketing notifications at night
- Implementation: Delay notification using BullMQ delayed jobs

---

### **Consumer 3: Welcome Email (Fanout)**

**Listens to:** `welcome_email_queue` (from `users_exchange` fanout)

```tsx
@Controller()
export class WelcomeEmailConsumer {
  @RabbitSubscribe({
    exchange: 'users_exchange',
    queue: 'welcome_email_queue',
  })
  async handleUserRegistered(message: UserRegisteredEvent) {
    const { userId, email, firstName } = message;

    // Wait 5 minutes before sending welcome email
    // (Give user time to explore app first, better open rate)
    const delayMs = 5 * 60 * 1000;

    await this.notificationService.sendEmailDelayed({
      userId,
      templateId: 'welcome_email',
      data: {
        userName: firstName,
        email,
        verifyEmailUrl: `https://app.finverse.com/verify-email?token=${this.generateToken(userId)}`,
        getStartedUrl: 'https://app.finverse.com/onboarding',
        supportEmail: 'support@finverse.com',
      },
      delayMs,
      priority: 2,
    });

    // Also send push notification to mobile device (if installed)
    // Check if user has device tokens
    const deviceTokens = await this.getDeviceTokens(userId);

    if (deviceTokens.length > 0) {
      await this.notificationService.sendPush({
        userId,
        templateId: 'welcome_push',
        data: {
          userName: firstName,
          title: 'Welcome to FinVerse! 🎉',
          body: 'Start your financial journey today',
        },
        priority: 1,
      });
    }
  }
}

```

---

## Module 2: Notification Orchestrator

**Core service that handles template rendering and job queuing**

```tsx
@Injectable()
export class NotificationService {
  constructor(
    private readonly templateService: TemplateService,
    @InjectQueue('email-queue') private emailQueue: Queue,
    @InjectQueue('push-queue') private pushQueue: Queue,
    @InjectQueue('sms-queue') private smsQueue: Queue,
    private readonly userPreferencesService: UserPreferencesService,
    private readonly mongoClient: MongoClient,
    private readonly postgres: PostgresService,
  ) {}

  async sendEmail(request: SendEmailRequest): Promise<void> {
    const { userId, templateId, data, priority = 1 } = request;

    try {
      // 1. Check user preferences
      const preferences = await this.userPreferencesService.getPreferences(userId);

      if (!preferences.emailEnabled) {
        this.logger.info('Email notifications disabled for user', { userId });
        return;
      }

      // 2. Fetch user email
      const user = await this.postgres.query(
        'SELECT email, language FROM users WHERE id = $1',
        [userId]
      );

      if (!user || !user.email) {
        throw new Error(`User ${userId} not found or has no email`);
      }

      // 3. Fetch template from MongoDB
      const template = await this.templateService.getTemplate(templateId, user.language);

      if (!template) {
        throw new Error(`Template ${templateId} not found`);
      }

      // 4. Render template with data
      const renderedSubject = this.renderTemplate(template.channels.email.subject, data);
      const renderedHtmlBody = this.renderTemplate(template.channels.email.htmlBody, data);
      const renderedTextBody = this.renderTemplate(template.channels.email.textBody, data);

      // 5. Add job to BullMQ email queue
      await this.emailQueue.add(
        'send-email',
        {
          to: user.email,
          subject: renderedSubject,
          htmlBody: renderedHtmlBody,
          textBody: renderedTextBody,
          userId,
          templateId,
        },
        {
          priority,
          attempts: 3,
          backoff: {
            type: 'exponential',
            delay: 10000, // Start with 10 seconds
          },
          removeOnComplete: true,
          removeOnFail: false, // Keep failed jobs for debugging
        }
      );

      // 6. Log to PostgreSQL
      await this.postgres.query(
        `INSERT INTO notification_queue (user_id, notification_type, channel, status, created_at)
         VALUES ($1, $2, $3, $4, NOW())`,
        [userId, templateId, 'email', 'queued']
      );

      this.logger.info('Email queued successfully', { userId, templateId });

    } catch (error) {
      this.logger.error('Failed to queue email', {
        userId,
        templateId,
        error: error.message,
      });
      throw error;
    }
  }

  async sendEmailDelayed(request: SendEmailRequest & { delayMs: number }): Promise<void> {
    const { delayMs, ...emailRequest } = request;

    // Same logic as sendEmail, but with delay option
    await this.emailQueue.add(
      'send-email',
      {
        // ... same data
      },
      {
        delay: delayMs, // Delay before processing
        priority: request.priority,
        attempts: 3,
        backoff: { type: 'exponential', delay: 10000 },
      }
    );
  }

  async sendPush(request: SendPushRequest): Promise<void> {
    const { userId, templateId, data, priority = 1 } = request;

    // 1. Check preferences
    const preferences = await this.userPreferencesService.getPreferences(userId);
    if (!preferences.pushEnabled) return;

    // 2. Get device tokens
    const deviceTokens = await this.getDeviceTokens(userId);

    if (deviceTokens.length === 0) {
      this.logger.info('No device tokens for user', { userId });
      return;
    }

    // 3. Fetch template
    const template = await this.templateService.getTemplate(templateId);

    // 4. Render title and body
    const title = this.renderTemplate(template.channels.push.title, data);
    const body = this.renderTemplate(template.channels.push.body, data);

    // 5. Add to push queue
    await this.pushQueue.add(
      'send-push',
      {
        userId,
        title,
        body,
        deviceTokens,
        data: data, // Additional data for deep linking
      },
      {
        priority,
        attempts: 2,
        backoff: { type: 'exponential', delay: 30000 },
      }
    );
  }

  private renderTemplate(template: string, data: Record<string, any>): string {
    // Simple template rendering (replace {{variable}} with actual values)
    let rendered = template;

    for (const [key, value] of Object.entries(data)) {
      const placeholder = `{{${key}}}`;
      rendered = rendered.replace(new RegExp(placeholder, 'g'), String(value));
    }

    return rendered;
  }

  private async getDeviceTokens(userId: number): Promise<string[]> {
    const result = await this.postgres.query(
      `SELECT device_token FROM user_devices
       WHERE user_id = $1 AND status = 'active'`,
      [userId]
    );

    return result.rows.map(row => row.device_token);
  }
}

```

**Key Design Decisions:**

1. **Template Rendering in Orchestrator, Not Worker:**
    - Faster to detect template errors before queuing
    - Can reject invalid data early
    - Worker only sends, doesn't need template logic
2. **Priority Levels:**
    - 1 (Low): Marketing emails, weekly digests
    - 2 (Medium): Order confirmations, goal milestones
    - 3 (High): Budget alerts, security alerts, failed payments
    - Workers process high-priority jobs first
3. **Retry Strategy:**
    - Email: 3 attempts (SendGrid might be temporarily down)
    - Push: 2 attempts (device token invalid = permanent failure)
    - SMS: 3 attempts (expensive, but critical alerts)

---

## Module 3: BullMQ Workers

### **Worker 1: Email Worker**

```tsx
@Processor('email-queue')
export class EmailWorker {
  private readonly sendGridClient: SendGridClient;

  constructor(
    @InjectQueue('email-queue') private emailQueue: Queue,
    private readonly postgres: PostgresService,
  ) {
    this.sendGridClient = new SendGridClient(process.env.SENDGRID_API_KEY);
  }

  @Process('send-email')
  async handleSendEmail(job: Job<SendEmailJobData>): Promise<void> {
    const { to, subject, htmlBody, textBody, userId, templateId } = job.data;

    this.logger.info('Processing email job', {
      jobId: job.id,
      to,
      userId,
      attempt: job.attemptsMade + 1,
    });

    try {
      // Send via SendGrid
      const response = await this.sendGridClient.send({
        to,
        from: {
          email: 'noreply@finverse.com',
          name: 'FinVerse',
        },
        subject,
        text: textBody,
        html: htmlBody,
        trackingSettings: {
          clickTracking: { enable: true },
          openTracking: { enable: true },
        },
      });

      this.logger.info('Email sent successfully', {
        jobId: job.id,
        sendGridMessageId: response[0].headers['x-message-id'],
      });

      // Update notification_queue status
      await this.postgres.query(
        `UPDATE notification_queue
         SET status = 'sent',
             sent_at = NOW(),
             external_id = $1
         WHERE user_id = $2 AND notification_type = $3 AND status = 'queued'`,
        [response[0].headers['x-message-id'], userId, templateId]
      );

      return; // Success - job completed

    } catch (error) {
      this.logger.error('Failed to send email', {
        jobId: job.id,
        error: error.message,
        attempt: job.attemptsMade + 1,
      });

      // Determine if error is retryable
      if (this.isRetryableError(error)) {
        throw error; // BullMQ will retry with backoff
      } else {
        // Permanent failure (e.g., invalid email address)
        await this.postgres.query(
          `UPDATE notification_queue
           SET status = 'failed',
               error_message = $1
           WHERE user_id = $2 AND notification_type = $3`,
          [error.message, userId, templateId]
        );

        // Don't throw - mark job as completed (failed permanently)
        return;
      }
    }
  }

  @OnQueueFailed()
  async handleFailedJob(job: Job, error: Error): Promise<void> {
    this.logger.error('Email job failed after all retries', {
      jobId: job.id,
      userId: job.data.userId,
      error: error.message,
    });

    // Alert operations team for manual review
    await this.alertOps('Email delivery failed', {
      jobId: job.id,
      userId: job.data.userId,
      attempts: job.attemptsMade,
    });
  }

  private isRetryableError(error: any): boolean {
    // SendGrid error codes
    const retryableCodes = [
      429, // Rate limit
      500, // Internal server error
      503, // Service unavailable
    ];

    return retryableCodes.includes(error.code);
  }
}

```

**SendGrid Integration:**

- API key stored in environment variable
- Track opens and clicks (analytics)
- Message ID stored for webhook correlation

---

### **Worker 2: Push Notification Worker**

```tsx
@Processor('push-queue')
export class PushWorker {
  private readonly snsClient: SNSClient;

  constructor(
    @InjectQueue('push-queue') private pushQueue: Queue,
    private readonly postgres: PostgresService,
  ) {
    this.snsClient = new SNSClient({
      region: 'eu-west-1',
      credentials: {
        accessKeyId: process.env.AWS_ACCESS_KEY_ID,
        secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY,
      },
    });
  }

  @Process('send-push')
  async handleSendPush(job: Job<SendPushJobData>): Promise<void> {
    const { userId, title, body, deviceTokens, data } = job.data;

    this.logger.info('Processing push notification', {
      jobId: job.id,
      userId,
      deviceCount: deviceTokens.length,
    });

    const results = [];

    // Send to each device token
    for (const token of deviceTokens) {
      try {
        const message = {
          default: body,
          APNS: JSON.stringify({
            aps: {
              alert: {
                title,
                body,
              },
              badge: 1,
              sound: 'default',
            },
            data, // Custom data for deep linking
          }),
          GCM: JSON.stringify({
            notification: {
              title,
              body,
            },
            data,
          }),
        };

        const command = new PublishCommand({
          TargetArn: token,
          Message: JSON.stringify(message),
          MessageStructure: 'json',
        });

        const response = await this.snsClient.send(command);

        results.push({
          token,
          success: true,
          messageId: response.MessageId,
        });

      } catch (error) {
        this.logger.warn('Failed to send push to device', {
          token: token.substring(0, 20) + '...',
          error: error.message,
        });

        // Check if token is invalid (permanent failure)
        if (error.code === 'EndpointDisabled' || error.code === 'InvalidParameter') {
          // Mark device as inactive
          await this.postgres.query(
            `UPDATE user_devices
             SET status = 'inactive', updated_at = NOW()
             WHERE device_token = $1`,
            [token]
          );
        }

        results.push({
          token,
          success: false,
          error: error.message,
        });
      }
    }

    // Update notification status
    const successCount = results.filter(r => r.success).length;

    await this.postgres.query(
      `UPDATE notification_queue
       SET status = $1,
           sent_at = NOW(),
           delivered_at = NOW()
       WHERE user_id = $2 AND notification_type = $3`,
      [successCount > 0 ? 'sent' : 'failed', userId, job.data.templateId]
    );

    this.logger.info('Push notification completed', {
      jobId: job.id,
      successCount,
      failureCount: results.length - successCount,
    });
  }
}

```

**AWS SNS Features:**

- Supports both APNS (iOS) and FCM (Android)
- Automatic retry for transient failures
- Delivery receipts (track delivery status)

---

### **Worker 3: SMS Worker**

```tsx
@Processor('sms-queue')
export class SmsWorker {
  private readonly twilioClient: Twilio;

  constructor(
    @InjectQueue('sms-queue') private smsQueue: Queue,
    private readonly postgres: PostgresService,
  ) {
    this.twilioClient = new Twilio(
      process.env.TWILIO_ACCOUNT_SID,
      process.env.TWILIO_AUTH_TOKEN,
    );
  }

  @Process('send-sms')
  async handleSendSms(job: Job<SendSmsJobData>): Promise<void> {
    const { userId, phone, message } = job.data;

    try {
      const response = await this.twilioClient.messages.create({
        to: phone,
        from: process.env.TWILIO_PHONE_NUMBER, // +4930123456789
        body: message,
      });

      this.logger.info('SMS sent successfully', {
        jobId: job.id,
        userId,
        twilioSid: response.sid,
        status: response.status,
      });

      await this.postgres.query(
        `UPDATE notification_queue
         SET status = 'sent',
             sent_at = NOW(),
             external_id = $1
         WHERE user_id = $2 AND notification_type = $3`,
        [response.sid, userId, job.data.templateId]
      );

    } catch (error) {
      this.logger.error('Failed to send SMS', {
        jobId: job.id,
        error: error.message,
      });

      // SMS is expensive - don't retry too many times
      if (job.attemptsMade >= 2) {
        // Final failure - log and alert
        await this.alertOps('SMS delivery failed', {
          userId,
          phone,
          error: error.message,
        });
      } else {
        throw error; // Retry
      }
    }
  }
}

```

**SMS Considerations:**

- Most expensive channel (~€0.05 per SMS)
- Only use for critical alerts (2FA, payment failures)
- Limited to 160 characters
- Fewer retries than email (cost)

---

## Module 4: Template Service

```tsx
@Injectable()
export class TemplateService {
  constructor(
    private readonly mongoClient: MongoClient,
  ) {}

  async getTemplate(templateId: string, language: string = 'en'): Promise<NotificationTemplate> {
    const db = this.mongoClient.db('finverse');
    const collection = db.collection('notification_templates');

    // Fetch template by ID and language
    const template = await collection.findOne({
      templateId,
      status: 'active',
    });

    if (!template) {
      throw new Error(`Template ${templateId} not found`);
    }

    // Check if template has language variant
    if (template.variants && template.variants[language]) {
      // Return language-specific variant
      return {
        ...template,
        channels: template.variants[language].channels,
      };
    }

    // Default to English
    return template;
  }

  async createTemplate(template: CreateTemplateDto): Promise<NotificationTemplate> {
    const db = this.mongoClient.db('finverse');
    const collection = db.collection('notification_templates');

    const newTemplate = {
      templateId: template.templateId,
      name: template.name,
      description: template.description,
      category: template.category,
      channels: template.channels,
      variables: template.variables,
      status: 'active',
      version: 1,
      createdAt: new Date(),
      updatedAt: new Date(),
    };

    await collection.insertOne(newTemplate);

    return newTemplate;
  }

  async updateTemplate(templateId: string, updates: UpdateTemplateDto): Promise<void> {
    const db = this.mongoClient.db('finverse');
    const collection = db.collection('notification_templates');

    // Version control: create new version instead of updating
    const currentTemplate = await collection.findOne({ templateId });

    await collection.insertOne({
      ...currentTemplate,
      ...updates,
      version: currentTemplate.version + 1,
      updatedAt: new Date(),
      previousVersionId: currentTemplate._id,
    });

    // Mark old version as inactive
    await collection.updateOne(
      { _id: currentTemplate._id },
      { $set: { status: 'archived' } }
    );
  }
}

```

**Template Versioning:**

- Never update templates in-place (might break in-flight notifications)
- Create new version, mark old as archived
- Can rollback to previous version if needed

---

## Module 5:

User Preferences Service

```tsx
@Injectable()
export class UserPreferencesService {
  constructor(
    private readonly postgres: PostgresService,
    @Inject('REDIS') private readonly redis: Redis,
  ) {}

  async getPreferences(userId: number): Promise<UserNotificationPreferences> {
    // Try cache first
    const cacheKey = `user:preferences:${userId}`;
    const cached = await this.redis.get(cacheKey);

    if (cached) {
      return JSON.parse(cached);
    }

    // Fetch from database
    const result = await this.postgres.query(
      `SELECT * FROM notification_preferences WHERE user_id = $1`,
      [userId]
    );

    if (!result.rows[0]) {
      // Create default preferences
      return this.createDefaultPreferences(userId);
    }

    const preferences = result.rows[0];

    // Cache for 1 hour
    await this.redis.setex(cacheKey, 3600, JSON.stringify(preferences));

    return preferences;
  }

  async updatePreferences(
    userId: number,
    updates: UpdatePreferencesDto,
  ): Promise<UserNotificationPreferences> {
    await this.postgres.query(
      `UPDATE notification_preferences
       SET email_enabled = $1,
           push_enabled = $2,
           sms_enabled = $3,
           budget_alerts = $4,
           investment_updates = $5,
           goal_milestones = $6,
           educational_content = $7,
           marketing_emails = $8,
           digest_frequency = $9,
           quiet_hours_enabled = $10,
           quiet_hours_start = $11,
           quiet_hours_end = $12,
           updated_at = NOW()
       WHERE user_id = $13`,
      [
        updates.emailEnabled,
        updates.pushEnabled,
        updates.smsEnabled,
        updates.budgetAlerts,
        updates.investmentUpdates,
        updates.goalMilestones,
        updates.educationalContent,
        updates.marketingEmails,
        updates.digestFrequency,
        updates.quietHoursEnabled,
        updates.quietHoursStart,
        updates.quietHoursEnd,
        userId,
      ]
    );

    // Invalidate cache
    await this.redis.del(`user:preferences:${userId}`);

    return this.getPreferences(userId);
  }

  private async createDefaultPreferences(userId: number): Promise<UserNotificationPreferences> {
    const defaults = {
      emailEnabled: true,
      pushEnabled: true,
      smsEnabled: false,
      budgetAlerts: true,
      investmentUpdates: true,
      goalMilestones: true,
      educationalContent: true,
      marketingEmails: false,
      digestFrequency: 'weekly',
      quietHoursEnabled: false,
      quietHoursStart: '22:00',
      quietHoursEnd: '08:00',
    };

    await this.postgres.query(
      `INSERT INTO notification_preferences (user_id, email_enabled, push_enabled, ...)
       VALUES ($1, $2, $3, ...)`,
      [userId, ...Object.values(defaults)]
    );

    return { userId, ...defaults };
  }
}

```

---

## Performance & Scalability

### **Current Capacity (1 NestJS instance):**

- 5,000 emails/hour
- 10,000 push notifications/hour
- 500 SMS/hour

### **Bottlenecks:**

- External API rate limits (SendGrid: 10,000/day on free tier)
- BullMQ worker concurrency (5 concurrent jobs per worker by default)

### **Scaling Strategy:**

- Horizontal: Add more NestJS instances (stateless, easy)
- Worker Concurrency: Increase to 10-20 jobs per worker
- Queue Sharding: Separate queues for different notification types

### **Monitoring:**

- Alert if queue depth > 1,000 (workers falling behind)
- Alert if delivery failure rate > 5%
- Track delivery times (email should send within 30 seconds of queuing)

---

This Notification Service is designed for **reliability and user experience**, with comprehensive preference management, retry logic, and multi-channel delivery. The separation of concerns (consumer → orchestrator → worker) makes it easy to debug and scale independently.