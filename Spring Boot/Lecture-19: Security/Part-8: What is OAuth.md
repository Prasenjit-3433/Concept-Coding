# Step 1 — The Problem OAuth Solves

---

## What is OAuth 2.0?

**OAuth** stands for **Open Authorization.**
It is an **authorization framework** that enables secure third-party access to a user's protected data.

Two keywords to note right away:
- **Authorization** — not authentication. OAuth is about *what you're allowed to access*, not *who you are*. (The instructor touches on this difference later when comparing OAuth vs OIDC.)
- **Third-party access** — a completely different app/service wants to use your data stored somewhere else.

---

## The Real-World Problem it Solves

Let's say you're already logged into your **Gmail account.** Gmail knows everything about you — your name, email ID, date of birth, profile picture, address. All of that is your **protected data**, sitting safely inside Gmail.

Now you visit some new website — let's call it **Instagram** (or any XYZ website). Instead of filling out a registration form again from scratch, you see a button:

```
[ Sign in with Google ]
```

You click it. Instagram somehow gets your name, email, and profile info from Gmail — and logs you in automatically.

**The question is: how did Instagram access your Gmail data securely, without you ever giving Instagram your Gmail password?**

That's *exactly* the problem OAuth 2.0 solves.

---

## Why Not Just Share Your Password?

Think about what would happen if Instagram simply asked:

> "Hey, give us your Gmail username and password so we can fetch your profile."

That would be a **nightmare:**

- Instagram now knows your Gmail password.
- If Instagram gets hacked, your Gmail is compromised.
- Instagram could access *everything* in your Gmail — not just your profile.
- You can't revoke Instagram's access without changing your Gmail password.

OAuth solves all of this. You never share your Gmail password with Instagram. Gmail itself hands over only the specific data Instagram is allowed to see — and only after *you* explicitly say yes.

---

## The Simple Mental Model

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│   YOU (already logged into Gmail)                           │
│        │                                                    │
│        │  Click "Sign in with Google" on Instagram          │
│        ▼                                                    │
│   Gmail asks you: "Do you want to let Instagram             │
│   access your name, email, and profile?"                    │
│        │                                                    │
│        │  You click YES (this is called giving CONSENT)     │
│        ▼                                                    │
│   Gmail gives Instagram only what it needs                  │
│   (NOT your password — just your profile data)              │
│        │                                                    │
│        ▼                                                    │
│   Instagram logs you in ✓                                   │
│   "Welcome, Shreyansh!"                                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Key Takeaway from Step 1

> OAuth lets a third-party app (Instagram) access your protected data from another service (Gmail) **on your behalf**, without ever seeing your password. You stay in control — you decide what to share, and you can revoke it anytime.

---
# Step 2 — The 4 Key Actors in OAuth 2.0

---

The instructor is very clear about this: **you must understand these 4 actors cold**, because every step of the OAuth flow is just a conversation between these four. If you're fuzzy on who is who, the flow will confuse you.

Let's use the same real-world example the instructor uses throughout: **You (Shreyansh) want to log into Instagram using your Gmail account.**

---

## The 4 Actors

---

### 1. Resource Owner
> **Who has the data? Who owns it?**

That's **you** — Shreyansh. You are the one who has a Gmail account. Your name, email, date of birth, profile picture — all of that is *your* data. You own it. So you are the **Resource Owner.**

Simple rule: **The user = Resource Owner.**

---

### 2. Client
> **Who wants to access the data?**

That's **Instagram.** Instagram is the third-party app that wants to use your Gmail data to log you in. It is the one *initiating the request* — it's asking Gmail, "hey, can I get this user's profile?"

Simple rule: **The third-party app that wants your data = Client.**

> ⚠️ Common confusion: the word "client" here does NOT mean your browser or your phone. It means the **application** (Instagram) that is requesting access to your protected data.

---

### 3. Authorization Server
> **Who asks for your permission and issues tokens?**

This is **Gmail's Authorization Server.** Its job is to:
- Authenticate you (check that you are really who you say you are)
- Show you the consent screen ("Do you allow Instagram to access your profile?")
- Issue tokens to Instagram once you say yes

Think of it as the **security gate** — nothing gets through without going past this.

> 📝 Note from instructor: In real systems, sometimes one service (like Gmail) plays *both* the Authorization Server role and the Resource Server role. But for clarity — and in most real architectures — these are treated as two separate components.

---

### 4. Resource Server
> **Who is actually storing your data?**

This is **Gmail's Resource Server** — the actual server that holds your profile data (name, email, age, address, etc.). Once Instagram has a valid token, it talks to this server to fetch your information.

Simple rule: **The server hosting your protected data = Resource Server.**

---

## All 4 Together — Visual

```
┌──────────────────────────────────────────────────────────────────────┐
│                                                                      │
│   1. RESOURCE OWNER          2. CLIENT                               │
│   ┌──────────────┐           ┌──────────────┐                        │
│   │   Shreyansh  │           │  Instagram   │                        │
│   │   (You)      │           │  (Third-     │                        │
│   │              │           │   party App) │                        │
│   └──────────────┘           └──────────────┘                        │
│         │                           │                                │
│         │                           │                                │
│         ▼                           ▼                                │
│   3. AUTHORIZATION SERVER    4. RESOURCE SERVER                      │
│   ┌──────────────────┐       ┌──────────────────┐                    │
│   │  Gmail Auth      │       │  Gmail Resource  │                    │
│   │  Server          │       │  Server          │                    │
│   │                  │       │                  │                    │
│   │  - Authenticates │       │  - Stores your   │                    │
│   │    you           │       │    actual data   │                    │
│   │  - Shows consent │       │    (name, email, │                    │
│   │    screen        │       │    DOB, address) │                    │
│   │  - Issues tokens │       │  - Serves data   │                    │
│   │                  │       │    when token is │                    │
│   │                  │       │    valid         │                    │
│   └──────────────────┘       └──────────────────┘                    │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Quick Cheat Sheet

| Actor | Who it is (in our example) | Simple role |
|---|---|---|
| Resource Owner | You / Shreyansh | Owns the data |
| Client | Instagram | Wants the data |
| Authorization Server | Gmail Auth Server | Guards access, issues tokens |
| Resource Server | Gmail Resource Server | Holds the actual data |

---

## One Thing the Instructor Wants You to Remember

Gmail here is playing **two roles at once** — it has both an Authorization Server and a Resource Server. You'll see this often in real systems. Google, GitHub, Facebook — they all have their own authorization server *and* their own resource server. They're just two separate components that happen to belong to the same company.

---
# Step 3 — Authorization Code Grant Flow

---

The instructor calls this the **most important and most widely used** grant type. Everything else builds on top of understanding this flow well. So let's go through it exactly the way the instructor teaches it — step by step, actor by actor.

---

## The Setup (Before the Flow Begins)

Our scenario:
- You (Shreyansh) are already logged into Gmail
- You want to log into **Instagram** using "Sign in with Google"
- Gmail has your name, email, DOB, address — all your protected data
- Instagram wants to use that data to sign you in

There are **2 tokens** you'll encounter in this flow. Don't worry about them deeply yet — just keep these definitions in the back of your mind:

| Token | Lifetime | Purpose |
|---|---|---|
| **Access Token** | Short-lived (e.g. 15–60 mins) | Used to fetch protected data from Resource Server |
| **Refresh Token** | Long-lived | Used to get a new Access Token when it expires |

---

## The Full Flow — Step by Step

---

### 🔷 Phase 1: Registration (One-time setup)

Before any user even tries to log in, **Instagram must register itself** with Gmail's Authorization Server. This is a one-time setup that happens at the developer level.

```
┌─────────────┐                    ┌──────────────────────┐
│  Instagram  │  ── Register ──▶   │  Gmail Auth Server   │
│  (Client)   │                    │                      │
│             │  ◀── Client ID ──  │                      │
│             │      + Secret ──   │                      │
└─────────────┘                    └──────────────────────┘
```

Instagram sends:
- Its app name
- Up to **3 redirect URIs** (callback URLs — where Gmail should send the user back after authorization)

Gmail's Authorization Server responds with:
- **Client ID** — a public identifier for Instagram
- **Client Secret** — a confidential key, known **only** to Instagram and Gmail's Auth Server

> ⚠️ The instructor is very firm here: **Client Secret is highly confidential.** It is used later to authenticate Instagram itself to the Authorization Server. If it leaks, anyone can impersonate Instagram.

---

### 🔷 Phase 2: User Clicks "Sign in with Google"

You (Shreyansh) land on Instagram and instead of filling a form, you click **"Sign in with Google."**

Instagram now redirects you to Gmail's Authorization Server, passing along some important parameters:

```
┌─────────────┐                         ┌──────────────────────┐
│  You        │── clicks sign in ──▶    │                      │
│  (Resource  │                         │                      │
│   Owner)    │                         │  Gmail Auth Server   │
│             │◀── Instagram redirects  │                      │
│             │    you here ──────────▶ │                      │
└─────────────┘                         └──────────────────────┘

Parameters Instagram sends along:
┌─────────────────────────────────────────────────────┐
│  response_type = code                               │
│  client_id     = (from registration)                │
│  redirect_uri  = (optional, one of the 3 registered)│
│  scope         = email profile address              │
│  state         = SJ111 (random unique value)        │
└─────────────────────────────────────────────────────┘
```

Let's understand each parameter:

| Parameter | What it means |
|---|---|
| `response_type=code` | "I want you to send me back an Authorization Code" |
| `client_id` | "I am Instagram — here's my ID from registration" |
| `redirect_uri` | "After authorization, send the user back here" (optional — if not given, one from registration is picked) |
| `scope` | "I only want access to these specific things — email, profile, address" |
| `state` | A random unique value Instagram generates. Protects against CSRF attacks (explained in Step 4) |

---

### 🔷 Phase 3: You Authenticate & Give Consent

Now you're on Gmail's login page. Two things happen here:

1. **Authentication** — if you're not already logged in, you enter your Gmail username and password
2. **Consent** — Gmail shows you a screen like:

```
┌────────────────────────────────────────────────┐
│                                                │
│  Instagram wants to access:                    │
│    ✅ Your email address                        │
│    ✅ Your profile info (name, picture)         │
│    ✅ Your address                              │
│                                                │
│  [ Allow ]        [ Deny ]                     │
│                                                │
└────────────────────────────────────────────────┘
```

You click **Allow.**

---

### 🔷 Phase 4: Authorization Server Sends Authorization Code

After you give consent, Gmail's Auth Server calls the **redirect URI** (the callback URL Instagram registered) and passes back:

```
┌──────────────────────┐                    ┌─────────────┐
│  Gmail Auth Server   │── redirect to ──▶  │  Instagram  │
│                      │   callback URI     │  (Client)   │
│                      │   with:            │             │
│                      │   code = ABC123    │             │
│                      │   state = SJ111    │             │
└──────────────────────┘                    └─────────────┘
```

Instagram checks: **does the `state` in the response match the `state` it originally sent?**
- If yes → proceed
- If no → discard (possible CSRF attack — Step 4 covers this in detail)

> 📝 **Important:** The Authorization Code is **single-use only.** Once used, it cannot be used again.

---

### 🔷 Phase 5: Instagram Exchanges Code for Tokens

Instagram now calls Gmail's Auth Server again — this time to exchange the Authorization Code for actual tokens:

```
┌─────────────┐                         ┌──────────────────────┐
│  Instagram  │── POST /token ────────▶ │  Gmail Auth Server   │
│  (Client)   │                         │                      │
│             │   grant_type = authorization_code              │
│             │   code       = ABC123                          │
│             │   client_id  = (from registration)             │
│             │   client_secret = (confidential!)              │
│             │   redirect_uri  = (optional)                   │
│             │                                                │
│             │◀── Access Token + Refresh Token ─────────────  │
└─────────────┘                         └──────────────────────┘
```

Notice that **client_secret is used here** — this is how Instagram proves to Gmail's Auth Server that it is the legitimate Instagram app, not some imposter.

Response from Auth Server:
```
{
  "access_token":  "eyJ...",
  "token_type":    "Bearer",
  "expires_in":    3600,
  "refresh_token": "dGhp..."
}
```

| Field | Meaning |
|---|---|
| `access_token` | Short-lived token to access protected data |
| `token_type = Bearer` | Pass this token in the `Authorization` header of every request |
| `expires_in = 3600` | Token expires in 3600 seconds (1 hour) — just an example |
| `refresh_token` | Long-lived token — use this to get a new access token when the current one expires |

---

### 🔷 Phase 6: Instagram Fetches Your Data

Now Instagram uses the Access Token to call Gmail's **Resource Server** and fetch your profile:

```
┌─────────────┐                         ┌──────────────────────┐
│  Instagram  │── GET /userinfo ───────▶│  Gmail Resource      │
│  (Client)   │   Authorization:        │  Server              │
│             │   Bearer eyJ...         │                      │
└─────────────┘                         └──────────────────────┘
```

But the Resource Server doesn't just hand over the data blindly. It first validates the token:

```
┌──────────────────────┐                ┌──────────────────────┐
│  Gmail Resource      │── validate ──▶ │  Gmail Auth Server   │
│  Server              │   token?       │                      │
│                      │◀── valid/      │                      │
│                      │    invalid ─── │                      │
└──────────────────────┘                └──────────────────────┘
```

- If **valid** → Resource Server sends back your profile (name, email, DOB, etc.) to Instagram
- If **invalid** → Request is rejected with a `401 Unauthorized` error

---

### 🔷 Phase 7: You're Logged In!

Instagram now has your name and email. It logs you in:

```
"Welcome, Shreyansh! 🎉"
```

---

## The Complete Flow — Master Diagram

```
  Resource        Instagram           Gmail Auth         Gmail Resource
  Owner           (Client)            Server             Server
  (You)
    │                │                    │                   │
    │   1. Register  │                    │                   │
    │                │──── Register ─────▶│                   │
    │                │◀─ Client ID+Secret─│                   │
    │                │                    │                   │
    │  2. Click      │                    │                   │
    │  Sign in       │                    │                   │
    │──with Google──▶│                    │                   │
    │                │                    │                   │
    │  3. Redirect   │                    │                   │
    │◀───────────────│                    │                   │
    │─── response_type=code ────────────▶ │                   │
    │    client_id, scope, state          │                   │
    │                │                    │                   │
    │  4. Authenticate & Consent          │                   │
    │────────────────────────────────────▶│                   │
    │                │                    │                   │
    │                │  5. Auth Code      │                   │
    │                │◀─── code=ABC123 ───│                   │
    │                │     state=SJ111    │                   │
    │                │                    │                   │
    │                │  6. Request Token  │                   │
    │                │──── code=ABC123 ──▶│                   │
    │                │     client_secret  │                   │
    │                │                    │                   │
    │                │  7. Tokens issued  │                   │
    │                │◀─ access_token ────│                   │
    │                │   refresh_token    │                   │
    │                │                    │                   │
    │                │  8. Fetch Data     │                   │
    │                │────────────────────────── Bearer ────▶ │
    │                │                    │                   │
    │                │                    │  9. Validate Token│
    │                │                    │◀──────────────────│
    │                │                    │──── valid/invalid▶│
    │                │                    │                   │
    │                │  10. User Data     │                   │
    │                │◀────────────────────────────────────── │
    │                │                    │                   │
    │  11. Logged In │                    │                   │
    │◀───────────────│                    │                   │
    │ "Welcome,      │                    │                   │
    │  Shreyansh!"   │                    │                   │
```

---

### 🔷 Bonus: What Happens When Access Token Expires?

This is where **Refresh Token** comes in. Instagram calls the token endpoint again, but this time with `grant_type=refresh_token`:

```
POST /token
grant_type    = refresh_token
refresh_token = dGhp...
client_id     = (from registration)
client_secret = (confidential)
redirect_uri  = (optional)
```

Response:
```
{
  "access_token":  "newToken...",
  "refresh_token": "newRefreshToken..."
}
```

> 📝 Notice: you don't need to involve the user at all during a refresh. No consent screen, no login — Instagram silently gets a new access token in the background. The user experience is seamless.

---

## Why is it called "Authorization CODE Grant"?

Because the flow involves an **intermediate code** (the Authorization Code) that gets exchanged for the actual token. It's a two-step process:

```
Step 1:  User consent  →  Authorization Code
Step 2:  Auth Code     →  Access Token + Refresh Token
```

This extra step exists for security. The access token is never exposed to the browser — it's exchanged server-to-server (Instagram's backend talking directly to Gmail's Auth Server). More on why this matters when we compare it with the Implicit Grant in Step 6.

---
# Step 4 — CSRF Attack & How `state` Prevents It

---

The instructor spends good time on this because it's not just theory — it's a real attack that OAuth's `state` parameter is specifically designed to block. Let's walk through it exactly the way the instructor explains it.

---

## First — What is CSRF?

**CSRF = Cross-Site Request Forgery**

In simple terms: an attacker tricks your system into accepting *their* authorization code as if it were *yours.* The result? You end up logged into Instagram — but using the attacker's account data. Anything you do on Instagram (upload files, access drive, etc.) goes into the attacker's account.

---

## The Attack — How It Happens (Without `state`)

Let's set the stage:

```
People involved:
- Shreyansh     → legitimate Resource Owner (you)
- Instagram     → Client (legitimate app)
- Gmail Auth    → Authorization Server
- Attacker      → bad actor trying to exploit the flow
```

---

### Step 1: Attacker initiates their own OAuth flow

The attacker goes to Instagram and clicks "Sign in with Google" — just like any normal user would. Instagram redirects them to Gmail's Authorization Server.

```
┌──────────────┐                    ┌──────────────────────┐
│   Attacker   │── authorize ──────▶│  Gmail Auth Server   │
│              │                    │                      │
│              │◀── Auth Code ──────│  (attacker's code)   │
│              │    = XYZ789        │                      │
└──────────────┘                    └──────────────────────┘
```

The attacker gets their own **Authorization Code (XYZ789).**

---

### Step 2: Attacker deliberately does NOT use it

Remember — an Authorization Code can only be used **once.** The attacker saves it and waits. They do not exchange it for a token.

---

### Step 3: You (Shreyansh) start your own legitimate OAuth flow

Now you go to Instagram and click "Sign in with Google." Instagram redirects you to Gmail's Authorization Server. You authenticate, give consent, and Gmail's Auth Server is about to send back *your* Authorization Code.

```
┌──────────────┐                    ┌──────────────────────┐
│   Shreyansh  │── authorize ──────▶│  Gmail Auth Server   │
│   (You)      │                    │                      │
│              │   waiting for      │   about to send      │
│              │   response...      │   YOUR code back     │
└──────────────┘                    └──────────────────────┘
```

---

### Step 4: Attacker intercepts and injects their code

Before Gmail's response reaches Instagram, the attacker intercepts the flow and **substitutes their own Authorization Code (XYZ789)** in place of yours.

```
┌──────────────────────┐
│  Gmail Auth Server   │──── your real code ────▶ (intercepted!)
└──────────────────────┘                               │
                                                       │
                                          Attacker injects XYZ789
                                                       │
                                                       ▼
                                          ┌─────────────┐
                                          │  Instagram  │
                                          │  receives   │
                                          │  XYZ789     │
                                          └─────────────┘
```

---

### Step 5: Instagram unknowingly uses the attacker's code

Instagram was waiting for an Authorization Code. It got one (XYZ789 — the attacker's). It doesn't know it's been swapped. So it goes ahead and exchanges it for tokens:

```
┌─────────────┐                    ┌──────────────────────┐
│  Instagram  │── POST /token ────▶│  Gmail Auth Server   │
│             │   code = XYZ789    │                      │
│             │◀── access_token ───│  (attacker's token)  │
└─────────────┘                    └──────────────────────┘
```

Gmail's Auth Server sees a valid code, issues a valid token — but this token belongs to the **attacker's Gmail account.**

---

### Step 6: You get logged into the attacker's account

Instagram uses the token to fetch data from Gmail's Resource Server. It gets back the attacker's name, email, and profile. You (Shreyansh) are now logged into Instagram — **as the attacker.**

```
┌──────────────────────────────────────────────────┐
│                                                  │
│  You think you're logged into YOUR account       │
│  But actually you're in the ATTACKER'S account   │
│                                                  │
│  You upload a file → goes to attacker's Drive    │
│  You save something → saved to attacker's data   │
│                                                  │
└──────────────────────────────────────────────────┘
```

This is the CSRF attack. Scary, right?

---

## The Full Attack Diagram

```
  Attacker            Instagram           Gmail Auth         Shreyansh
                      (Client)            Server             (You)
     │                    │                   │                  │
     │  1. Start OAuth    │                   │                  │
     │───── authorize ───▶│                   │                  │
     │                    │──── redirect ────▶│                  │
     │                    │                   │                  │
     │  2. Get Auth Code  │                   │                  │
     │◀───────────────────────── XYZ789 ──────│                  │
     │                    │                   │                  │
     │  3. SAVE it.       │                   │                  │
     │     Don't use it.  │                   │                  │
     │                    │                   │                  │
     │                    │                   │  4. You start    │
     │                    │                   │     OAuth flow   │
     │                    │◀──── authorize ───────────────────── │
     │                    │──── redirect ────▶│                  │
     │                    │                   │◀── authenticate──│
     │                    │                   │    + consent     │
     │                    │                   │                  │
     │  5. INTERCEPT response. Inject XYZ789  │                  │
     │────────────────────▶│                  │                  │
     │                    │  (thinks it got   │                  │
     │                    │   your real code) │                  │
     │                    │                   │                  │
     │                    │  6. Exchange code │                  │
     │                    │──── XYZ789 ──────▶│                  │
     │                    │◀─── token ────────│                  │
     │                    │  (attacker's      │                  │
     │                    │   token)          │                  │
     │                    │                   │                  │
     │                    │  7. Fetch data    │                  │
     │                    │  gets attacker's  │                  │
     │                    │  profile          │                  │
     │                    │                   │                  │
     │                    │  8. Logs YOU in   │                  │
     │                    │  as ATTACKER  ──────────────────────▶│
     │                    │                   │   "Welcome,      │
     │                    │                   │    Attacker!" 😱 │
```

---

## How `state` Shuts This Down

The `state` parameter is a **random, unique, hard-to-guess value** that Instagram generates fresh for every single authorization request.

Here's how it works:

---

### Step 1: Instagram generates a state value

When you click "Sign in with Google," Instagram generates a random value — let's say `SJ111` — and sends it along with the authorization request:

```
GET /authorize
  response_type = code
  client_id     = instagram_id
  scope         = email profile
  state         = SJ111        ← random, unique, hard to guess
```

Instagram also **stores this value** on its side (tied to your session).

---

### Step 2: Gmail's Auth Server echoes it back

After you authenticate and give consent, Gmail's Auth Server sends back the Authorization Code — and **includes the same `state` value** in the response:

```
GET /callback
  code  = ABC123
  state = SJ111    ← same value echoed back
```

---

### Step 3: Instagram validates the state

Instagram now compares:
- The `state` it **sent** → `SJ111`
- The `state` it **received** → `SJ111`

```
┌─────────────────────────────────────────────────────┐
│                                                     │
│   state sent     = SJ111                            │
│   state received = SJ111                            │
│                                                     │
│   ✅ Match → Accept the code, proceed                │
│   ❌ No match → Discard, possible CSRF attack        │
│   ❌ No state at all → Discard                       │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

### Why the Attacker is Blocked Now

The attacker intercepts the response and injects their own Authorization Code (XYZ789). But their injected response has a **different state value** (or no state at all) — because they couldn't know or guess the `SJ111` that Instagram generated specifically for your session.

```
┌──────────────────────────────────────────────────────────┐
│                                                          │
│  Attacker injects:                                       │
│    code  = XYZ789                                        │
│    state = ??? (doesn't know SJ111)                      │
│                                                          │
│  Instagram checks:                                       │
│    state sent     = SJ111                                │
│    state received = ??? ← doesn't match                  │
│                                                          │
│  Instagram says: ❌ DISCARD. This is not my response.     │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

Attack blocked. ✅

---

## Summary Diagram — With vs Without `state`

```
WITHOUT state                      WITH state
─────────────────────────────      ─────────────────────────────
Instagram sends authorize          Instagram sends authorize
request (no state)                 request WITH state = SJ111
        │                                  │
        │                                  │ (stores SJ111)
        ▼                                  ▼
Attacker intercepts,               Attacker intercepts,
injects their code                 injects their code
        │                          BUT doesn't know SJ111
        │                                  │
        ▼                                  ▼
Instagram accepts it 😱            Instagram checks state
        │                          SJ111 ≠ attacker's value
        ▼                                  │
You logged into                            ▼
attacker's account 💀              Request DISCARDED ✅
                                   You are safe 🔒
```

---

## Key Points the Instructor Wants You to Remember

> ✅ `state` is **recommended** but not mandatory in the OAuth spec — however, skipping it is a serious security risk.

> ✅ `state` must be **random, unique, and hard to guess** — a predictable value defeats the whole purpose.

> ✅ Instagram generates `state`, sends it, stores it, and then **validates it matches** when the response comes back.

> ✅ If `state` is missing from the response, or doesn't match — **discard the response entirely.**

---
# Step 5 — Request & Response Breakdown (API Level Detail)

---

The instructor is clear here: the API names he uses are **sample names to aid understanding** — they may not be the exact names in real implementations. Always refer to the official RFC documentation for exact API specs. But the parameters and the flow are accurate and important to understand.

---

## API 1 — Registration (Client → Authorization Server)

This is the **one-time setup** where Instagram registers itself with Gmail's Authorization Server.

---

### Request

```
POST /register

{
  "client_name"    : "Instagram",
  "redirect_uris"  : [
      "https://instagram.com/callback",
      "https://instagram.com/auth/callback",
      "https://instagram.com/oauth/callback"
  ]
}
```

| Parameter | Description |
|---|---|
| `client_name` | Name of the third-party app registering itself |
| `redirect_uris` | Up to **3 URIs** where Auth Server can redirect the user after authorization. These are your callback URLs. |

---

### Response

```json
{
  "client_id"     : "instagram_client_id_abc",
  "client_secret" : "super_secret_xyz_123"
}
```

| Field | Description |
|---|---|
| `client_id` | Public identifier for Instagram. Sent openly in future requests. |
| `client_secret` | Confidential. Known **only** to Instagram and Gmail's Auth Server. Used later to authenticate Instagram itself. |

> ⚠️ **The instructor's warning:** Client Secret must never be exposed publicly. It is the key Instagram uses to prove its own identity to the Auth Server. If it leaks, anyone can impersonate Instagram.

---

```
┌─────────────┐         POST /register           ┌──────────────────────┐
│  Instagram  │─────────────────────────────────▶│  Gmail Auth Server   │
│  (Client)   │   client_name, redirect_uris     │                      │
│             │                                  │                      │
│             │◀─────────────────────────────────│                      │
│             │   client_id + client_secret      │                      │
└─────────────┘                                  └──────────────────────┘
```

---

## API 2 — Authorization Request (Client → Auth Server, via browser redirect)

When you click "Sign in with Google" on Instagram, Instagram redirects your browser to Gmail's Auth Server with this request:

---

### Request

```
GET /authorize

?response_type = code
&client_id     = instagram_client_id_abc
&redirect_uri  = https://instagram.com/callback   (optional)
&scope         = email profile address
&state         = SJ111
```

| Parameter | Required? | Description |
|---|---|---|
| `response_type=code` | ✅ Required | Tells Auth Server: "I want an Authorization Code back" |
| `client_id` | ✅ Required | Instagram's identity — got this during registration |
| `redirect_uri` | ⚪ Optional | Where to send user after consent. If not provided, Auth Server picks one from the registered list |
| `scope` | ✅ Required | What data Instagram wants to access. Space-separated values |
| `state` | ⚠️ Recommended | Random unique value. Protects against CSRF. Not mandatory but strongly advised |

> 📝 **On redirect_uri:** If you provide it, it **must match** one of the URIs registered in Step 1. You can't pass a random URL here — that would be a security risk (attacker redirecting your token somewhere else).

> 📝 **On scope:** This is how OAuth keeps access limited. Instagram only gets what it asks for here — it can't suddenly access your Google Drive or Gmail inbox unless that scope was requested and you consented to it.

---

### Response (after you authenticate & give consent)

Auth Server redirects you back to Instagram's callback URI:

```
GET https://instagram.com/callback

?code  = AUTH_CODE_ABC123
&state = SJ111
```

| Field | Description |
|---|---|
| `code` | The Authorization Code. Single-use. Short-lived. |
| `state` | Same value Instagram sent. Instagram must validate this matches. |

---

```
┌──────────────┐   GET /authorize (redirect)   ┌──────────────────────┐
│   You        │──────────────────────────────▶│  Gmail Auth Server   │
│  (Browser)   │   response_type, client_id,   │                      │
│              │   scope, state                │  Shows login +       │
│              │                               │  consent screen      │
│              │◀──────────────────────────────│                      │
│              │   redirects to callback URI   │                      │
│              │   with code + state           │                      │
└──────────────┘                               └──────────────────────┘
        │
        │ browser lands on Instagram's callback
        ▼
┌─────────────┐
│  Instagram  │  receives code=AUTH_CODE_ABC123
│  (Client)   │  validates state=SJ111 ✅
└─────────────┘
```

---

## API 3 — Token Exchange (Client → Auth Server)

Instagram now has the Authorization Code. It exchanges it for actual tokens — this is a **server-to-server** call (not through the browser):

---

### Request

```
POST /token

{
  "grant_type"    : "authorization_code",
  "code"          : "AUTH_CODE_ABC123",
  "client_id"     : "instagram_client_id_abc",
  "client_secret" : "super_secret_xyz_123",
  "redirect_uri"  : "https://instagram.com/callback"  (optional)
}
```

| Parameter | Description |
|---|---|
| `grant_type=authorization_code` | Tells Auth Server which OAuth flow is being used |
| `code` | The Authorization Code received in the previous step |
| `client_id` | Instagram's public ID |
| `client_secret` | Instagram's confidential secret — this authenticates Instagram itself |
| `redirect_uri` | Optional. If provided, must match registration |

> 📝 **Why is client_secret needed here?** Because this endpoint issues powerful tokens that can access your protected data. The Auth Server needs to verify that the request is coming from the *real* Instagram — not some random app that somehow got hold of a valid Authorization Code.

---

### Response

```json
{
  "access_token"  : "eyJhbGciOiJSUzI1NiJ9...",
  "token_type"    : "Bearer",
  "expires_in"    : 3600,
  "refresh_token" : "dGhpcyBpcyBh..."
}
```

| Field | Description |
|---|---|
| `access_token` | Short-lived token to access protected data. Can be a plain string or a JWT. |
| `token_type=Bearer` | Means: pass this token in the `Authorization` header as `Bearer <token>` |
| `expires_in=3600` | Token expires in 3600 seconds (1 hour). This is just an example — can be 15 mins, 30 mins, etc. |
| `refresh_token` | Long-lived token. Used to get a new access token when the current one expires. |

> 📝 **On access_token format:** The instructor mentions it can be either a **plain opaque string** (only the Auth Server knows what it means) or a **JWT token** (self-contained, can be decoded). JWT will be covered in a separate lecture.

---

```
┌─────────────┐      POST /token (server-to-server)     ┌──────────────────────┐
│  Instagram  │────────────────────────────────────────▶│  Gmail Auth Server   │
│  (Client)   │  grant_type, code, client_id,           │                      │
│             │  client_secret, redirect_uri            │                      │
│             │                                         │                      │
│             │◀────────────────────────────────────────│                      │
│             │  access_token, token_type,              │                      │
│             │  expires_in, refresh_token              │                      │
└─────────────┘                                         └──────────────────────┘
```

---

## API 4 — Accessing Protected Data (Client → Resource Server)

Instagram now uses the access token to fetch your data from Gmail's Resource Server:

---

### Request

```
GET /userinfo

Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
```

The token goes in the **Authorization header** as `Bearer <token>` — this is what `token_type=Bearer` told Instagram to do.

---

### What Resource Server Does Internally

```
┌──────────────────────┐      validate token?      ┌──────────────────────┐
│  Gmail Resource      │──────────────────────────▶│  Gmail Auth Server   │
│  Server              │                           │                      │
│                      │◀──────────────────────────│                      │
│                      │   valid ✅ or invalid ❌    │                      │
└──────────────────────┘                           └──────────────────────┘
```

### Response (if token valid)

```json
{
  "name"    : "Shreyansh",
  "email"   : "shreyansh@gmail.com",
  "dob"     : "01-01-1995",
  "address" : "Mumbai, India"
}
```

Instagram uses this data to log you in. ✅

If token is invalid → `401 Unauthorized` is returned. ❌

---

## API 5 — Refresh Token (Client → Auth Server)

When the access token expires, Instagram silently gets a new one using the refresh token — **no user involvement needed:**

---

### Request

```
POST /token

{
  "grant_type"    : "refresh_token",
  "refresh_token" : "dGhpcyBpcyBh...",
  "client_id"     : "instagram_client_id_abc",
  "client_secret" : "super_secret_xyz_123",
  "redirect_uri"  : "https://instagram.com/callback"  (optional)
}
```

| Parameter | Description |
|---|---|
| `grant_type=refresh_token` | Tells Auth Server: "I'm using a refresh token to get a new access token" |
| `refresh_token` | The refresh token received during token exchange |
| `client_id` + `client_secret` | Instagram authenticates itself again |

> 📝 Notice: **no username/password, no authorization code, no user consent screen** — the refresh happens completely in the background. The user experience is seamless.

---

### Response

```json
{
  "access_token"  : "newAccessToken_xyz...",
  "token_type"    : "Bearer",
  "expires_in"    : 3600,
  "refresh_token" : "newRefreshToken_abc..."
}
```

Both a new access token AND a new refresh token are returned. The cycle continues.

---

## Complete API Flow — Master Summary

```
┌────────────────────────────────────────────────────────────────────────┐
│                                                                        │
│  1. POST /register                                                     │
│     Instagram → Auth Server                                            │
│     Gets: client_id + client_secret                                    │
│                          │                                             │
│                          ▼                                             │
│  2. GET /authorize  (via browser redirect)                             │
│     Instagram → Auth Server (response_type=code, scope, state)         │
│     You: login + consent                                               │
│     Gets: authorization code + state                                   │
│                          │                                             │
│                          ▼                                             │
│  3. POST /token  (server-to-server)                                    │
│     Instagram → Auth Server (code + client_secret)                     │
│     Gets: access_token + refresh_token                                 │
│                          │                                             │
│                          ▼                                             │
│  4. GET /userinfo  (with Bearer token)                                 │
│     Instagram → Resource Server                                        │
│     Resource Server validates token with Auth Server                   │
│     Gets: your profile data                                            │
│                          │                                             │
│                          ▼                                             │
│  5. POST /token  (when access token expires)                           │
│     Instagram → Auth Server (grant_type=refresh_token)                 │
│     Gets: new access_token + new refresh_token                         │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Quick Reference — All Parameters at a Glance

```
┌─────────────────────┬────────────────────────────────────────────────┐
│  API                │  Key Parameters                                │
├─────────────────────┼────────────────────────────────────────────────┤
│  /register          │  client_name, redirect_uris                    │
│                     │  → client_id, client_secret                    │
├─────────────────────┼────────────────────────────────────────────────┤
│  /authorize         │  response_type=code, client_id,                │
│                     │  scope, state, redirect_uri(optional)          │
│                     │  → code, state                                 │
├─────────────────────┼────────────────────────────────────────────────┤
│  /token             │  grant_type=authorization_code, code,          │
│  (exchange)         │  client_id, client_secret,                     │
│                     │  redirect_uri(optional)                        │
│                     │  → access_token, refresh_token,                │
│                     │    token_type, expires_in                      │
├─────────────────────┼────────────────────────────────────────────────┤
│  /userinfo          │  Authorization: Bearer <access_token>          │
│                     │  → protected user data                         │
├─────────────────────┼────────────────────────────────────────────────┤
│  /token             │  grant_type=refresh_token, refresh_token,      │
│  (refresh)          │  client_id, client_secret                      │
│                     │  → new access_token, new refresh_token         │
└─────────────────────┴────────────────────────────────────────────────┘
```

---
# Step 6 — The Other Grant Types

---

The instructor covers 4 grant types in total (plus Refresh Token which we already covered). He's clear that **Authorization Code Grant is the gold standard** — the others exist for specific scenarios, and some are outright discouraged. Let's go through each one carefully.

---

## Quick Recap — What is a "Grant Type"?

A grant type is simply the **mechanism a client uses to obtain an access token.** Different situations call for different mechanisms. Think of it like different ways to prove your identity at a security desk — sometimes you show your ID card, sometimes a badge, sometimes a PIN.

```
┌─────────────────────────────────────────────────────────────┐
│              OAuth 2.0 Grant Types                          │
│                                                             │
│  1. Authorization Code Grant   ← most important, most used  │
│  2. Implicit Grant             ← not recommended anymore    │
│  3. Resource Owner Password    ← use with caution           │
│     Credentials Grant                                       │
│  4. Client Credentials Grant   ← for machine-to-machine     │
│  5. Refresh Token Grant        ← already covered in Step 5  │
└─────────────────────────────────────────────────────────────┘
```

---

## Grant Type 2 — Implicit Grant

---

### What's Different?

In Authorization Code Grant, the flow was **two steps:**
```
Step 1: User consent  →  Authorization Code
Step 2: Auth Code     →  Access Token
```

In Implicit Grant, there is **no Authorization Code at all.** You skip straight to getting the Access Token in one step — directly from the `/authorize` endpoint.

---

### The Flow

```
┌──────────────┐                         ┌──────────────────────┐
│  You         │── GET /authorize ──────▶│  Gmail Auth Server   │
│  (Browser)   │   response_type=token   │                      │
│              │   client_id             │  You login +         │
│              │   scope                 │  give consent        │
│              │   state                 │                      │
│              │                         │                      │
│              │◀── redirect to URI ─────│                      │
│              │    #access_token=xyz    │                      │
│              │    (in URL fragment)    │                      │
└──────────────┘                         └──────────────────────┘
```

### Request

```
GET /authorize

?response_type = token        ← KEY DIFFERENCE: "token" not "code"
&client_id     = instagram_client_id_abc
&redirect_uri  = https://instagram.com/callback
&scope         = email profile
&state         = SJ111
```

### Response

```
https://instagram.com/callback
  #access_token = eyJhbGci...
  &token_type   = Bearer
  &expires_in   = 3600
  &state        = SJ111
```

> 📝 Notice the `#` — the token is returned in the **URL fragment** (the part after `#`). This means it's only accessible to the browser, not sent to the server in HTTP requests.

---

### Key Differences from Authorization Code Grant

```
┌─────────────────────────┬──────────────────────────────────────────┐
│  Authorization Code     │  Implicit Grant                          │
├─────────────────────────┼──────────────────────────────────────────┤
│  Two steps              │  One step                                │
│  Gets code first        │  Gets token directly                     │
│  Token exchanged        │  Token in URL fragment                   │
│  server-to-server       │  (exposed to browser)                    │
│  Has refresh token      │  NO refresh token                        │
│  Client secret used     │  NO client secret needed                 │
│  More secure            │  Less secure                             │
└─────────────────────────┴──────────────────────────────────────────┘
```

### Why No Refresh Token?

The instructor explains this simply — since there's no two-step process and no server-side exchange, if you need a new token, you just hit `/authorize` again directly. No need for a separate refresh mechanism.

### Why is it NOT Recommended?

The access token is exposed directly in the browser URL. Anyone who can see the URL (browser history, logs, referrer headers) can steal the token. The Authorization Code Grant keeps the token exchange server-side — much safer.

```
┌─────────────────────────────────────────────────────────────┐
│  ⚠️  Implicit Grant is officially discouraged               │
│                                                             │
│  The token lives in the browser URL — visible to:           │
│  - Browser history                                          │
│  - Server logs                                              │
│  - JavaScript on the page                                   │
│  - Referrer headers                                         │
│                                                             │
│  Use Authorization Code Grant with PKCE instead             │
│  (for single-page apps and mobile apps)                     │
└─────────────────────────────────────────────────────────────┘
```

---

## Grant Type 3 — Resource Owner Password Credentials Grant

---

### What's Different?

In all previous flows, **you never gave your Gmail password to Instagram.** That was the whole point of OAuth.

In this grant type — you do. You give Instagram your Gmail username and password directly, and Instagram passes them to Gmail's Auth Server to get a token.

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  Normal OAuth:                                              │
│  You → Gmail (your password)   Instagram never sees it      │
│                                                             │
│  Password Grant:                                            │
│  You → Instagram (your Gmail password) → Gmail Auth Server  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### The Flow

There is **no `/authorize` call at all** in this grant type. Instagram directly calls `/token` with your credentials:

```
┌──────────────┐                         ┌──────────────────────┐
│  Instagram   │── POST /token ─────────▶│  Gmail Auth Server   │
│  (Client)    │                         │                      │
│              │   grant_type=password   │                      │
│              │   username=shreyansh    │                      │
│              │   password=mypassword   │                      │
│              │   client_id=...         │                      │
│              │   client_secret=...     │                      │
│              │   scope=email profile   │                      │
│              │                         │                      │
│              │◀── access_token ────────│                      │
│              │    refresh_token        │                      │
└──────────────┘                         └──────────────────────┘
```

### Request

```
POST /token

{
  "grant_type"    : "password",
  "username"      : "shreyansh@gmail.com",
  "password"      : "myGmailPassword",
  "client_id"     : "instagram_client_id_abc",
  "client_secret" : "super_secret_xyz_123",
  "scope"         : "email profile"
}
```

### Response

```json
{
  "access_token"  : "eyJhbGci...",
  "token_type"    : "Bearer",
  "expires_in"    : 3600,
  "refresh_token" : "dGhpcyBp..."
}
```

### Refreshing (when access token expires)

```
POST /token

{
  "grant_type"    : "refresh_token",
  "refresh_token" : "dGhpcyBp...",
  "client_id"     : "instagram_client_id_abc",
  "client_secret" : "super_secret_xyz_123"
}
```

> 📝 Notice: during refresh you do **not** need to pass username/password again. The refresh token is enough.

---

### When is it Used?

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  Only when:                                                 │
│  - The client app is highly trusted (e.g. first-party app)  │
│  - You own BOTH the client app AND the resource server      │
│  - There is no way to do a browser redirect                 │
│                                                             │
│  Example: Gmail's own official mobile app accessing         │
│  Gmail's own API — same company, fully trusted              │
│                                                             │
│  ⚠️  Never use this for third-party apps                    │
│  The whole point of OAuth was to NOT share passwords!       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Grant Type 4 — Client Credentials Grant

---

### What's Different? — The Key Concept

In all previous grant types, there were always **3 separate parties:**

```
Resource Owner (You)  +  Client (Instagram)  +  Auth/Resource Server (Gmail)
```

In Client Credentials Grant, the **Resource Owner and the Client are the same entity.** There is no human user involved at all. This is purely **machine-to-machine** communication.

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  Previous flows:                                            │
│  Resource Owner ≠ Client                                    │
│  (Shreyansh)      (Instagram)                               │
│                                                             │
│  Client Credentials Grant:                                  │
│  Resource Owner = Client                                    │
│  (the app itself owns and accesses its own data)            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Real-World Example

Imagine a backend service (Service A) that needs to call another backend service (Service B) to get some data. There's no human logging in — it's just two servers talking to each other. Service A authenticates itself using its own client credentials.

---

### The Flow

No `/authorize` call, no user, no consent screen — just a direct token request:

```
┌──────────────┐                         ┌──────────────────────┐
│  Client App  │── POST /token ─────────▶│  Auth Server         │
│  (also the   │                         │                      │
│   Resource   │   grant_type=           │                      │
│   Owner)     │   client_credentials    │                      │
│              │   client_id=...         │                      │
│              │   client_secret=...     │                      │
│              │   scope=...             │                      │
│              │                         │                      │
│              │◀── access_token ────────│                      │
└──────────────┘                         └──────────────────────┘
```

### Request

```
POST /token

{
  "grant_type"    : "client_credentials",
  "client_id"     : "service_a_client_id",
  "client_secret" : "service_a_secret",
  "scope"         : "read write"
}
```

### Response

```json
{
  "access_token" : "eyJhbGci...",
  "token_type"   : "Bearer",
  "expires_in"   : 3600
}
```

> 📝 **No refresh token** — and the instructor explains why clearly: since the client IS the resource owner, getting a new token is trivial. When the access token expires, just call `/token` again with your `client_id` and `client_secret`. You get a fresh token instantly. No user, no code, no complexity.

---

### Why No Refresh Token?

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  In Authorization Code Grant:                               │
│  Getting a new token requires user involvement              │
│  (login + consent) → so refresh token saves that effort     │
│                                                             │
│  In Client Credentials Grant:                               │
│  No user involved at all                                    │
│  Client just calls /token again with its own credentials    │
│  → refresh token is unnecessary                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## All 4 Grant Types — Side by Side Comparison

```
┌──────────────────────┬───────────┬───────────┬───────────┬────────────┐
│                      │  Auth     │ Implicit  │ Password  │  Client    │
│                      │  Code     │  Grant    │  Grant    │  Creds     │
├──────────────────────┼───────────┼───────────┼───────────┼────────────┤
│ /authorize call?     │    ✅      │    ✅      │    ❌      │    ❌       │
├──────────────────────┼───────────┼───────────┼───────────┼────────────┤
│ Auth Code step?      │    ✅      │    ❌      │    ❌      │    ❌       │
├──────────────────────┼───────────┼───────────┼───────────┼────────────┤
│ Client Secret used?  │    ✅      │    ❌      │    ✅      │    ✅       │
├──────────────────────┼───────────┼───────────┼───────────┼────────────┤
│ User password sent?  │    ❌      │    ❌      │    ✅      │    ❌       │
├──────────────────────┼───────────┼───────────┼───────────┼────────────┤
│ Access Token?        │    ✅      │    ✅      │    ✅      │    ✅       │
├──────────────────────┼───────────┼───────────┼───────────┼────────────┤
│ Refresh Token?       │    ✅      │    ❌      │    ✅      │    ❌       │
├──────────────────────┼───────────┼───────────┼───────────┼────────────┤
│ User involved?       │    ✅      │    ✅      │    ✅      │    ❌       │
├──────────────────────┼───────────┼───────────┼───────────┼────────────┤
│ Recommended?         │    ✅✅     │    ❌      │  ⚠️       │    ✅       │
│                      │  Highly   │Discouraged│ Caution   │(right use) │
└──────────────────────┴───────────┴───────────┴───────────┴────────────┘
```

---

## When to Use Which — Decision Guide

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│  Is there a human user involved?                                    │
│                                                                     │
│  YES                              NO                                │
│   │                                │                                │
│   ▼                                ▼                                │
│  Can you do a browser redirect?   Use CLIENT CREDENTIALS GRANT      │
│                                   (machine to machine)              │
│  YES              NO                                                │
│   │                │                                                │
│   ▼                ▼                                                │
│  Use AUTH CODE    Is this a fully                                   │
│  GRANT ✅         trusted first-party app?                           │
│  (most cases)                                                       │
│                   YES              NO                               │
│                    │                │                               │
│                    ▼                ▼                               │
│                  PASSWORD         Don't use OAuth                   │
│                  GRANT ⚠️         for this case                     │
│                  (use carefully)                                    │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Interview Tips the Instructor Implies 🎯

> **Q: What is the difference between Authorization Code Grant and Implicit Grant?**
> Auth Code Grant has a two-step process — first gets a code, then exchanges it for a token server-to-server. Implicit Grant skips the code step and returns the token directly in the URL, making it less secure. Implicit Grant also has no refresh token.

> **Q: Why is Implicit Grant discouraged?**
> The access token is exposed in the browser URL — visible in browser history, logs, and to JavaScript. Authorization Code Grant keeps the token exchange server-side, which is far more secure.

> **Q: When would you use Client Credentials Grant?**
> When there is no human user involved — purely machine-to-machine communication. The client itself is the resource owner. Example: a microservice authenticating to another microservice.

> **Q: Why does Client Credentials Grant have no refresh token?**
> Because the client can simply call the token endpoint again with its own credentials anytime — no user involvement needed, so a refresh token adds no value.

> **Q: Why is the state parameter important in OAuth?**
> It prevents CSRF attacks. It's a random unique value sent with the authorization request and echoed back in the response. The client validates it matches before accepting the authorization code.

---

## Complete OAuth 2.0 — Everything in One Picture

```
┌──────────────────────────────────────────────────────────────────────┐
│                        OAuth 2.0                                     │
│                   Open Authorization Framework                       │
│                                                                      │
│  PURPOSE: Secure third-party access to user protected data           │
│                                                                      │
│  4 ACTORS:                                                           │
│  Resource Owner → Client → Authorization Server → Resource Server    │
│                                                                      │
│  5 GRANT TYPES:                                                      │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐     │
│  │ 1. Authorization Code  → Most secure, most used             │     │
│  │    User → Code → Token (server-to-server exchange)          │     │
│  ├─────────────────────────────────────────────────────────────┤     │
│  │ 2. Implicit            → Discouraged                        │     │
│  │    User → Token directly in URL (no code step)              │     │
│  ├─────────────────────────────────────────────────────────────┤     │
│  │ 3. Password Credentials → Use with caution                  │     │
│  │    User gives password to client directly                   │     │
│  ├─────────────────────────────────────────────────────────────┤     │
│  │ 4. Client Credentials  → Machine to machine                 │     │
│  │    No user involved, client = resource owner                │     │
│  ├─────────────────────────────────────────────────────────────┤     │
│  │ 5. Refresh Token       → Renew expired access token         │     │
│  │    No user involvement needed                               │     │
│  └─────────────────────────────────────────────────────────────┘     │
│                                                                      │
│  2 KEY TOKENS:                                                       │
│  Access Token  → Short-lived, used to access data                    │
│  Refresh Token → Long-lived, used to renew access token              │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

That completes **Part 1 — What is OAuth 2.0** in full. The implementation in Spring Boot (OAuth + OIDC + Spring Security configuration) is a separate lecture and will need its own set of notes with the corresponding transcript.