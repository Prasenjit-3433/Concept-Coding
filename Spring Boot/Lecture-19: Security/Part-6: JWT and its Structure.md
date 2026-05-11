# JWT (JSON Web Token) — Complete Notes

## Step 1 of 5 — What is JWT, Where is it Used, and the Full Authentication Flow

---

### What Problem Does JWT Solve?

Before jumping into what JWT is, let's understand *why* it was needed.

The old way of handling authentication was using **Session IDs** (you might also know it as `JSessionID`). Here's how that worked:

```
CLIENT                  AUTH/RESOURCE SERVER              DATABASE
  |                            |                              |
  |-- login (user+pass) ------>|                              |
  |                            |-- create Session ID -------->|
  |                            |   store: sessionId,          |
  |                            |   userId, roles, expiry      |
  |<-- returns Session ID -----|                              |
  |                            |                              |
  |-- GET /resource            |                              |
  |   (sends Session ID) ----->|                              |
  |                            |-- query DB with sessionID -->|
  |                            |<-- fetch roles, expiry ------|
  |                            |   validate everything        |
  |<-- send response ----------|                              |
```

**The Session ID is just a random unique key** — nothing more. All the real data (roles, expiry, user info) lives in the DB. Every single request means a DB hit. That's the core problem.

**Problems with Session ID:**

- **Stateful** — the server has to remember state. The DB has to be alive, in sync, and responsive on every request.
- **Distributed system nightmare** — if you have multiple DB clusters (master-slave setup), all of them need to stay in sync. You never know which server a request will hit.
- **Extra latency** — every API call = one extra DB query just to validate who you are.

---

### So What is JWT?

JWT is simply **a secure way of transmitting information between parties as a JSON object.**

That information can be **digitally signed** (using RSA or HMAC), so the receiver can verify:
- who sent it
- that nothing was tampered with

But over time, JWT became the industry standard for three specific jobs:

```
┌─────────────────────────────────────────────────────────┐
│                   WHERE JWT IS USED                     │
├──────────────────┬──────────────────┬───────────────────┤
│  AUTHENTICATION  │  AUTHORIZATION   │   SINGLE SIGN ON  │
│                  │                  │       (SSO)       │
│ Confirms WHO     │ Checks WHAT you  │ Login once, access│
│ you are.         │ are allowed      │ multiple apps with│
│ "Yes, you are    │ to do.           │ the same token.   │
│  Shreyansh."     │ "Can Shreyansh   │                   │
│                  │  fetch this?"    │                   │
└──────────────────┴──────────────────┴───────────────────┘
```

---

### The Full JWT Authentication Flow

```
CLIENT              AUTH SERVER           RESOURCE SERVER (Your App)
  |                     |                          |
  |-- 1. login -------->|                          |
  |   (user + pass)     |                          |
  |                     |-- generates JWT token    |
  |<-- 2. JWT token ----|                          |
  |                     |                          |
  |                                                |
  |-- 3. GET /resource --------------------------->|
  |      Header: Authorization: Bearer <JWT>       |
  |                                                |
  |                     |<-- 4. verify token ------|
  |                     |    (calls auth server's  |
  |                     |     verify API)          |
  |                     |-- 5. "valid!" ---------->|
  |                                                |
  |<-- 6. returns requested data ----------------- |
```

Key point: **no DB is touched by your application at all**. The JWT itself carries all the information. That's what makes it **stateless**.

> **Interview Tip 🎯** — "How do you implement Single Sign On (SSO)?" → Answer: Using JWT. The user logs in once, gets a token. That same token is passed to App 1, App 2, App 3 — each app validates it against the auth server. No re-login needed.

---
# JWT (JSON Web Token) — Complete Notes

## Step 2 of 5 — JWT Structure: Header, Payload, and Signature

---

### The Big Picture First

A JWT token looks like this when you actually see one:

```
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV
```

Looks like random gibberish, right? But it has a very clear structure — **three parts, separated by dots (`.`)**:

```
<Header>.<Payload>.<Signature>

aaaaaaaaaa.bbbbbbbbbb.cccccccccccc
    │             │           │
  Part 1        Part 2      Part 3
  HEADER       PAYLOAD    SIGNATURE
```

Let's go one by one.

---

### Part 1 — Header

The header contains **metadata about the token itself** — not about the user, just about the token.

```
{
  "typ": "JWT",       <-- type: always "JWT", never anything else
  "alg": "RSA",       <-- signing algorithm used: RSA or HMAC
  "kid": "sfdsf234234"  <-- Key ID (we'll explain this in challenges section)
}
```

```
┌──────────────────────────────────────────────┐
│                   HEADER                     │
├────────────┬─────────────────────────────────┤
│  Field     │  Meaning                        │
├────────────┼─────────────────────────────────┤
│  typ       │  Type of token → always "JWT"   │
│  alg       │  Algorithm used to sign it:     │
│            │  RSA (asymmetric) or            │
│            │  HMAC (symmetric)               │
│  kid       │  Key ID → used to look up the   │
│            │  correct public key for verify  │
└────────────┴─────────────────────────────────┘
```

---

### Part 2 — Payload

The payload is where **the actual information lives** — about the user, permissions, expiry, etc. In JWT language, each piece of info inside payload is called a **Claim**.

Claims are of three types:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           PAYLOAD CLAIMS                                │
├─────────────────────┬──────────────────────┬────────────────────────────┤
│  REGISTERED CLAIMS  │   PUBLIC CLAIMS      │   PRIVATE CLAIMS           │
│  (reserved by JWT,  │   (custom, but       │   (custom, internal use    │
│   predefined names  │    understood by     │    only — not expected     │
│   & meanings)       │    multiple parties) │    other parties to know)  │
├─────────────────────┼──────────────────────┼────────────────────────────┤
│  iss → Issuer       │  email               │  iam (some internal        │
│  sub → Subject      │  country             │  field only the auth       │
│  aud → Audience     │  city                │  server understands)       │
│  exp → Expiry time  │  firstName           │                            │
│  nbf → Not Before   │  lastName            │  Resource servers don't    │
│  iat → Issued At    │                      │  know what this means.     │
│  jti → JWT ID       │  Anyone receiving    │  Only the issuer does.     │
│                     │  this token can      │                            │
│  These names are    │  understand these    │                            │
│  reserved. Don't    │  fields.             │                            │
│  rename them.       │                      │                            │
└─────────────────────┴──────────────────────┴────────────────────────────┘
```

**Registered Claims explained:**

```
┌──────┬───────────────┬────────────────────────────────────────────────┐
│ Key  │ Full Name     │ What it means                                  │
├──────┼───────────────┼────────────────────────────────────────────────┤
│ iss  │ Issuer        │ Who created/issued this token                  │
│ sub  │ Subject       │ Who this token is about (user identifier)      │
│ aud  │ Audience      │ Who this token is intended for (your app name) │
│ exp  │ Expiry Time   │ Token invalid AFTER this timestamp             │
│ nbf  │ Not Before    │ Token invalid BEFORE this timestamp            │
│ iat  │ Issued At     │ When this token was created                    │
│ jti  │ JWT ID        │ Unique ID for this specific token              │
└──────┴───────────────┴────────────────────────────────────────────────┘
```

**Sample payload:**
```json
{
  "iss": "https://example.com/auth",
  "sub": "1234567890",
  "exp": 1711945909,
  "email": "james@xyz.com",
  "jti": "unique_id_12312312"
}
```

> ⚠️ **NEVER put sensitive information (like passwords) in the payload.** The payload is only encoded (base64), NOT encrypted. Anyone can decode it and read it. More on this in challenges.

---

### Part 3 — Signature

This is the most important part. This is what makes JWT **tamper-proof**. And this is also why JWT and **JWS (JSON Web Signature)** are used interchangeably — because in practice, JWT always has a signature, making it a JWS.

Here's exactly how the signature is built:

```
STEP 1: Base64 encode the Header
        Header JSON  →  base64Encode()  →  "aaaaaaaaaa"

STEP 2: Base64 encode the Payload
        Payload JSON →  base64Encode()  →  "bbbbbbbbbb"

STEP 3: Concatenate with a dot → This is called the "Message"
        "aaaaaaaaaa.bbbbbbbbbb"

STEP 4: Sign the message using a cryptographic algorithm + key
        
        If RSA (asymmetric):
          sign(message, PRIVATE KEY)  →  raw signature bytes

        If HMAC (symmetric):
          sign(message, SECRET KEY)   →  raw signature bytes

STEP 5: Base64 encode the signature
        raw signature  →  base64Encode()  →  "cccccccccc"

STEP 6: Final JWT = Header.Payload.Signature
        "aaaaaaaaaa.bbbbbbbbbb.cccccccccc"
```

**Visually:**

```
┌────────────────────────────────────────────────────────────────┐
│                    SIGNATURE GENERATION                        │
│                                                                │
│  base64(Header) ──┐                                            │
│                   ├──── "message" ──── sign(message, key)      │
│  base64(Payload)──┘         │                │                 │
│                             │           private key (RSA)      │
│                             │         or secret key (HMAC)     │
│                             ▼                │                 │
│                    "aaaaaaa.bbbbbbb"         │                 │
│                                              ▼                 │
│                                     raw signature bytes        │
│                                              │                 │
│                                     base64Encode()             │
│                                              │                 │
│                                              ▼                 │
│                                       "cccccccccc"             │
│                                                                │
│  FINAL JWT = "aaaaaaa.bbbbbbb.cccccccccc"                      │
└────────────────────────────────────────────────────────────────┘
```

**How signature verification works at the receiving end:**

```
┌────────────────────────────────────────────────────────────────┐
│                    SIGNATURE VERIFICATION                      │
│                                                                │
│  Received JWT: "aaaaaaa.bbbbbbb.cccccccccc"                    │
│                                                                │
│  Step 1: Split by "."                                          │
│          encodedHeader  = "aaaaaaa"                            │
│          encodedPayload = "bbbbbbb"                            │
│          receivedSig    = "cccccccccc"                         │
│                                                                │
│  Step 2: Rebuild message = "aaaaaaa.bbbbbbb"                   │
│                                                                │
│  Step 3: Verify signature                                      │
│          RSA  → verify(message, receivedSig, PUBLIC KEY)       │
│          HMAC → verify(message, receivedSig, SAME SECRET KEY)  │
│                                                                │
│  Step 4: If verification passes → data not tampered ✅          │
│          If verification fails  → reject the token  ❌          │
└────────────────────────────────────────────────────────────────┘
```

**Symmetric vs Asymmetric — quick reminder:**

```
┌────────────────────┬───────────────────────────────────────────┐
│  HMAC (Symmetric)  │  RSA (Asymmetric)                         │
├────────────────────┼───────────────────────────────────────────┤
│  One secret key    │  Two keys: private + public               │
│  Same key used     │  Sign with PRIVATE key                    │
│  for sign &        │  Verify with PUBLIC key                   │
│  verify            │                                           │
└────────────────────┴───────────────────────────────────────────┘
```

---

### How the Token is Passed in API Requests

JWT always travels in the **Authorization header** of an HTTP request, with the word `Bearer` before it:

```
curl --location --request GET 'https://exampleHost.com:12345/api/resource' \
--header 'Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...'
```

```
┌───────────────────────────────────────────────────────┐
│             Authorization Header Standards            │
├───────────────┬───────────────────────────────────────┤
│  Basic        │ Passing username:password             │
│               │ base64 encoded                        │
│               │ → server decodes to get credentials   │
├───────────────┼───────────────────────────────────────┤
│  Bearer       │ Passing a TOKEN                       │
│               │ → server knows: "this is a JWT,       │
│               │   I need to validate it"              │
└───────────────┴───────────────────────────────────────┘
```

> **Interview Tip 🎯** — "Basic" in Authorization header = username/password (base64 encoded). "Bearer" = token. Never confuse the two. JWT is always sent as `Bearer <token>`.

---
# JWT (JSON Web Token) — Complete Notes

## Step 3 of 5 — JWT Advantages (Now They'll Actually Make Sense)

---

Shreyansh deliberately held back the advantages section until after explaining the structure. Now that you know what JWT looks like inside, each advantage will click naturally.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         JWT ADVANTAGES                                  │
├─────────────────────────┬───────────────────────────────────────────────┤
│  ADVANTAGE              │  WHY IT MAKES SENSE NOW                       │
├─────────────────────────┼───────────────────────────────────────────────┤
│  1. Compact             │  Three base64 encoded strings joined by dots. │
│                         │  Small enough to fit inside an HTTP header.   │
│                         │  Fast to transmit over the network.           │
├─────────────────────────┼───────────────────────────────────────────────┤
│  2. Stateless /         │  Payload already carries everything: user ID, │
│     Self-Contained      │  roles, expiry. No DB call needed on every    │
│                         │  request. Server just reads the token.        │
├─────────────────────────┼───────────────────────────────────────────────┤
│  3. Digitally Signed    │  Signature part (JWS) ensures nobody tampered │
│                         │  with the payload. Verified using RSA or HMAC │
│                         │  at the receiver end.                         │
├─────────────────────────┼───────────────────────────────────────────────┤
│  4. Built-in Expiry     │  "exp" claim is right there in the payload.   │
│                         │  No DB needed to check if token has expired.  │
│                         │  Auth server just reads "exp" and decides.    │
├─────────────────────────┼───────────────────────────────────────────────┤
│  5. Custom Claims       │  You can add roles, email, country, anything  │
│                         │  into the payload. All travels with the token.│
│                         │  No separate DB lookup for user roles.        │
├─────────────────────────┼───────────────────────────────────────────────┤
│  6. Third-party ready   │  You don't have to write auth logic yourself. │
│                         │  Many battle-tested libraries exist that      │
│                         │  handle token generation & validation for you.│
└─────────────────────────┴───────────────────────────────────────────────┘
```

---

### Session ID vs JWT — The Full Comparison

Since Shreyansh taught Session ID as the "before JWT" context, here's the full side-by-side picture:

```
┌──────────────────────────┬────────────────────────────────────────────┐
│     SESSION ID           │              JWT                           │
├──────────────────────────┼────────────────────────────────────────────┤
│ Stateful                 │ Stateless                                  │
│ State lives in DB        │ State lives inside the token itself        │
├──────────────────────────┼────────────────────────────────────────────┤
│ DB hit on every request  │ No DB hit needed during validation         │
├──────────────────────────┼────────────────────────────────────────────┤
│ Distributed systems      │ Works seamlessly across distributed        │
│ need DB sync across      │ systems — every server validates           │
│ all clusters             │ independently using the token              │
├──────────────────────────┼────────────────────────────────────────────┤
│ Just a random key,       │ Self-contained — carries user info,        │
│ no info inside it        │ roles, expiry all within itself            │
├──────────────────────────┼────────────────────────────────────────────┤
│ Easy to invalidate       │ Hard to invalidate before expiry           │
│ (just delete from DB)    │ (biggest challenge with JWT)               │
└──────────────────────────┴────────────────────────────────────────────┘
```

---

### How SSO (Single Sign On) Works With JWT

This is a very direct interview question. Here's the complete picture:

```
                        AUTH SERVER
                             │
    ┌───── login once ───────┤
    │     (user + pass)      │
    │                        │── generates JWT
    │                        │
    │◄──── JWT token ────────┘
    │
  CLIENT (has JWT now)
    │
    ├──── Bearer <JWT> ──────► APP 1 (f1.com)
    │                              │
    │                              │── verify token with Auth Server
    │                              │◄─ "valid" → signs you in
    │                              │   (reads name, email from payload)
    │
    ├──── Bearer <JWT> ──────► APP 2 (f2.com)
    │                              │── same verification → signs you in
    │
    └──── Bearer <JWT> ──────► APP 3 (f3.com)
                                   │── same verification → signs you in
```

One login. One token. Multiple apps. That's SSO via JWT.

> **Interview Tip 🎯** — Whenever SSO is asked, the answer involves JWT. Key point: the payload already carries user info (name, email from public claims), so each app can identify and sign in the user without asking for credentials again.

---
# JWT (JSON Web Token) — Complete Notes

## Step 4 of 5 — Challenges with JWT (Most Interview-Heavy Section)

---

### Challenge 1 — Token Invalidation

This is the **biggest problem** with JWT. Let's build up to it properly.

Because JWT is stateless, your application server has **zero knowledge** that a particular token even exists. It was issued to a user. When it arrives, the server just checks: is it valid? is it expired? If yes to both — access granted.

Now imagine this situation:

```
┌─────────────────────────────────────────────────────────────┐
│                   THE FRAUD USER PROBLEM                    │
│                                                             │
│  Auth Server issued a JWT to a user.                        │
│  Expiry: valid till 21st April.                             │
│  Today: 10th April.                                         │
│                                                             │
│  You discover → this user is FRAUD.                         │
│  You want to BLOCK him immediately.                         │
│                                                             │
│  But the token is still technically valid.                  │
│  How do you stop him?                                       │
│                                                             │
│  JWT has no built-in answer for this. ❌                    │
└─────────────────────────────────────────────────────────────┘
```

There are **four solutions** — each with trade-offs:

---

**Solution A — Blacklist the token**

```
AUTH SERVER
    │
    ├── maintains a BLACKLISTED TOKENS list (in DB or Cache)
    │   e.g.:  jti: "unique_token_id_xyz" → BLOCKED
    │
    │
FRAUD USER sends JWT
    │
    ▼
Resource Server → passes token to Auth Server for validation
    │
Auth Server checks:
    ├── Is token valid? → Yes
    ├── Is it expired?  → No
    └── Is jti in blacklist? → YES → REJECT ❌
```

**Problem with this:** You're back to doing a DB/cache lookup on every request — the exact problem you were trying to escape from with Session ID.

---

**Solution B — Change the Secret Key**

```
Earlier:  tokens signed with  PRIVATE KEY 1
Now:      rotate to           PRIVATE KEY 2

Verification now uses PUBLIC KEY 2.

Fraud user's token was signed with KEY 1
→ verification with KEY 2 fails → REJECTED ✅

BUT... every genuine user's token was also signed with KEY 1
→ they all get rejected too ❌
→ everyone has to log in again
```

Solves the problem, but **punishes all genuine users** too.

---

**Solution C — Keep Tokens Very Short-Lived**

```
Instead of: valid till April 21st (days/weeks)
Use:        valid for only 5 minutes

Fraud user gets blocked within 5 minutes max.
After expiry, token is useless anyway.
```

Problem: the fraud window still exists (up to 5 minutes). But this is still **the most popular approach** in the industry.

---

**Solution D — Token Used Only Once**

```
Each token: valid for ONE use only.
After one use → invalidated.

How to track "already used"?
→ Store the jti (unique JWT ID) in cache
→ On each request, check: have I seen this jti before?
→ If yes → REJECT
→ If no  → ALLOW, then mark it as seen
```

Problem: again requires cache/DB lookup per request.

---

**What's actually used in practice:**

```
┌─────────────────────────────────────────────────────────────┐
│           MOST COMMON REAL-WORLD COMBINATION                │
│                                                             │
│   Short-lived token (5-10 mins)                             │
│         +                                                   │
│   Token used only once                                      │
│                                                             │
│   After expiry → user must re-authenticate                  │
│   (or use a Refresh Token flow — covered in OAuth)          │
└─────────────────────────────────────────────────────────────┘
```

> **Interview Tip 🎯** — "How do you invalidate a JWT before expiry?" is a very common question. Walk through all four solutions and their trade-offs. Mention that short-lived tokens are the most popular production choice. Bonus: mention `jti` claim is what makes token-level tracking possible.

---

### Challenge 2 — JWT is Encoded, NOT Encrypted

This is a security trap that many developers fall into.

```
┌──────────────────────────────────────────────────────────────┐
│                  ENCODED vs ENCRYPTED                        │
├───────────────────────┬──────────────────────────────────────┤
│  Base64 ENCODING      │  ENCRYPTION                          │
├───────────────────────┼──────────────────────────────────────┤
│  Anyone can decode it │  Only someone with the key           │
│  No key needed        │  can decrypt it                      │
│                       │                                      │
│  It's just a          │  It's actually hidden/               │
│  representation       │  protected data                      │
│  change               │                                      │
└───────────────────────┴──────────────────────────────────────┘

JWT Payload is base64 encoded → anyone can decode and READ it.
```

So if you put sensitive data (passwords, secrets) in the payload — anyone who intercepts the token can read it.

**Solution → Use JWE (JSON Web Encryption)**

```
JWT  (what we've been studying)
  └── Payload is base64 ENCODED  → readable by anyone

JWE  (JSON Web Encryption)
  └── Payload is ENCRYPTED       → only auth server can decrypt

Flow with JWE:
  Auth server encrypts payload using PUBLIC KEY (RSA)
  Resource server sends token to auth server for validation
  Auth server decrypts using PRIVATE KEY
  Reads claims, validates, responds
```

> **Interview Tip 🎯** — "Is JWT secure?" → It is tamper-proof (signature), but NOT confidential (payload is just encoded). If you need confidentiality, use JWE which encrypts the payload.

---

### Challenge 3 — Unsecured JWT (algo: none)

Remember the header has an `alg` field that says which algorithm was used to sign the token.

```
NORMAL JWT header:
{
  "typ": "JWT",
  "alg": "RSA"      ← signed with RSA, signature present
}

UNSECURED JWT header:
{
  "typ": "JWT",
  "alg": "none"     ← no signing, no signature part
}
```

```
┌─────────────────────────────────────────────────────────┐
│                  UNSECURED JWT                          │
│                                                         │
│  Structure:  Header . Payload . (empty)                 │
│                                                         │
│  No signature = no tamper protection                    │
│  Anyone can modify the payload freely                   │
│                                                         │
│  RULE: Any JWT with alg = "none" must be                │
│        REJECTED immediately. No exceptions. ❌           │
└─────────────────────────────────────────────────────────┘
```

> **Interview Tip 🎯** — If asked "what is an unsecured JWT?" → it's a JWT with `alg: none` and no signature. It should always be rejected. This is also a known attack vector where attackers try to send tokens with `alg: none` hoping the server accepts them.

---

### Challenge 4 — JWK Exploit (Public Key Injection Attack)

This is the most sophisticated challenge. Read carefully.

First, understand what `jwk` is in the header:

```
JWT Header with JWK:
{
  "typ": "JWT",
  "alg": "RSA",
  "jwk": {
    "n": "sfsdf234324fsd4sfdsfsdf23",   ← modulus   (part of public key)
    "e": "ABC4ED",                       ← exponent  (part of public key)
    "kid": "sdfds3432432432fwfdwfsfwf"
  }
}
```

`n` + `e` together **form the public key** in RSA.

**The Attack:**

```
ATTACKER'S PLAN:
─────────────────
Step 1: Take a legitimate JWT token

Step 2: Modify the payload
        (add extra roles, change user ID, whatever)

Step 3: Sign this modified token with ATTACKER'S OWN private key

Step 4: Put ATTACKER'S OWN public key inside the "jwk" field of header

Step 5: Send this forged token to resource server

        Resource server reads "jwk" from header
        Uses that public key to verify signature
        → Verification PASSES ✅ (because attacker signed with matching key)
        → Attacker gains unauthorized access 🚨
```

**Why this works if server is naive:**

```
┌────────────────────────────────────────────────────────────┐
│                    THE TRAP                                │
│                                                            │
│  Server uses public key FROM INSIDE the token header       │
│  to verify the token's own signature.                      │
│                                                            │
│  This is like trusting a stranger who says:                │
│  "Verify my ID using the photo I just gave you."           │
│  Of course it'll match — he provided the photo himself.    │
└────────────────────────────────────────────────────────────┘
```

**The Fix — Use `kid` to look up whitelisted public keys:**

```
AUTH SERVER maintains a well-known public endpoint:
  https://{auth-server-domain}/.well-known/jwks.json

This file contains a LIST of trusted/whitelisted public keys:
  {
    "keys": [
      { "kid": "key-id-1",  "n": "...", "e": "..." },
      { "kid": "key-id-2",  "n": "...", "e": "..." }
    ]
  }

CORRECT VERIFICATION FLOW:
───────────────────────────
  Token arrives with kid = "key-id-1" in header

  Auth server:
    1. Reads kid from token header
    2. Goes to ITS OWN jwks.json (not from the token's jwk field)
    3. Finds the public key matching kid = "key-id-1"
    4. Uses THAT trusted public key to verify signature

  Attacker's public key is NOT in this whitelisted list
  → Verification FAILS → Access DENIED ✅
```

```
┌──────────────────────────────────────────────────────────────┐
│                    JWK EXPLOIT SUMMARY                       │
├──────────────────────────────────────────────────────────────┤
│  WRONG: Use public key from inside token's "jwk" field       │
│  RIGHT: Use kid to look up public key from auth server's     │
│         own trusted /.well-known/jwks.json list              │
└──────────────────────────────────────────────────────────────┘
```

> **Interview Tip 🎯** — "What is the JWK exploit?" → Attacker embeds their own public key inside the JWT header's `jwk` field. If the server naively uses that key to verify, the forged token passes. Fix: always use `kid` to look up keys from the auth server's own whitelisted `jwks.json`, never trust the key embedded in the token itself.

---

### All 4 Challenges — Quick Reference

```
┌────┬──────────────────────────┬─────────────────────────────────────────┐
│ #  │  Challenge               │  Solution                               │
├────┼──────────────────────────┼─────────────────────────────────────────┤
│ 1  │ Token Invalidation       │ Blacklist / rotate key / short-lived /  │
│    │                          │ single-use (most popular: short-lived)  │
├────┼──────────────────────────┼─────────────────────────────────────────┤
│ 2  │ Payload readable by all  │ Use JWE (encrypt the payload)           │
│    │ (encoded ≠ encrypted)    │                                         │
├────┼──────────────────────────┼─────────────────────────────────────────┤
│ 3  │ Unsecured JWT (alg:none) │ Always reject tokens with alg = "none"  │
├────┼──────────────────────────┼─────────────────────────────────────────┤
│ 4  │ JWK exploit              │ Never use public key from token's own   │
│    │ (public key injection)   │ jwk field. Use kid + jwks.json instead. │
└────┴──────────────────────────┴─────────────────────────────────────────┘
```

---
# JWT (JSON Web Token) — Complete Notes

## Step 5 of 5 — JWT vs JWS vs JWE + Full Recap

---

### The Terminology Confusion — Cleared Once and For All

You'll often hear people say "JWT" when they actually mean different things. Here's the full picture:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    THE JWT FAMILY                                       │
├──────────────────┬──────────────────────────┬───────────────────────────┤
│   JWT            │   JWS                    │   JWE                     │
│ (JSON Web Token) │ (JSON Web Signature)     │ (JSON Web Encryption)     │
├──────────────────┼──────────────────────────┼───────────────────────────┤
│ The BASE concept │ JWT + Signature          │ JWT + Encrypted Payload   │
│                  │                          │                           │
│ Just a format    │ What everyone actually   │ Used when payload         │
│ for transmitting │ uses in production.      │ confidentiality matters.  │
│ JSON securely.   │ Payload is encoded.      │ Payload is encrypted.     │
│                  │ Signature verifies       │                           │
│ On its own       │ tamper-proofing.         │ Only auth server can      │
│ (alg: none) →    │                          │ decrypt and read it.      │
│ Unsecured JWT.   │ This is what people      │                           │
│ Always reject.   │ mean when they say JWT.  │                           │
└──────────────────┴──────────────────────────┴───────────────────────────┘
```

**Why JWT and JWS are used interchangeably:**

```
In theory:
  JWT (alg: none)  →  no signature  →  unsecured  →  never used
  JWT (alg: RSA)   →  has signature →  this is JWS

In practice:
  Nobody uses JWT without a signature.
  So "JWT" in daily conversation always means "JWT with signature" = JWS.
  That's why the terms are interchangeable.
```

---

### Structure Comparison — JWT vs JWS vs JWE

```
┌─────────────────────────────────────────────────────────────────────────┐
│  JWT (Unsecured)                                                        │
│                                                                         │
│  base64(Header) . base64(Payload) .  (empty)                            │
│       │                │                                                │
│  alg: none        user info etc.    No signature at all                 │
│                                     ← ALWAYS REJECT                     │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│  JWS (What everyone calls "JWT" in production)                          │
│                                                                         │
│  base64(Header) . base64(Payload) . base64(Signature)                   │
│       │                │                    │                           │
│  alg: RSA/HMAC    user info etc.    sign(header.payload, key)           │
│                   ENCODED only      Tamper-proof ✅                      │
│                   Readable by all   Payload NOT confidential ⚠️         │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│  JWE (When you need payload confidentiality)                            │
│                                                                         │
│  base64(Header) . base64(EncryptedPayload) . base64(Signature)          │
│       │                    │                         │                  │
│  alg: RSA/HMAC    ENCRYPTED with public key   Tamper-proof ✅            │
│                   Only auth server can        Payload confidential ✅    │
│                   decrypt with private key                              │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### The Complete JWT Flow — Everything Together

Now that you know every piece, here's the full end-to-end picture:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     COMPLETE JWT FLOW                                   │
└─────────────────────────────────────────────────────────────────────────┘

CLIENT                    AUTH SERVER                   RESOURCE SERVER
  │                            │                               │
  │── 1. login (user+pass) ───►│                               │
  │                            │                               │
  │                            │  Build token:                 │
  │                            │  Header  = {typ, alg, kid}    │
  │                            │  Payload = {iss, sub, exp,    │
  │                            │             email, roles...}  │
  │                            │  Sign    = sign(H.P, privKey) │
  │                            │  JWT     = b64(H).b64(P).     │
  │                            │            b64(Sign)          │
  │                            │                               │
  │◄── 2. returns JWT ─────────│                               │
  │                            │                               │
  │                                                            │
  │── 3. GET /resource ────────────────────────────────────────►
  │      Authorization: Bearer <JWT>                           │
  │                                                            │
  │                            │◄── 4. verify token ───────────│
  │                            │       (passes JWT)            │
  │                            │                               │
  │                            │  Validate:                    │
  │                            │  ① split Header.Payload.Sig   │
  │                            │  ② read kid from header       │
  │                            │  ③ look up jwks.json for key  │
  │                            │  ④ verify signature           │
  │                            │  ⑤ check exp (not expired?)   │
  │                            │  ⑥ check alg ≠ none           │
  │                            │  ⑦ check jti not blacklisted  │
  │                            │                                │
  │                            │── 5. "valid!" ───────────────► │
  │                                                             │
  │◄── 6. returns requested data ────────────────────────────── │
```

---

### Master Cheat Sheet — Everything in One Place

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        JWT MASTER CHEAT SHEET                           │
├─────────────────────────────────────────────────────────────────────────┤
│  WHAT IS JWT                                                            │
│  Secure way to transmit JSON information between parties.               │
│  Digitally signed → tamper-proof.                                       │
│  Used for: Authentication, Authorization, SSO.                          │
├─────────────────────────────────────────────────────────────────────────┤
│  STRUCTURE     Header . Payload . Signature                             │
│                                                                         │
│  Header:    typ (JWT), alg (RSA/HMAC), kid (key ID)                     │
│  Payload:   Registered + Public + Private claims                        │
│             iss, sub, aud, exp, nbf, iat, jti + custom fields           │
│  Signature: sign(base64(Header).base64(Payload), key)                   │
├─────────────────────────────────────────────────────────────────────────┤
│  SESSION ID vs JWT                                                      │
│  Session ID → stateful, DB hit every request, bad for distributed.      │
│  JWT        → stateless, self-contained, no DB hit, scales easily.      │
├─────────────────────────────────────────────────────────────────────────┤
│  ADVANTAGES                                                             │
│  Compact, Stateless, Self-Contained, Digitally Signed,                  │
│  Built-in Expiry, Custom Claims, Third-party library support.           │
├─────────────────────────────────────────────────────────────────────────┤
│  CHALLENGES                                                             │
│  1. Token Invalidation  → blacklist / key rotation /                    │
│                           short-lived / single-use                      │
│  2. Encoded ≠ Encrypted → use JWE for payload confidentiality           │
│  3. alg: none           → always reject unsecured JWT                   │
│  4. JWK exploit         → never trust public key from token's           │
│                           own jwk field, use kid + jwks.json            │
├─────────────────────────────────────────────────────────────────────────┤
│  TERMINOLOGY                                                            │
│  JWT  = base concept (unsecured if no signature)                        │
│  JWS  = JWT + Signature (what everyone calls "JWT" in practice)         │
│  JWE  = JWT + Encrypted Payload (for confidential data)                 │
├─────────────────────────────────────────────────────────────────────────┤
│  HOW TOKEN IS SENT                                                      │
│  Authorization: Bearer <token>                                          │
│  Basic  = username:password (base64)                                    │
│  Bearer = JWT token                                                     │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### All Interview Tips — Consolidated

> **🎯 What is JWT?** → Secure way to transmit JSON between parties. Digitally signed (JWS). Used for Auth, Authorization, SSO.

> **🎯 JWT vs Session ID?** → Session ID is stateful (DB hit every request, bad for distributed systems). JWT is stateless (all info self-contained in the token).

> **🎯 How does SSO work?** → Login once → get JWT → pass same token to multiple apps → each app validates with auth server → no re-login needed.

> **🎯 Is JWT encrypted?** → No. Payload is base64 encoded — anyone can decode and read it. Don't put sensitive data in payload. For encryption, use JWE.

> **🎯 How to invalidate JWT before expiry?** → Four ways: blacklist the jti, rotate secret key (affects all users), keep tokens short-lived (most popular), use token only once.

> **🎯 What is unsecured JWT?** → JWT with `alg: none`, no signature. Always reject. Known attack vector.

> **🎯 What is the JWK exploit?** → Attacker puts own public key in token's `jwk` header field. Server naively uses it to verify → passes forged token. Fix: use `kid` to look up key from auth server's own `jwks.json`, never from the token itself.

> **🎯 JWT vs JWS vs JWE?** → JWT is the base concept. JWS adds signature (tamper-proof). JWE encrypts the payload (confidential). In practice, "JWT" always means JWS.

---

That wraps up the complete JWT lecture. The next natural topic in Spring Security would be implementing JWT in a Spring Boot application — filters, security config, token generation and validation code. Let me know when you're ready!