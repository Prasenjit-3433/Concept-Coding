# Step 1 — JWT Recap + Why No Default Implementation + Architecture Deep Dive

---

## 1.1 What is JWT? (Quick Recap)

JWT (JSON Web Token) is a **stateless authentication** method. "Stateless" means the server does **not** maintain any session for the user. Every request must carry proof of identity by itself — and that proof is the JWT token.

A JWT token has **3 parts**, separated by dots:

```
HEADER . PAYLOAD . SIGNATURE
```

| Part | What it contains | Purpose |
|---|---|---|
| **Header** | Algorithm used (e.g. HMAC, RSA), token type | Metadata |
| **Payload** | User data — userId, username, role, expiry time (also called "claims") | Application-specific data |
| **Signature** | Header + Payload signed with a secret key | Ensures nobody tampered with the token |

**Why does the Signature matter?**
If someone changes the role in the payload from `ROLE_USER` → `ROLE_ADMIN`, when the server recalculates the signature, it won't match the original. So the token gets rejected. This is the **integrity guarantee**.

All three parts are Base64 encoded. A real token looks like:

```
eyJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6InNqIn0.abc123signature
   [HEADER]              [PAYLOAD]                [SIGNATURE]
```

---

## 1.2 The 4 Steps We Will Implement

The instructor lays out a clear roadmap before writing a single line of code:

```
┌─────────────────────────────────────────────────────────┐
│                  JWT Implementation Roadmap             │
├────────┬────────────────────────────────────────────────┤
│ Step 1 │ User Creation  → /api/user-register            │
│ Step 2 │ Token Generation → /generate-token             │
│ Step 3 │ Token Validation → any restricted API          │
│ Step 4 │ Refresh Token  → /refresh-token                │
└────────┴────────────────────────────────────────────────┘
```

---

## 1.3 Why Spring Boot Has NO Default JWT Implementation

This is something the instructor stresses very hard — and it's an important thing to understand before you touch any code.

For **Form-based** and **Basic Authentication**, Spring Boot gives you everything out of the box. You just configure it and the framework handles the rest.

**But for JWT — there is nothing pre-built.** Why?

Because JWT implementation varies heavily from application to application:

| Decision | Varies by App |
|---|---|
| What goes in Payload? | Some need just username, some need userId, role, email, etc. |
| Which signing algorithm? | Some want HMAC (symmetric), some want RSA (asymmetric) |
| Refresh token strategy? | Some apps don't even need it |
| Token expiry time? | Completely up to you |

So Spring Boot says: *"I give you the framework — you build on top of it."*

> 💡 **This is why you'll see different JWT implementations across the internet. No one solution is wrong — because there is no one official way. Don't get confused when you see someone else's code look different from this.**

---

## 1.4 The Architecture — The Most Important Part

The instructor says: *"Don't skip this. Even if you think you know the architecture, there's one specific behavior I'm going to point out that is the KEY to understanding how we will plug JWT into the framework."*

Here is the full Spring Security request flow:

```
Incoming Request
      │
      ▼
┌─────────────────────────────────────────────────────────────┐
│                    Filter Chain                             │
│                                                             │
│  ┌──────────────────┐                                       │
│  │  Security Filter │  ← Creates Authentication Object      │
│  │  (e.g. Form /    │    with data from request             │
│  │   Basic / JWT)   │    (username+password / token / etc.) │
│  └────────┬─────────┘                                       │
│           │  Authentication Object (isAuthenticated=false)  │
│           ▼                                                 │
│  ┌──────────────────┐                                       │
│  │ Authentication   │  ← ProviderManager (implementation)   │
│  │ Manager          │                                       │
│  └────────┬─────────┘                                       │
│           │  Iterates over list of AuthenticationProviders  │
│           │  Calls support() on each one                    │
│           ▼                                                 │
│  ┌──────────────────────────────────────────┐               │
│  │         Authentication Providers         │               │
│  │                                          │               │
│  │  ┌─────────────────────────────────┐     │               │
│  │  │ DaoAuthenticationProvider       │     │               │
│  │  │ supports: UsernamePasswordToken │     │               │
│  │  └─────────────────────────────────┘     │               │
│  │                                          │               │
│  │  ┌─────────────────────────────────┐     │               │
│  │  │ AuthenticationProvider N        │     │               │
│  │  │ supports: some other token      │     │               │
│  │  └─────────────────────────────────┘     │               │
│  └──────────────────────────────────────────┘               │
│           │                                                 │
│           │  Fully authenticated object returned            │
│           ▼                                                 │
│  ┌──────────────────┐                                       │
│  │ SecurityContext  │  ← Stores the authenticated object    │
│  │ Holder           │                                       │
│  └──────────────────┘                                       │
└─────────────────────────────────────────────────────────────┘
      │
      ▼
  Controller
```

---

## 1.5 The ONE Behavior You Must Notice

The instructor highlights this very specifically:

**The Security Filter creates the Authentication object — but it does NOT know which provider can handle it.**

**The Authentication Manager (ProviderManager) holds a list of providers. It calls `supports()` on each one and delegates to whoever says YES.**

This is the exact mechanism we will exploit to plug in JWT. Here is the actual framework code the instructor shows:

```java
// ProviderManager.java (Spring Framework Source Code)
@Override
public Authentication authenticate(Authentication authentication)
        throws AuthenticationException {

    Class<? extends Authentication> toTest = authentication.getClass();

    // Iterates over ALL registered providers
    for (AuthenticationProvider provider : getProviders()) {

        // Calls support() — checks if this provider understands
        // the incoming Authentication object type
        if (!provider.supports(toTest)) {
            continue; // Not my job — skip
        }

        // If yes → call its authenticate() method
        result = provider.authenticate(authentication);
    }
}
```

And here is how `DaoAuthenticationProvider` declares what it supports:

```java
// DaoAuthenticationProvider.java (Spring Framework Source Code)
@Override
public boolean supports(Class<?> authentication) {
    return (UsernamePasswordAuthenticationToken.class
                .isAssignableFrom(authentication));
}
// Translation: "I only handle UsernamePasswordAuthenticationToken objects"
```

---

## 1.6 How We Will Plug JWT Into This Framework

Now the instructor connects everything. Here is the plan — this is the blueprint for everything we code in the next steps:

```
┌────────────────────────────────────────────────────────────────────┐
│                  What We Need to Build for JWT                     │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  1. Custom Security Filter                                         │
│     → Reads JWT token from the request                             │
│     → Creates a Custom Authentication Object                       │
│     → Passes it to Authentication Manager                          │
│     → Must be added to the Filter Chain at the right position      │
│                                                                    │
│  2. Custom Authentication Object (JwtAuthenticationToken)          │
│     → Holds the JWT token string                                   │
│     → isAuthenticated = false initially                            │
│                                                                    │
│  3. Custom Authentication Provider (JWTAuthenticationProvider)     │
│     → support() returns true for JwtAuthenticationToken            │
│     → authenticate() validates the token, loads user from DB       │
│     → Returns fully authenticated object                           │
│                                                                    │
│  4. Register Custom Provider in ProviderManager's list             │
│     → So it gets picked up during the support() iteration          │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

Visually, here's how the enhanced flow looks:

```
Incoming Request (with JWT token)
      │
      ▼
┌──────────────────────┐
│  Custom JWT Filter   │  ← NEW (we write this)
│  Reads token from    │
│  Authorization header│
│  Creates:            │
│  JwtAuthToken(token) │
└────────┬─────────────┘
         │
         ▼
┌──────────────────────┐
│  Authentication      │
│  Manager             │
│  (ProviderManager)   │
└────────┬─────────────┘
         │ calls support() on each provider
         ▼
┌─────────────────────────────────────┐
│  Provider List                      │
│                                     │
│  DaoAuthenticationProvider          │
│  → support(JwtAuthToken)? ❌ NO      │
│                                     │
│  JWTAuthenticationProvider          │  ← NEW (we write this)
│  → support(JwtAuthToken)? ✅ YES     │
│  → authenticate() → validate token  │
│  → load user from DB                │
│  → return fully authenticated obj   │
└─────────────────────────────────────┘
         │
         ▼
┌──────────────────────┐
│  SecurityContext     │
│  Holder              │
│  (stores auth obj)   │
└──────────────────────┘
         │
         ▼
     Controller
```

---

## Summary of Step 1

| Concept | Key Takeaway |
|---|---|
| JWT is stateless | No session on server side |
| 3 parts: Header, Payload, Signature | Signature prevents tampering |
| No default JWT in Spring Boot | Because requirements vary per app |
| Filter creates Auth object | But doesn't know which provider handles it |
| ProviderManager calls support() | Delegates to correct provider |
| Our plan | Custom Filter + Custom Auth Object + Custom Provider |

---
# Step 2 — Token Generation (`/generate-token`)

---

## 2.1 What Are We Building Here?

Before writing any code, let's be crystal clear on what this step does:

```
┌─────────────────────────────────────────────────────────────┐
│                   Token Generation Flow                     │
│                                                             │
│  Client sends:                                              │
│  POST /generate-token                                       │
│  Body: { "username": "sj", "password": "123" }              │
│                 │                                           │
│                 ▼                                           │
│  Server validates username+password against DB              │
│                 │                                           │
│        ┌────────┴────────┐                                  │
│     ✅ Match          ❌ No Match                             │
│        │                 │                                  │
│        ▼                 ▼                                  │
│  Generate JWT Token   Throw Exception                       │
│  Return in Header                                           │
│  Authorization: Bearer <token>                              │
└─────────────────────────────────────────────────────────────┘
```

> 💡 **Important point from instructor:** We are NOT creating a controller for `/generate-token`. We are handling this entirely inside the Security Filter itself. This keeps security logic inside the security framework — where it belongs.

---

## 2.2 First — Add JWT Dependencies

Before writing any class, add these 3 dependencies to `pom.xml`:

```xml
<!-- JWT API — provides the interfaces -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>

<!-- JWT Implementation — actual implementation of the API -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- Required for JSON processing of the payload (key-value pairs) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
</dependency>
```

**Why 3 dependencies?**

```
┌─────────────────────────────────────────────────────┐
│  jjwt-api      → Interfaces only (what to do)       │
│  jjwt-impl     → Actual implementation (how to do)  │
│  jjwt-jackson  → JSON parsing of payload            │
│                  (payload is key-value = JSON)      │
└─────────────────────────────────────────────────────┘
```

---

## 2.3 The Full Picture Before Coding

Here is the complete flow we are implementing in this step, with every class involved:

```
POST /generate-token
{ username, password }
        │
        ▼
┌───────────────────────────────┐
│   JWTAuthenticationFilter     │  ← Custom Filter (we write)
│   extends OncePerRequestFilter│
│                               │
│   1. Check if path =          │
│      /generate-token          │
│   2. Read username+password   │
│      from request body        │
│      (LoginRequest object)    │
│   3. Create:                  │
│      UsernamePasswordAuth     │
│      enticationToken          │
│   4. Pass to                  │
│      AuthenticationManager    │
└──────────────┬────────────────┘
               │
               ▼
┌───────────────────────────────┐
│   AuthenticationManager       │
│   (ProviderManager)           │
│                               │
│   Iterates providers...       │
│   calls support()...          │
└──────────────┬────────────────┘
               │
               ▼
┌───────────────────────────────┐
│   DaoAuthenticationProvider   │  ← Already exists in framework
│                               │
│   support(                    │
│   UsernamePasswordToken)✅YES  │
│                               │
│   1. Hash the incoming pwd    │
│   2. Load user from DB via    │
│      UserDetailsService       │
│   3. Compare hashes           │
│   4. Return fully             │
│      authenticated object     │
└──────────────┬────────────────┘
               │
               ▼
┌───────────────────────────────┐
│  Back in JWTAuthFilter        │
│                               │
│  authResult.isAuthenticated() │
│  = true                       │
│                               │
│  Call JWTUtil.generateToken() │
│  Put token in response header │
│  Authorization: Bearer <token>│
│                               │
│  NO filterChain.doFilter()    │
│  (don't go to controller)     │
└───────────────────────────────┘
```

> 💡 **Why do we use `UsernamePasswordAuthenticationToken` here and not a custom token?**
> Because `DaoAuthenticationProvider` already knows how to handle `UsernamePasswordAuthenticationToken`. It already does password hashing + DB lookup + comparison. We reuse this existing functionality instead of reinventing it!

---

## 2.4 Code — Step by Step

### piece 1: `LoginRequest.java`

This is a simple POJO. The user sends username and password in the request body — we need an object to capture that.

```java
public class LoginRequest {

    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
```

---

### Piece 2: `JWTUtil.java`

This is the utility class responsible for **generating** the JWT token. It uses the `jjwt` library we added.

```java
@Component
public class JWTUtil {

    // ⚠️ In production, NEVER hardcode this.
    // Store it in environment variables or a secrets manager.
    // This is just for demo purposes.
    private static final String SECRET_KEY = "your-secure-secret-key-min-32bytes";

    // Create the signing key using HMAC-SHA
    // HMAC = symmetric key → same key used to sign AND verify
    private static final Key key =
        Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    // Generate JWT Token
    public String generateToken(String username, long expiryMinutes) {
        return Jwts.builder()
            .setSubject(username)               // payload: who this token is for
            .setIssuedAt(new Date())            // payload: when was it issued
            .setExpiration(new Date(
                System.currentTimeMillis()
                + expiryMinutes * 60 * 1000))   // payload: when does it expire
            .signWith(key, SignatureAlgorithm.HS256) // sign with our secret key
            .compact();                         // build the final token string
    }
}
```

**What goes inside the token (Payload)?**

```
┌──────────────────────────────────────┐
│           JWT Payload (Claims)       │
├──────────────────────────────────────┤
│  sub  (subject)   → username         │
│  iat  (issued at) → current time     │
│  exp  (expiry)    → now + 15 minutes │
└──────────────────────────────────────┘
```

**About the signing algorithm:**

```
┌───────────────────────────────────────────────────┐
│  HMAC (HS256) = Symmetric Key                     │
│                                                   │
│  Same secret key used to:                         │
│  → SIGN the token when generating                 │
│  → VERIFY the token when validating               │
│                                                   │
│  Both server operations use the same key          │
│  (unlike RSA which has public/private key pair)   │
└───────────────────────────────────────────────────┘
```

---

### Piece 3: `JWTAuthenticationFilter.java`

This is the most important class in this step. This filter intercepts the `/generate-token` request and handles everything.

```java
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;

    // Constructor — we pass these in from SecurityConfig
    public JWTAuthenticationFilter(AuthenticationManager authenticationManager,
                                   JWTUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // STEP 1: Is this request for /generate-token?
        // If NOT → pass to next filter, this filter has nothing to do
        if (!request.getServletPath().equals("/generate-token")) {
            filterChain.doFilter(request, response);
            return;
        }

        // STEP 2: Read username + password from request body
        ObjectMapper objectMapper = new ObjectMapper();
        LoginRequest loginRequest =
            objectMapper.readValue(request.getInputStream(), LoginRequest.class);

        // STEP 3: Create UsernamePasswordAuthenticationToken
        // Why this specific token? → DaoAuthenticationProvider supports it!
        // So we intentionally use this so Dao handles the DB matching for us.
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            );

        // STEP 4: Pass to AuthenticationManager
        // → ProviderManager iterates providers
        // → Dao says YES I support this
        // → Dao hashes password, loads from DB, compares, returns result
        Authentication authResult =
            authenticationManager.authenticate(authToken);

        // STEP 5: If authenticated → generate JWT and put in response header
        if (authResult.isAuthenticated()) {
            String token = jwtUtil.generateToken(authResult.getName(), 15); // 15 min
            response.setHeader("Authorization", "Bearer " + token);
        }

        // STEP 6: Do NOT call filterChain.doFilter()
        // We don't want to go to the controller.
        // Response is already set. We're done here.
    }
}
```

**Why `OncePerRequestFilter`?**

```
┌──────────────────────────────────────────────────────┐
│  OncePerRequestFilter                                │
│                                                      │
│  Guarantees that this filter runs ONLY ONCE          │
│  per request — even if the filter chain somehow      │
│  loops back to it internally.                        │
│                                                      │
│  All our custom JWT filters extend this.             │
└──────────────────────────────────────────────────────┘
```

---

### Piece 4: `SecurityConfig.java`

This is where everything gets wired together. Read every line carefully.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private JWTUtil jwtUtil;
    private UserDetailsService userDetailsService;

    @Autowired
    public SecurityConfig(JWTUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    // --- Bean: PasswordEncoder ---
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // --- Bean: DaoAuthenticationProvider ---
    // We create this manually because we're not using
    // http.formLogin() or http.httpBasic() which would
    // create it automatically for us.
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService); // how to load from DB
        provider.setPasswordEncoder(passwordEncoder());     // how to hash password
        return provider;
    }

    // --- Bean: AuthenticationManager ---
    // We create a NEW ProviderManager and give it our list of providers.
    // Currently only DaoAuthenticationProvider is needed for token generation.
    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(
            Arrays.asList(daoAuthenticationProvider())
        );
    }

    // --- Bean: SecurityFilterChain ---
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationManager authenticationManager,
                                                   JWTUtil jwtUtil)
            throws Exception {

        // Create our custom filter object
        // Pass authenticationManager + jwtUtil (it needs both)
        JWTAuthenticationFilter jwtAuthFilter =
            new JWTAuthenticationFilter(authenticationManager, jwtUtil);

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/user-register").permitAll()
                .anyRequest().authenticated())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // no sessions!
            .csrf(csrf -> csrf.disable())
            .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class  // add our filter BEFORE this
            );

        return http.build();
    }
}
```

**Why `addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)`?**

```
Default Filter Chain Order (simplified):
─────────────────────────────────────────────────────
  SecurityContextPersistenceFilter
  ↓
  [JWTAuthenticationFilter]  ← WE INSERT HERE
  ↓
  UsernamePasswordAuthenticationFilter
  ↓
  BasicAuthenticationFilter
  ↓
  AuthorizationFilter
─────────────────────────────────────────────────────
```

---

## 2.5 Why No Controller for `/generate-token`?

The instructor makes this point explicitly:

```
┌────────────────────────────────────────────────────────┐
│  ❌ Wrong approach:                                     │
│  Create a @RestController with /generate-token         │
│  and write token logic there                           │
│                                                        │
│  ✅ Right approach (what we do):                        │
│  Handle it inside the Security Filter itself           │
│                                                        │
│  Why?                                                  │
│  Security logic belongs in the security layer.         │
│  We're using the framework the way it's designed —     │
│  filters are the right place to intercept,             │
│  validate, and respond to auth requests.               │
└────────────────────────────────────────────────────────┘
```

---

## 2.6 Full Flow Walkthrough (End to End)

Let's trace a real request:

```
1. User calls POST /generate-token
   Body: { "username": "sj", "password": "123" }

2. Request enters Filter Chain

3. JWTAuthenticationFilter gets invoked
   → path matches /generate-token ✅
   → reads LoginRequest (sj, 123)
   → creates UsernamePasswordAuthenticationToken(sj, 123)
   → calls authenticationManager.authenticate(token)

4. ProviderManager iterates its provider list
   → only one provider: DaoAuthenticationProvider
   → calls support(UsernamePasswordAuthenticationToken) → ✅ YES
   → calls authenticate()

5. DaoAuthenticationProvider
   → calls UserDetailsService.loadUserByUsername("sj")
   → fetches user from DB (username=sj, hashedPassword=..., role=ROLE_USER)
   → hashes incoming "123"
   → compares with stored hash
   → ✅ MATCH → returns fully authenticated object

6. Back in JWTAuthenticationFilter
   → authResult.isAuthenticated() = true
   → jwtUtil.generateToken("sj", 15)
   → response.setHeader("Authorization", "Bearer eyJhbGci...")

7. Response returned to client
   → No filterChain.doFilter() called
   → Never reaches controller
```

---

## Summary of Step 2

| Class | Role |
|---|---|
| `LoginRequest` | POJO to capture username+password from request body |
| `JWTUtil` | Generates JWT token using jjwt library |
| `JWTAuthenticationFilter` | Custom filter — reads creds, delegates to Dao, generates token |
| `DaoAuthenticationProvider` | Framework class — handles DB lookup + password match |
| `SecurityConfig` | Wires everything — creates filter, provider, manager, chain |

---
# Step 3 — Token Validation

---

## 3.1 What Are We Building Here?

Once the user has the JWT token, every time they want to access a restricted resource, they must send that token. The server needs to validate it.

```
┌─────────────────────────────────────────────────────────────┐
│                   Token Validation Flow                     │
│                                                             │
│  Client sends:                                              │
│  GET /api/users                                             │
│  Header: Authorization: Bearer <JWT token>                  │
│                 │                                           │
│                 ▼                                           │
│  Server extracts token from header                          │
│  Validates the token (signature + expiry)                   │
│  Extracts username from token payload                       │
│  Loads user from DB                                         │
│                 │                                           │
│        ┌────────┴────────┐                                  │
│     ✅ Valid          ❌ Invalid / Expired                    │
│        │                 │                                  │
│        ▼                 ▼                                  │
│  Store in Security    Throw Exception                       │
│  Context → allow      403 Forbidden                         │
│  access to resource                                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 3.2 The Problem We Need to Solve First

In Step 2, we used `UsernamePasswordAuthenticationToken` intentionally — because `DaoAuthenticationProvider` already knows how to handle it.

But now for token validation, the situation is different:

```
┌─────────────────────────────────────────────────────────────┐
│  What's coming in the request?                              │
│  → A JWT token string (not username + password)             │
│                                                             │
│  Can we use UsernamePasswordAuthenticationToken?            │
│  → Technically yes, but it's wrong semantically.            │
│    That token is designed for username+password.            │
│                                                             │
│  Can DaoAuthenticationProvider validate a JWT?              │
│  → NO. Dao just does DB lookup + password hash comparison.  │
│    It has no idea how to parse or verify a JWT.             │
│                                                             │
│  Solution:                                                  │
│  → Create a CUSTOM Authentication Object for JWT            │
│  → Create a CUSTOM Authentication Provider that             │
│    knows how to validate JWT                                │
└─────────────────────────────────────────────────────────────┘
```

---

## 3.3 Full Picture Before Coding

```
GET /api/users
Authorization: Bearer <token>
        │
        ▼
┌────────────────────────────────┐
│   JwtValidationFilter          │  ← Custom Filter (we write)
│   extends OncePerRequestFilter │
│                                │
│   1. Extract token from        │
│      Authorization header      │
│   2. Create:                   │
│      JwtAuthenticationToken    │  ← Custom Auth Object (we write)
│      (token, authenticated=false)
│   3. Pass to                   │
│      AuthenticationManager     │
└───────────────┬────────────────┘
                │
                ▼
┌────────────────────────────────┐
│   AuthenticationManager        │
│   (ProviderManager)            │
│                                │
│   Iterates providers...        │
│   calls support() on each...   │
└───────────────┬────────────────┘
                │
                ▼
┌──────────────────────────────────────────┐
│   Provider List                          │
│                                          │
│   DaoAuthenticationProvider              │
│   → support(JwtAuthenticationToken)❌ NO  │
│                                          │
│   JWTAuthenticationProvider              │  ← Custom Provider (we write)
│   → support(JwtAuthenticationToken)✅YES  │
│   → authenticate():                      │
│     1. Extract token from auth object    │
│     2. Validate token (signature+expiry) │
│     3. Extract username from payload     │
│     4. Load user from DB                 │
│     5. Return fully authenticated object │
└───────────────┬──────────────────────────┘
                │
                ▼
┌────────────────────────────────┐
│   Back in JwtValidationFilter  │
│                                │
│   Store auth object in         │
│   SecurityContextHolder        │
│                                │
│   Call filterChain.doFilter()  │  ← YES, continue chain this time!
│   (we DO want to reach         │
│    the controller here)        │
└───────────────┬────────────────┘
                │
                ▼
           Controller
      (returns the resource)
```

> 💡 **Key difference from Step 2:** In token generation, we did NOT call `filterChain.doFilter()` because we didn't need to reach the controller. In token validation, we MUST call it — because the whole point is to let the request through to the controller after validation.

---

## 3.4 Code — Step by Step

### Piece 1: `JwtAuthenticationToken.java` (Custom Authentication Object)

This is a custom wrapper that holds the raw JWT string. It extends `AbstractAuthenticationToken` which implements the `Authentication` interface.

```java
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String token;

    public JwtAuthenticationToken(String token) {
        super(null);            // null = no authorities yet (not authenticated)
        this.token = token;
        setAuthenticated(false); // explicitly mark as NOT authenticated yet
    }

    public String getToken() {
        return token;
    }

    @Override
    public Object getCredentials() {
        return token;  // the JWT string IS the credential
    }

    @Override
    public Object getPrincipal() {
        return null;   // we don't know who this is yet — not validated yet
    }
}
```

**Why do we need this custom object?**

```
┌──────────────────────────────────────────────────────────┐
│  Authentication Object Hierarchy                         │
│                                                          │
│  Authentication (interface)                              │
│    └── AbstractAuthenticationToken (abstract class)      │
│          ├── UsernamePasswordAuthenticationToken         │
│          │   → used by Form + Basic auth                 │
│          │   → DaoAuthenticationProvider supports this   │
│          │                                               │
│          └── JwtAuthenticationToken  ← WE CREATE THIS    │
│              → carries the JWT string                    │
│              → JWTAuthenticationProvider supports this   │
└──────────────────────────────────────────────────────────┘
```

This is the key mechanism. The TYPE of the authentication object determines which provider handles it. By creating a new type, we ensure only our custom provider picks it up.

---

### Piece 2: `JWTUtil.java` (Updated — add validation method)

We already wrote `generateToken()` in Step 2. Now we add `validateAndExtractUsername()`:

```java
@Component
public class JWTUtil {

    private static final String SECRET_KEY = "your-secure-secret-key-min-32bytes";
    private static final Key key =
        Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    // FROM STEP 2 — Generate JWT Token
    public String generateToken(String username, long expiryMinutes) {
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date(
                System.currentTimeMillis() + expiryMinutes * 60 * 1000))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    // NEW IN STEP 3 — Validate token + extract username
    public String validateAndExtractUsername(String token) {
        try {
            return Jwts.parser()
                .setSigningKey(key)   // use same key (HMAC = symmetric)
                .build()
                .parseClaimsJws(token) // parse + validate signature + expiry
                .getBody()             // get the payload (claims)
                .getSubject();         // extract "sub" = username
        } catch (JwtException e) {
            return null; // token is invalid or expired
        }
    }
}
```

**What does `parseClaimsJws()` actually verify?**

```
┌──────────────────────────────────────────────────────────┐
│  parseClaimsJws() does ALL of this automatically:        │
│                                                          │
│  1. Decodes the Base64 header + payload                  │
│  2. Recalculates the signature using our secret key      │
│  3. Compares recalculated signature with token signature │
│     → if mismatch → JwtException (tampered token)        │
│  4. Checks expiry time                                   │
│     → if expired → JwtException (expired token)          │
│  5. If all good → returns the Claims (payload)           │
└──────────────────────────────────────────────────────────┘
```

---

### Piece 3: `JWTAuthenticationProvider.java` (Custom Authentication Provider)

This is the brain of token validation. It implements `AuthenticationProvider` — the same interface that `DaoAuthenticationProvider` implements.

```java
public class JWTAuthenticationProvider implements AuthenticationProvider {

    private JWTUtil jwtUtil;
    private UserDetailsService userDetailsService;

    public JWTAuthenticationProvider(JWTUtil jwtUtil,
                                     UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        // STEP 1: Extract the raw JWT string from our custom auth object
        String token = ((JwtAuthenticationToken) authentication).getToken();

        // STEP 2: Validate token + extract username from payload
        String username = jwtUtil.validateAndExtractUsername(token);

        // STEP 3: If null → token is invalid or expired
        if (username == null) {
            throw new BadCredentialsException("Invalid JWT Token");
        }

        // STEP 4: Load user details from DB using the extracted username
        UserDetails userDetails =
            userDetailsService.loadUserByUsername(username);

        // STEP 5: Return a fully authenticated object
        // We use UsernamePasswordAuthenticationToken here (3-arg constructor)
        // because it sets isAuthenticated = true automatically
        // and carries the user's authorities (roles)
        return new UsernamePasswordAuthenticationToken(
            userDetails,                    // principal (who is this user)
            null,                           // credentials (no password needed)
            userDetails.getAuthorities()    // roles → needed for authorization
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        // "I only handle JwtAuthenticationToken objects"
        return JwtAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
```

**The `supports()` method is the gatekeeper:**

```
┌──────────────────────────────────────────────────────────┐
│  When ProviderManager iterates its provider list:        │
│                                                          │
│  DaoAuthenticationProvider.supports(JwtAuthToken)        │
│  → checks: is JwtAuthToken a UsernamePasswordToken?      │
│  → NO ❌ → skip                                           │
│                                                          │
│  JWTAuthenticationProvider.supports(JwtAuthToken)        │
│  → checks: is JwtAuthToken a JwtAuthenticationToken?     │
│  → YES ✅ → call authenticate()                           │
└──────────────────────────────────────────────────────────┘
```

---

### Piece 4: `JwtValidationFilter.java` (Custom Filter)

```java
public class JwtValidationFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;

    public JwtValidationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // STEP 1: Extract JWT from Authorization header
        // No path check here — this filter runs for ALL requests
        String token = extractJwtFromRequest(request);

        if (token != null) {

            // STEP 2: Create our custom authentication object
            // with the raw token string, isAuthenticated = false
            JwtAuthenticationToken authenticationToken =
                new JwtAuthenticationToken(token);

            // STEP 3: Pass to AuthenticationManager
            // → ProviderManager iterates providers
            // → JWTAuthenticationProvider says YES I support this
            // → validates token, loads user, returns authenticated object
            Authentication authResult =
                authenticationManager.authenticate(authenticationToken);

            // STEP 4: If authenticated → store in SecurityContextHolder
            // This makes the auth object available to the controller too
            if (authResult.isAuthenticated()) {
                SecurityContextHolder.getContext()
                    .setAuthentication(authResult);
            }
        }

        // STEP 5: ALWAYS continue the chain
        // Unlike token generation, we MUST reach the controller
        filterChain.doFilter(request, response);
    }

    // Helper: extract token string from "Authorization: Bearer <token>"
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // remove "Bearer " prefix
        }
        return null;
    }
}
```

---

### Piece 5: `SecurityConfig.java` (Updated)

Now we register `JWTAuthenticationProvider` and add the validation filter to the chain:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private JWTUtil jwtUtil;
    private UserDetailsService userDetailsService;

    @Autowired
    public SecurityConfig(JWTUtil jwtUtil,
                          UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // NEW: Register our custom JWT provider as a Bean
    @Bean
    public JWTAuthenticationProvider jwtAuthenticationProvider() {
        return new JWTAuthenticationProvider(jwtUtil, userDetailsService);
    }

    // UPDATED: Now ProviderManager has TWO providers in its list
    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(Arrays.asList(
            daoAuthenticationProvider(),     // handles UsernamePasswordToken
            jwtAuthenticationProvider()      // handles JwtAuthenticationToken
        ));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationManager authenticationManager,
                                                   JWTUtil jwtUtil)
            throws Exception {

        JWTAuthenticationFilter jwtAuthFilter =
            new JWTAuthenticationFilter(authenticationManager, jwtUtil);

        // NEW: Create validation filter
        JwtValidationFilter jwtValidationFilter =
            new JwtValidationFilter(authenticationManager);

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/user-register").permitAll()
                .anyRequest().authenticated())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            // Generate token filter — BEFORE UsernamePasswordAuthenticationFilter
            .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class)
            // Validate token filter — AFTER JWTAuthenticationFilter
            .addFilterAfter(
                jwtValidationFilter,
                JWTAuthenticationFilter.class);

        return http.build();
    }
}
```

**Updated filter chain order:**

```
Request comes in
      │
      ▼
SecurityContextPersistenceFilter
      │
      ▼
JWTAuthenticationFilter        ← handles /generate-token only
      │
      ▼
JwtValidationFilter            ← validates token for ALL requests
      │
      ▼
UsernamePasswordAuthenticationFilter  (default, mostly skipped)
      │
      ▼
AuthorizationFilter            ← checks roles
      │
      ▼
Controller
```

---

## 3.5 What Happens With an Invalid Token?

```
Client sends: Authorization: Bearer <tampered-or-expired-token>
      │
      ▼
JwtValidationFilter extracts token
      │
      ▼
Creates JwtAuthenticationToken(bad-token)
      │
      ▼
JWTAuthenticationProvider.authenticate()
      │
      ▼
jwtUtil.validateAndExtractUsername(bad-token)
      │
      ▼
parseClaimsJws() → throws JwtException
      │
      ▼
returns null
      │
      ▼
throw new BadCredentialsException("Invalid JWT Token")
      │
      ▼
Request fails → 403 Forbidden
Controller is NEVER reached
```

---

## 3.6 Full End-to-End Walkthrough

```
1. User already has token from Step 2.
   Calls GET /api/users
   Header: Authorization: Bearer eyJhbGci...

2. JWTAuthenticationFilter runs
   → path is NOT /generate-token
   → calls filterChain.doFilter() immediately
   → passes to next filter

3. JwtValidationFilter runs
   → extracts token from Authorization header
   → creates JwtAuthenticationToken(token, authenticated=false)
   → calls authenticationManager.authenticate(jwtAuthToken)

4. ProviderManager iterates providers:
   → DaoAuthenticationProvider.support(JwtAuthToken) → ❌ NO
   → JWTAuthenticationProvider.support(JwtAuthToken) → ✅ YES
   → calls JWTAuthenticationProvider.authenticate()

5. JWTAuthenticationProvider:
   → extracts token from JwtAuthenticationToken
   → calls jwtUtil.validateAndExtractUsername(token)
   → parseClaimsJws() validates signature + expiry ✅
   → extracts username = "sj"
   → calls userDetailsService.loadUserByUsername("sj")
   → loads user from DB (username, roles, etc.)
   → returns UsernamePasswordAuthenticationToken(
        userDetails, null, authorities)
        isAuthenticated = true ✅

6. Back in JwtValidationFilter:
   → authResult.isAuthenticated() = true
   → SecurityContextHolder.getContext().setAuthentication(authResult)
   → calls filterChain.doFilter() ← continues the chain

7. Request reaches Controller
   → Controller can also access the auth object
      via SecurityContextHolder if needed
   → Returns "fetched user details successfully"
```

---

## Summary of Step 3

| Class | Role |
|---|---|
| `JwtAuthenticationToken` | Custom auth object — carries raw JWT string, type determines which provider handles it |
| `JWTUtil.validateAndExtractUsername()` | Validates token signature + expiry, extracts username |
| `JWTAuthenticationProvider` | Custom provider — validates JWT, loads user, returns authenticated object |
| `JwtValidationFilter` | Runs on every request — extracts token, triggers validation, stores result in SecurityContextHolder |
| `SecurityConfig` | Adds new provider to ProviderManager list, adds validation filter to chain |

---
# Step 4 — Refresh Token

---

## 4.1 The Problem — Why Do We Need Refresh Tokens?

Before writing any code, the instructor spends time explaining the **why** behind refresh tokens. This is important to understand deeply.

```
┌─────────────────────────────────────────────────────────────┐
│                  The Access Token Problem                   │
│                                                             │
│  Access tokens are SHORT LIVED (e.g. 15 minutes)            │
│                                                             │
│  Why short lived?                                           │
│  → If a token gets leaked/stolen, attacker has access       │
│  → With JWT, server CANNOT invalidate a token early         │
│    (remember — stateless, no session on server)             │
│  → So the only safety net is: make it expire quickly        │
│                                                             │
│  But this creates a UX problem:                             │
│  → After 15 minutes, user has to login again                │
│  → For every restricted API call                            │
│  → This is completely unacceptable in real apps             │
└─────────────────────────────────────────────────────────────┘
```

**The solution — Refresh Token:**

```
┌─────────────────────────────────────────────────────────────┐
│                   Two Token Strategy                        │
│                                                             │
│  ACCESS TOKEN                  REFRESH TOKEN                │
│  ─────────────                 ─────────────                │
│  Short lived (15 min)          Long lived (7 days)          │
│  Used to access APIs           Used ONLY to get new         │
│                                access token                 │
│  Sent in every request         Sent ONLY to                 │
│  Authorization header          /refresh-token endpoint      │
│                                                             │
│  If leaked → expires in 15min  Keep this VERY safe          │
│  Risk is low                   Risk is high if leaked       │
└─────────────────────────────────────────────────────────────┘
```

**The full lifecycle:**

```
┌──────────────────────────────────────────────────────────────┐
│                   Token Lifecycle                            │
│                                                              │
│  Step 1: User logs in → /generate-token                      │
│          Server returns:                                     │
│          → Access Token  (15 min) in Authorization header    │
│          → Refresh Token (7 days) in HttpOnly Cookie         │
│                                                              │
│  Step 2: User accesses APIs using Access Token               │
│          (works for 15 minutes)                              │
│                 │                                            │
│                 ▼                                            │
│  Step 3: Access Token expires after 15 min                   │
│          User calls /refresh-token                           │
│          Browser automatically sends Refresh Token           │
│          via cookie                                          │
│                 │                                            │
│                 ▼                                            │
│  Step 4: Server validates Refresh Token                      │
│          Issues NEW Access Token (15 min)                    │
│          Returns in Authorization header                     │
│                 │                                            │
│                 ▼                                            │
│  Step 5: User continues using new Access Token               │
│          Back to Step 2                                      │
└──────────────────────────────────────────────────────────────┘
```

---

## 4.2 How to Send the Refresh Token Securely?

The instructor explains two approaches and gives his recommendation:

```
┌──────────────────────────────────────────────────────────────┐
│           Two Ways to Return Refresh Token                   │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Option 1: Return in Response Body                           │
│  → Client receives it in JSON response                       │
│  → Client must store it somewhere (localStorage, memory)     │
│  → localStorage is vulnerable to XSS attacks                 │
│  → Onus is on the CLIENT to keep it safe                     │
│                                                              │
│  Option 2: HttpOnly Cookie ← instructor's recommendation     │
│  → Server sets it as a cookie                                │
│  → Browser stores it automatically                           │
│  → JavaScript CANNOT access it (HttpOnly flag)               │
│  → Sent automatically by browser to correct endpoint         │
│  → Much harder to steal via XSS                              │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

**The 3 security flags the instructor sets on the cookie:**

```
┌──────────────────────────────────────────────────────────────┐
│              Refresh Token Cookie Security Flags             │
│                                                              │
│  HttpOnly = true                                             │
│  → JavaScript (document.cookie) CANNOT read this cookie      │
│  → Protects against XSS attacks                              │
│                                                              │
│  Secure = true                                               │
│  → Cookie is ONLY sent over HTTPS connections                │
│  → Protects against man-in-the-middle attacks                │
│                                                              │
│  Path = "/refresh-token"                                     │
│  → Browser sends this cookie ONLY when hitting               │
│    /refresh-token endpoint                                   │
│  → NOT sent with every single request                        │
│  → Reduces exposure significantly                            │
│                                                              │
│  MaxAge = 7 * 24 * 60 * 60 (7 days in seconds)               │
│  → Cookie lives for 7 days in the browser                    │
└──────────────────────────────────────────────────────────────┘
```

---

## 4.3 Full Picture Before Coding

There are **two parts** to this step:

**Part A** — Update `JWTAuthenticationFilter` (from Step 2) to also return a refresh token during login.

**Part B** — Write a brand new `JWTRefreshFilter` to handle the `/refresh-token` endpoint.

```
PART A: During Login (/generate-token)
──────────────────────────────────────
Login request validated ✅
      │
      ├──→ Generate ACCESS TOKEN (15 min)
      │    → Put in Authorization header
      │
      └──→ Generate REFRESH TOKEN (7 days)
           → Put in HttpOnly Cookie
           → Cookie path = /refresh-token only


PART B: Refresh Flow (/refresh-token)
──────────────────────────────────────
Client calls GET /refresh-token
Browser auto-sends refreshToken cookie
      │
      ▼
┌─────────────────────────────┐
│   JWTRefreshFilter          │  ← New filter (we write)
│                             │
│   1. Check path =           │
│      /refresh-token         │
│   2. Read refresh token     │
│      from Cookie            │
│   3. Create                 │
│      JwtAuthenticationToken │  ← Reuse from Step 3!
│   4. Pass to                │
│      AuthenticationManager  │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│   JWTAuthenticationProvider │  ← Reuse from Step 3!
│                             │
│   Validates refresh token   │
│   (same validation logic —  │
│   it's still a JWT)         │
│   Loads user from DB        │
│   Returns authenticated obj │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│   Back in JWTRefreshFilter  │
│                             │
│   Generate NEW ACCESS TOKEN │
│   (15 min)                  │
│   Put in Authorization      │
│   header                    │
│                             │
│   NO filterChain.doFilter() │
│   Don't go to controller    │
└─────────────────────────────┘
```

> 💡 **Big insight from instructor:** For the refresh flow, we **reuse** `JwtAuthenticationToken` and `JWTAuthenticationProvider` from Step 3. The provider doesn't care if it's an access token or refresh token — it just validates whatever JWT it receives. This is elegant reuse of our existing framework extension.

---

## 4.4 Code — Step by Step

### Piece 1: Update `JWTAuthenticationFilter.java`

Only the authenticated block changes — we now also generate and return a refresh token:

```java
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;

    public JWTAuthenticationFilter(AuthenticationManager authenticationManager,
                                   JWTUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (!request.getServletPath().equals("/generate-token")) {
            filterChain.doFilter(request, response);
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        LoginRequest loginRequest =
            objectMapper.readValue(request.getInputStream(), LoginRequest.class);

        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            );

        Authentication authResult =
            authenticationManager.authenticate(authToken);

        if (authResult.isAuthenticated()) {

            // ── ACCESS TOKEN ──────────────────────────────────────
            // Short lived: 15 minutes
            // Returned in Authorization header
            String token = jwtUtil.generateToken(authResult.getName(), 15);
            response.setHeader("Authorization", "Bearer " + token);

            // ── REFRESH TOKEN ─────────────────────────────────────
            // Long lived: 7 days
            // NOT in header — stored in HttpOnly Cookie
            String refreshToken =
                jwtUtil.generateToken(authResult.getName(), 7 * 24 * 60);
                // 7 days converted to minutes

            // Build the cookie with all 3 security flags
            Cookie refreshCookie = new Cookie("refreshToken", refreshToken);

            refreshCookie.setHttpOnly(true);
            // → JavaScript cannot access this cookie

            refreshCookie.setSecure(true);
            // → Only sent over HTTPS

            refreshCookie.setPath("/refresh-token");
            // → Browser sends this cookie ONLY to /refresh-token
            // → Not leaked into every other request

            refreshCookie.setMaxAge(7 * 24 * 60 * 60);
            // → 7 days in SECONDS (cookie expiry)

            response.addCookie(refreshCookie);
        }

        // Do NOT continue chain — response is complete
    }
}
```

---

### Piece 2: `JWTRefreshFilter.java` (Brand New Filter)

```java
public class JWTRefreshFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;

    public JWTRefreshFilter(JWTUtil jwtUtil,
                            AuthenticationManager authenticationManager) {
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // STEP 1: Only care about /refresh-token endpoint
        if (!request.getServletPath().equals("/refresh-token")) {
            filterChain.doFilter(request, response);
            return;
        }

        // STEP 2: Extract refresh token from Cookie
        // (NOT from Authorization header — it's in the cookie)
        String refreshToken = extractJwtFromRequest(request);

        if (refreshToken == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // STEP 3: Create JwtAuthenticationToken
        // Same custom auth object we used in Step 3 validation
        // Reusing it here — provider doesn't care if it's access or refresh token
        JwtAuthenticationToken authenticationToken =
            new JwtAuthenticationToken(refreshToken);

        // STEP 4: Pass to AuthenticationManager
        // → JWTAuthenticationProvider handles it
        // → validates the refresh token (signature + expiry)
        // → loads user from DB
        // → returns authenticated object
        Authentication authResult =
            authenticationManager.authenticate(authenticationToken);

        // STEP 5: If refresh token is valid → generate NEW access token
        if (authResult.isAuthenticated()) {
            String newToken =
                jwtUtil.generateToken(authResult.getName(), 15); // 15 min
            response.setHeader("Authorization", "Bearer " + newToken);
        }

        // STEP 6: Do NOT continue chain
        // We've sent the new access token — job done
        // No need to reach controller
    }

    // Read the refresh token from the cookie
    // (Browser sends it automatically because path matches /refresh-token)
    private String extractJwtFromRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        String refreshToken = null;
        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                refreshToken = cookie.getValue();
            }
        }
        return refreshToken;
    }
}
```

---

### Piece 3: `SecurityConfig.java` (Updated — add refresh filter)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private JWTUtil jwtUtil;
    private UserDetailsService userDetailsService;

    @Autowired
    public SecurityConfig(JWTUtil jwtUtil,
                          UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public JWTAuthenticationProvider jwtAuthenticationProvider() {
        return new JWTAuthenticationProvider(jwtUtil, userDetailsService);
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(Arrays.asList(
            daoAuthenticationProvider(),
            jwtAuthenticationProvider()
        ));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationManager authenticationManager,
                                                   JWTUtil jwtUtil)
            throws Exception {

        JWTAuthenticationFilter jwtAuthFilter =
            new JWTAuthenticationFilter(authenticationManager, jwtUtil);

        JwtValidationFilter jwtValidationFilter =
            new JwtValidationFilter(authenticationManager);

        // NEW: Create refresh filter
        JWTRefreshFilter jwtRefreshFilter =
            new JWTRefreshFilter(jwtUtil, authenticationManager);

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/user-register").permitAll()
                .anyRequest().authenticated())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(
                jwtValidationFilter,
                JWTAuthenticationFilter.class)
            // NEW: Refresh filter goes AFTER validation filter
            .addFilterAfter(
                jwtRefreshFilter,
                JwtValidationFilter.class);

        return http.build();
    }
}
```

**Final filter chain order — the complete picture:**

```
Incoming Request
      │
      ▼
SecurityContextPersistenceFilter  (default)
      │
      ▼
JWTAuthenticationFilter           → handles /generate-token
      │                             returns access + refresh token
      ▼
JwtValidationFilter               → runs for ALL requests
      │                             validates token, stores in context
      ▼
JWTRefreshFilter                  → handles /refresh-token
      │                             validates refresh token, returns
      ▼                             new access token
UsernamePasswordAuthenticationFilter  (default, mostly bypassed)
      │
      ▼
AuthorizationFilter               → checks roles
      │
      ▼
Controller
```

---

## 4.5 Full End-to-End Walkthrough

**Part A — Login and getting both tokens:**

```
1. User calls POST /generate-token
   Body: { "username": "sj", "password": "123" }

2. JWTAuthenticationFilter intercepts
   → validates credentials via DaoAuthenticationProvider ✅
   → generates ACCESS TOKEN (15 min)
      → set in Authorization header
   → generates REFRESH TOKEN (7 days)
      → set in HttpOnly Cookie
         HttpOnly=true, Secure=true,
         Path=/refresh-token, MaxAge=7days

3. Client receives:
   Headers: Authorization: Bearer <access-token>
   Cookie:  refreshToken=<refresh-token>
            (HttpOnly — JS cannot read this)
```

**Part B — Access token expires, client refreshes:**

```
1. After 15 minutes, access token expires
   Client calls GET /refresh-token
   Browser automatically attaches refreshToken cookie
   (because path matches /refresh-token)

2. JWTRefreshFilter intercepts
   → reads refreshToken from cookie
   → creates JwtAuthenticationToken(refreshToken)
   → passes to AuthenticationManager

3. JWTAuthenticationProvider handles it
   → validateAndExtractUsername(refreshToken)
   → signature valid ✅, not expired ✅ (still within 7 days)
   → extracts username = "sj"
   → loads user from DB
   → returns authenticated object

4. Back in JWTRefreshFilter
   → generates NEW ACCESS TOKEN (15 min)
   → sets in Authorization header

5. Client receives new access token
   Continues making API calls normally
```

---

## 4.6 What Gets Reused vs What Is New

The instructor explicitly points this out — and it shows how cleanly the framework extension scales:

```
┌───────────────────────────────────────────────────────────────┐
│                    Reuse vs New in Step 4                     │
├──────────────────────────┬────────────────────────────────────┤
│       REUSED             │         NEW                        │
├──────────────────────────┼────────────────────────────────────┤
│ JwtAuthenticationToken   │ JWTRefreshFilter                   │
│ JWTAuthenticationProvider│ Refresh cookie logic in            │
│ JWTUtil.generateToken()  │ JWTAuthenticationFilter            │
│ JWTUtil.validate...()    │                                    │
│ AuthenticationManager    │                                    │
│ ProviderManager list     │                                    │
└──────────────────────────┴────────────────────────────────────┘
```

---

## Summary of Step 4

| Concept | Key Takeaway |
|---|---|
| Access tokens are short lived | Because leaked JWT cannot be invalidated server-side |
| Refresh tokens solve the UX problem | Long lived — used only to get new access token |
| HttpOnly cookie for refresh token | JS cannot read it — protects against XSS |
| Secure flag | Only sent over HTTPS |
| Path flag | Only sent to /refresh-token — not every request |
| JWTRefreshFilter | New filter, reuses existing auth objects and provider |
| No new provider needed | JWTAuthenticationProvider handles both access and refresh token validation |

---
# Step 5 — Authorization (Role-Based Access Control)

---

## 5.1 What Are We Building Here?

Authentication answers: **"Who are you?"**
Authorization answers: **"What are you allowed to do?"**

We've built authentication across Steps 2, 3, and 4. Now we layer authorization on top — restricting which roles can access which APIs.

```
┌─────────────────────────────────────────────────────────────┐
│                   Authorization Flow                        │
│                                                             │
│  User has valid JWT token ✅                                 │
│  Calls GET /api/users                                       │
│                 │                                           │
│                 ▼                                           │
│  JwtValidationFilter validates token                        │
│  Stores auth object in SecurityContextHolder                │
│  (auth object carries the user's ROLE)                      │
│                 │                                           │
│                 ▼                                           │
│  AuthorizationFilter checks:                                │
│  "Does this user's role match what /api/users requires?"    │
│                 │                                           │
│        ┌────────┴────────┐                                  │
│   ✅ Role matches    ❌ Role mismatch                         │
│        │                 │                                  │
│        ▼                 ▼                                  │
│   Allow access       403 Forbidden                          │
│   → Controller       Controller never reached               │
└─────────────────────────────────────────────────────────────┘
```

> 💡 **Instructor's note:** Authorization works EXACTLY the same way as it does in Form-based and Basic authentication. We don't write anything new for this — Spring's `AuthorizationFilter` (already in the chain) handles it automatically. We just configure which role each endpoint requires.

---

## 5.2 How Does AuthorizationFilter Know the User's Role?

This connects back to something we did in Step 3 — inside `JWTAuthenticationProvider`:

```java
// From Step 3 — JWTAuthenticationProvider.authenticate()
return new UsernamePasswordAuthenticationToken(
    userDetails,                    // principal
    null,                           // credentials
    userDetails.getAuthorities()    // ← ROLES are passed here!
);
```

And in Step 3's `JwtValidationFilter`:

```java
// Stored in SecurityContextHolder after validation
SecurityContextHolder.getContext().setAuthentication(authResult);
```

So the full chain is:

```
┌──────────────────────────────────────────────────────────────┐
│          How Role Reaches AuthorizationFilter                │
│                                                              │
│  1. During registration:                                     │
│     User saved with ROLE_USER in DB                          │
│                                                              │
│  2. During token validation (Step 3):                        │
│     JWTAuthenticationProvider loads user from DB             │
│     → userDetails.getAuthorities() = [ROLE_USER]             │
│     → stored in UsernamePasswordAuthenticationToken          │
│     → stored in SecurityContextHolder                        │
│                                                              │
│  3. AuthorizationFilter:                                     │
│     → reads auth object from SecurityContextHolder           │
│     → checks authorities against required role for API       │
│     → ROLE_USER matches /api/users requirement ✅             │
│     → ROLE_ADMIN does NOT match ❌ → 403 Forbidden            │
└──────────────────────────────────────────────────────────────┘
```

---

## 5.3 Complete Filter Chain — The Full Picture

Now that all 5 steps are done, here is the complete filter chain with every filter we've built:

```
Incoming Request
      │
      ▼
┌─────────────────────────────────────────────────────────────┐
│                    Complete Filter Chain                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. SecurityContextPersistenceFilter  (default)             │
│                                                             │
│  2. JWTAuthenticationFilter           (we wrote - Step 2)   │
│     → Only activates for /generate-token                    │
│     → Validates credentials via Dao                         │
│     → Returns access token + refresh cookie                 │
│     → Does NOT continue chain                               │
│                                                             │
│  3. JwtValidationFilter               (we wrote - Step 3)   │
│     → Activates for ALL requests                            │
│     → Extracts + validates JWT from Authorization header    │
│     → Stores auth object in SecurityContextHolder           │
│     → DOES continue chain                                   │
│                                                             │
│  4. JWTRefreshFilter                  (we wrote - Step 4)   │
│     → Only activates for /refresh-token                     │
│     → Validates refresh token from cookie                   │
│     → Returns new access token                              │
│     → Does NOT continue chain                               │
│                                                             │
│  5. UsernamePasswordAuthenticationFilter  (default, skipped)│
│                                                             │
│  6. AuthorizationFilter               (default - Step 5)    │
│     → Checks role from SecurityContextHolder                │
│     → Matches against configured endpoint requirements      │
│     → Allows or blocks access                               │
│                                                             │
│  7. Controller                                              │
│     → Business logic runs here                              │
│     → Returns response                                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 5.4 Code — Only SecurityConfig Changes

Authorization requires just **one line change** in `SecurityConfig`. Everything else stays the same.

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                               AuthenticationManager authenticationManager,
                                               JWTUtil jwtUtil)
        throws Exception {

    JWTAuthenticationFilter jwtAuthFilter =
        new JWTAuthenticationFilter(authenticationManager, jwtUtil);

    JwtValidationFilter jwtValidationFilter =
        new JwtValidationFilter(authenticationManager);

    JWTRefreshFilter jwtRefreshFilter =
        new JWTRefreshFilter(jwtUtil, authenticationManager);

    http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/user-register").permitAll()
            // ↓ THIS IS THE ONLY NEW LINE — role based restriction
            .requestMatchers("/api/users").hasRole("USER")
            .anyRequest().authenticated())
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(csrf -> csrf.disable())
        .addFilterBefore(
            jwtAuthFilter,
            UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(
            jwtValidationFilter,
            JWTAuthenticationFilter.class)
        .addFilterAfter(
            jwtRefreshFilter,
            JwtValidationFilter.class);

    return http.build();
}
```

**How `hasRole()` works internally:**

```
┌──────────────────────────────────────────────────────────────┐
│  .requestMatchers("/api/users").hasRole("USER")              │
│                                                              │
│  Spring internally converts this to:                         │
│  → requires authority "ROLE_USER"                            │
│                                                              │
│  So in DB, role must be stored as "ROLE_USER"                │
│  And UserRegisterEntity.getAuthorities() must return         │
│  → List.of(new SimpleGrantedAuthority(role))                 │
│  where role = "ROLE_USER"                                    │
└──────────────────────────────────────────────────────────────┘
```

---

## 5.5 End-to-End Walkthrough — Both Scenarios

**Scenario A — Correct Role (ROLE_USER accessing /api/users):**

```
1. User registered with ROLE_USER
   Calls POST /generate-token → gets access token

2. Calls GET /api/users
   Header: Authorization: Bearer <token>

3. JWTAuthenticationFilter
   → path is NOT /generate-token → passes to next filter

4. JwtValidationFilter
   → extracts token from header
   → JWTAuthenticationProvider validates token ✅
   → loads user from DB → ROLE_USER
   → returns authenticated object with [ROLE_USER]
   → stores in SecurityContextHolder
   → calls filterChain.doFilter()

5. JWTRefreshFilter
   → path is NOT /refresh-token → passes to next filter

6. AuthorizationFilter
   → reads auth from SecurityContextHolder
   → /api/users requires ROLE_USER
   → user has ROLE_USER ✅ MATCH
   → allows request through

7. Controller runs
   → returns "fetched user details successfully"
```

**Scenario B — Wrong Role (ROLE_ADMIN accessing /api/users):**

```
1. User registered with ROLE_ADMIN
   Calls POST /generate-token → gets access token

2. Calls GET /api/users
   Header: Authorization: Bearer <token>

3. JWTAuthenticationFilter → passes

4. JwtValidationFilter
   → validates token ✅
   → loads user → ROLE_ADMIN
   → stores in SecurityContextHolder
   → continues chain

5. JWTRefreshFilter → passes

6. AuthorizationFilter
   → reads auth from SecurityContextHolder
   → /api/users requires ROLE_USER
   → user has ROLE_ADMIN ❌ MISMATCH
   → throws AccessDeniedException
   → 403 Forbidden returned

7. Controller is NEVER reached
```

---

## 5.6 Complete Class Summary — Everything We Built

Here is every single class across all 5 steps in one place:

```
┌──────────────────────────────────────────────────────────────┐
│              Complete JWT Implementation Map                 │
├────────────────────┬─────────────────────────────────────────┤
│ Class              │ Purpose                                 │
├────────────────────┼─────────────────────────────────────────┤
│ LoginRequest       │ POJO — captures username+password       │
│                    │ from /generate-token request body       │
├────────────────────┼─────────────────────────────────────────┤
│ JWTUtil            │ Generates + validates JWT tokens        │
│                    │ Uses jjwt library + HMAC-SHA256         │
├────────────────────┼─────────────────────────────────────────┤
│ JwtAuthentication  │ Custom Authentication Object            │
│ Token              │ Carries raw JWT string                  │
│                    │ TYPE determines which provider handles  │
├────────────────────┼─────────────────────────────────────────┤
│ JWTAuthentication  │ Custom Filter — Step 2                  │
│ Filter             │ Handles /generate-token                 │
│                    │ Returns access token + refresh cookie   │
├────────────────────┼─────────────────────────────────────────┤
│ JwtValidationFilter│ Custom Filter — Step 3                  │
│                    │ Runs on ALL requests                    │
│                    │ Validates token, stores in context      │
├────────────────────┼─────────────────────────────────────────┤
│ JWTRefreshFilter   │ Custom Filter — Step 4                  │
│                    │ Handles /refresh-token                  │
│                    │ Issues new access token                 │
├────────────────────┼─────────────────────────────────────────┤
│ JWTAuthentication  │ Custom Provider — Step 3                │
│ Provider           │ supports() JwtAuthenticationToken       │
│                    │ Validates JWT + loads user from DB      │
├────────────────────┼─────────────────────────────────────────┤
│ SecurityConfig     │ Wires everything together               │
│                    │ Registers filters, providers, manager   │
└────────────────────┴─────────────────────────────────────────┘
```

---

## 5.7 The Big Picture — Everything Connected

```
                         INCOMING REQUEST
                               │
              ┌────────────────┼─────────────────┐
              │                │                 │
    /generate-token     /api/users (any)    /refresh-token
              │                │                 │
              ▼                ▼                 │
   ┌─────────────────┐  ┌──────────────┐         │
   │JWTAuthentication│  │JwtValidation │         │
   │Filter           │  │Filter        │         │
   │                 │  │              │         │
   │reads: body      │  │reads: Auth   │         │
   │creates: Username│  │header        │         │
   │PasswordAuth     │  │creates:      │         │
   │Token            │  │JwtAuth       │         │
   │                 │  │Token         │         │
   └────────┬────────┘  └──────┬───────┘         │
            │                  │                 │
            ▼                  ▼                 ▼
   ┌──────────────────────────────────────────────────┐
   │              AuthenticationManager               │
   │              (ProviderManager)                   │
   │                                                  │
   │  ┌──────────────────────────────────────────┐    │
   │  │           Provider List                  │    │
   │  │                                          │    │
   │  │  DaoAuthenticationProvider               │    │
   │  │  → supports UsernamePasswordToken ✅      │    │
   │  │  → hashes pwd, loads DB, compares        │    │
   │  │                                          │    │
   │  │  JWTAuthenticationProvider               │    │
   │  │  → supports JwtAuthenticationToken ✅     │    │
   │  │  → validates JWT, loads user from DB     │    │
   │  └──────────────────────────────────────────┘    │
   └────────────────────┬─────────────────────────────┘
                        │
            ┌───────────┴──────────┐
            │                      │
   /generate-token           /api/users
            │                      │
            ▼                      ▼
   Generate tokens      SecurityContextHolder
   access → header      stores auth object
   refresh → cookie            │
                               ▼
                       AuthorizationFilter
                       checks role ✅ or ❌
                               │
                               ▼
                          Controller
```

---

## 5.8 Interview Tips — From the Instructor

These are the key points the instructor emphasizes that are very likely to come up:

**Q: Why does Spring Boot not provide a default JWT implementation?**
Because different applications have different requirements — what goes in the payload, which signing algorithm to use, whether refresh tokens are needed, expiry strategy. There is no one-size-fits-all solution.

**Q: How does `ProviderManager` decide which `AuthenticationProvider` to use?**
It iterates over its list of providers and calls `support()` on each one, passing the type of the `Authentication` object. The first provider that returns true gets delegated the `authenticate()` call.

**Q: Why do we use `OncePerRequestFilter`?**
To guarantee a filter runs only once per request, even if the filter chain internally revisits it.

**Q: Why is the refresh token stored in an HttpOnly cookie instead of the response body?**
HttpOnly cookies cannot be accessed by JavaScript, protecting against XSS attacks. The `Secure` flag ensures it's only sent over HTTPS. The `Path` flag limits it to only the `/refresh-token` endpoint.

**Q: Why is the access token short-lived?**
Because JWT tokens are stateless — the server cannot invalidate them before expiry. Short expiry limits the damage window if a token is stolen.

**Q: What is the difference between Authentication and Authorization?**
Authentication = verifying who you are (valid token, valid credentials). Authorization = verifying what you are allowed to do (role check against endpoint requirements).

**Q: Where are the user's roles stored during a request?**
In the `Authentication` object inside `SecurityContextHolder`. The `JWTAuthenticationProvider` loads them from DB and puts them in the returned `UsernamePasswordAuthenticationToken` as authorities.

---

## Final Summary — All 5 Steps

| Step | What We Built | Key Classes |
|---|---|---|
| 1 | Understood architecture + JWT structure | Conceptual foundation |
| 2 | Token Generation | `JWTAuthenticationFilter`, `JWTUtil`, `LoginRequest` |
| 3 | Token Validation | `JwtValidationFilter`, `JwtAuthenticationToken`, `JWTAuthenticationProvider` |
| 4 | Refresh Token | `JWTRefreshFilter`, updated `JWTAuthenticationFilter` |
| 5 | Authorization | `SecurityConfig` — one line `.hasRole()` |

That's the complete JWT implementation in Spring Boot Security — all 5 steps, built on top of the Spring Security framework itself without stepping outside it.