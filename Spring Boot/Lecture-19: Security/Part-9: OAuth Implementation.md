# Spring Boot Security — OAuth2 Authentication Implementation
## Part 1: OAuth2 vs OIDC — What's the Difference & Why Does It Matter?

---

Before jumping into implementation, the instructor clears one important confusion first — **OAuth2 and OIDC are NOT the same thing**, even though they are deeply related. Understanding this difference is critical because in this lecture we are implementing **authentication** (not just authorization), and the token type, scope, and flow behave differently depending on which one you're using.

---

### The Core Difference

Think of it this way:

- **OAuth2** was built to answer: *"Can this third-party app access my data?"*
- **OIDC** was built to answer: *"Who is this user? Is this user really who they claim to be?"*

OIDC is not a replacement for OAuth2. It is a **layer built on top of OAuth2**, specifically for authentication.

---

### Comparison Table

```
+------------------+---------------------------+------------------------------------+
|     Feature      |          OAuth2           |              OIDC                  |
+------------------+---------------------------+------------------------------------+
| Full Form        | Open Authorization        | OpenID Connect                     |
+------------------+---------------------------+------------------------------------+
| Purpose          | AUTHORIZATION             | AUTHENTICATION                     |
|                  | Grant secure 3rd-party    | Layer on top of OAuth2.            |
|                  | access to user's          | Lets a 3rd-party app VERIFY        |
|                  | protected data            | the identity of a user via         |
|                  |                           | an Authorization Server            |
+------------------+---------------------------+------------------------------------+
| Token Generated  | Access Token              | ID Token (JWT)                     |
|                  | (Opaque or JWT)           | + Access Token (both given)        |
+------------------+---------------------------+------------------------------------+
| Token Understood | Resource Server           | ID Token → Only the 3rd-party app  |
| By               | validates it              | Access Token → Resource Server     |
+------------------+---------------------------+------------------------------------+
| Scope Used       | read / write              | openid (mandatory)                 |
|                  |                           | + profile, email (optional)        |
+------------------+---------------------------+------------------------------------+
```

---

### The Token Difference — Explained Clearly

This is the most important part to understand before writing a single line of code.

```
OAUTH2 FLOW (plain):
====================

  3rd Party App                 Resource Server
  (e.g. Insta)                  (e.g. Gmail)
       |                               |
       |   --- passes Access Token --> |
       |                               |
       |  Access Token can be:         |
       |  - Opaque (only the           |
       |    Authorization Server       |
       |    understands it)            |
       |  - JWT (readable, signed)     |
       |                               |
       |  Resource Server validates    |
       |  it and gives back data       |
       |  (emails, calendar, etc.)     |


OIDC FLOW:
==========

  3rd Party App              Authorization Server
  (our Spring            (GitLab / Auth0)
   Boot app)                    |
       |                        |
       | <--- ID Token (JWT) ---|
       |                        |
  ID Token contains:            |
  - user's name                 |
  - email                       |
  - profile picture             |
  - sub (unique user ID)        |
  (minimal info, just enough    |
   for authentication)          |
       |                        |
  [ID Token is NOT sent to      |
   Resource Server]             |
       |                        |
  ALSO gets an Access Token     |
  (if you need to call          |
   Resource Server too)         |
```

So to be very clear:

- In plain OAuth2 → you get an **Access Token** → pass it to Resource Server → get protected data
- In OIDC → you get an **ID Token (JWT)** → it has minimal user info → use it ONLY for authentication → **do NOT pass it to Resource Server**
- But since OIDC is built on top of OAuth2, you also get an **Access Token** alongside — so if you need protected data too, you can still do that

---

### What is the `openid` scope?

In plain OAuth2, the scope is things like `read`, `write` — telling the Resource Server what level of access to give.

In OIDC, there is a **special scope called `openid`**. When the 3rd-party app sends `scope=openid` in its request, it is signaling to the Authorization Server:

> *"I don't want full data access. I just want to authenticate this user. Please give me an ID Token."*

The Authorization Server then generates an **ID Token in JWT format** containing only the minimal info needed for authentication — nothing more.

If you also add `scope=openid, profile`, then the JWT will additionally carry some profile-related info like name and picture.

---

### Quick Summary Before Moving to Implementation

```
REMEMBER THIS:
==============

  ID Token
  --------
  - JWT format
  - Meant ONLY for the 3rd-party app (our Spring Boot app)
  - Contains minimal user info (sub, email, name, picture)
  - Used ONLY for authentication
  - NEVER sent to Resource Server

  Access Token
  ------------
  - Opaque or JWT
  - Sent to Resource Server
  - Used for authorization (fetch protected data)
  - Understood by Resource Server (not by 3rd-party app directly)

  In this lecture → we are using OIDC
  So our Spring Boot app will receive BOTH tokens,
  but we primarily care about the ID Token for authentication.
```

---
# Spring Boot Security — OAuth2 Authentication Implementation
## Part 2: Step 1 — Registering Your Spring Boot App with the Authorization Server

---

Before any OAuth2/OIDC flow can begin, your Spring Boot app (which acts as the **3rd-party app** in this flow) must **introduce itself** to the Authorization Server. This is called **Registration**.

Think of it like this — before Insta can access your Gmail data, Insta must first go to Google and say: *"Hey, I am Insta. Here is who I am. Please give me credentials so I can interact with you on behalf of users."*

Only after this registration does the Authorization Server trust your app and issue it a **Client ID and Client Secret**.

---

### Why Registration is Needed

```
WITHOUT REGISTRATION:
=====================

  Spring Boot App  ----asks for token---->  Authorization Server
                                                     |
                                              "Who are you?
                                               I don't know you.
                                               Request DENIED."


WITH REGISTRATION:
==================

  Spring Boot App  ---registers itself--->  Authorization Server
                   <---Client ID + Secret--

  Now when Spring Boot App requests a token:
  Spring Boot App  ---Client ID + Secret--->  Authorization Server
                                                     |
                                              "I know you. 
                                               Here is your token."
```

---

### What You Get After Registration

```
+---------------------------+------------------------------------------+
|       What you provide    |        What you get back                 |
+---------------------------+------------------------------------------+
| App Name                  |  Client ID  (public identifier)          |
| Redirect URI              |  Client Secret  (private, keep it safe)  |
| Scope (openid, profile)   |                                          |
+---------------------------+------------------------------------------+
```

- **Client ID** — a public identifier for your app. Sent openly in requests.
- **Client Secret** — a private key. Never expose it. Used to prove your app's identity when calling the token endpoint.
- **Redirect URI** — the URL the Authorization Server will call back on your app after the user authenticates. You define this during registration and it must match exactly when the flow runs.

---

### Registration on GitLab

Go to: **GitLab → Edit Profile → Applications → Add New Application**

Fill in:
- **Name** → My SpringBoot App (anything you want)
- **Redirect URI** → `http://localhost:8080/login/oauth2/code/gitlab`
- **Scope** → select `openid` (since we only want authentication)

Once you click **Save Application**, GitLab gives you:
- **Client ID**
- **Client Secret** (copy it immediately — GitLab shows it only once for security. After that you have to renew it.)

```
GITLAB REGISTRATION SUMMARY:
=============================

  App Name     : My SpringBoot App
  Redirect URI : http://localhost:8080/login/oauth2/code/gitlab
  Scope        : openid
                 (authenticate with GitLab using OpenID Connect)
                 (also gives read-only access to user profile)

  After Save:
  -----------
  Client ID     : e224307910b1fd7b087b36076cddd114...
  Client Secret : gloas4d80d8c0b97e7807e36270769e5... (copy immediately!)
```

---

### Registration on Auth0

Go to: **Auth0 Dashboard → Applications → Create Application**

Fill in:
- **Name** → My SpringBoot App
- **Allowed Callback URLs** → `http://localhost:8080/login/oauth2/code/auth0`
- **OIDC Conformant** → ON (very important — this tells Auth0 to strictly follow OIDC spec)
- **JWT Signature Algorithm** → RS256 (asymmetric — private key signs, public key verifies)

After saving, Auth0 gives you:
- **Client ID**
- **Client Secret**

```
AUTH0 REGISTRATION SUMMARY:
============================

  App Name         : My SpringBoot App
  Callback URL     : http://localhost:8080/login/oauth2/code/auth0
  OIDC Conformant  : ON
  JWT Algorithm    : RS256 (asymmetric encryption)

  After Save:
  -----------
  Client ID     : pCDhGLi2bXTLLYb0PwPZGMVqFek6PiEx
  Client Secret : crSWtxt6uBYJ_NJjbN8GX4mBULnYB622...
```

---

### What is the Redirect URI exactly?

This is a very commonly asked concept. Let's be very clear about it.

```
REDIRECT URI — HOW IT WORKS:
==============================

  Step 1: User clicks "Login with GitLab" on your app

  Step 2: Spring Boot redirects user to GitLab login page

  Step 3: User enters username + password on GitLab
          and gives consent

  Step 4: GitLab now needs to send back the Authorization Code
          to your Spring Boot app.
          WHERE does it send it?
          --> To the Redirect URI you registered!

  Step 5: GitLab calls:
          http://localhost:8080/login/oauth2/code/gitlab?code=AUTH_CODE_HERE

  Spring Boot catches this, extracts the code,
  and continues the flow internally.


  IMPORTANT:
  The Redirect URI you set in application.properties
  MUST EXACTLY MATCH what you registered on GitLab/Auth0.
  Even a trailing slash difference will cause an error.
```

---

### What is a Registration ID?

The instructor uses the term **Registration ID** a lot in the code and config. Let's define it clearly now so it doesn't confuse you later.

```
REGISTRATION ID:
================

  When you configure multiple Authorization Servers
  in application.properties, you give each one a name.
  That name is the Registration ID.

  spring.security.oauth2.client.registration.gitlab.*
                                              ^^^^^^^
                                         Registration ID = "gitlab"

  spring.security.oauth2.client.registration.auth0.*
                                             ^^^^^
                                        Registration ID = "auth0"

  This same Registration ID appears in the Redirect URI:
  http://localhost:8080/login/oauth2/code/gitlab
                                          ^^^^^^
                                    Must match Registration ID

  And in the authorization URL Spring Boot generates:
  /oauth2/authorization/gitlab
                         ^^^^^^
                    Registration ID appended here
```

---

### Big Picture So Far

```
+----------------+                        +----------------------+
|  Spring Boot   |  --- registers with -->|  Authorization       |
|  App           |                        |  Server              |
|  (3rd party    |  <-- Client ID +-------|  (GitLab / Auth0)    |
|   app)         |      Client Secret     |                      |
+----------------+                        +----------------------+

  We registered on 2 Authorization Servers:
  1. GitLab    --> Registration ID: "gitlab"
  2. Auth0     --> Registration ID: "auth0"

  Each gave us a Client ID and Client Secret.
  We'll store these in application.properties next.
```

---
# Spring Boot Security — OAuth2 Authentication Implementation
## Part 3: The 3 Config Changes + Demo

---

After registration, you have your Client ID and Client Secret from both GitLab and Auth0. Now the instructor shows that with just **3 changes**, the entire OAuth2 flow works out of the box in Spring Boot — no custom login page, no user management, nothing. Spring Boot Security framework handles everything.

The 3 changes are:
1. `pom.xml` — add the OAuth2 client dependency
2. `application.properties` — configure both authorization servers
3. `SecurityConfig.java` — tell Spring Boot to use OAuth2 login

---

### Change 1: pom.xml

Just one dependency. This brings in all the OAuth2/OIDC filters, providers, and handlers into your project automatically.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

```
WHAT THIS DEPENDENCY DOES:
===========================

  Adds to your project:
  - OAuth2AuthorizationRequestRedirectFilter
  - OAuth2LoginAuthenticationFilter
  - OidcAuthorizationCodeAuthenticationProvider
  - DefaultLoginPageGeneratingFilter
  - InMemoryOAuth2AuthorizedClientService
  ... and many more

  You don't write any of this.
  Spring Boot brings it all in automatically
  just from this one dependency.
```

---

### Change 2: application.properties

This is the most detailed part. You configure both Authorization Servers here. Let's go through every property clearly.

```properties
#==============================================================
# GITLAB CONFIGURATION
#==============================================================

# Your Client ID from GitLab registration
spring.security.oauth2.client.registration.gitlab.client-id=e224307910b1fd7b...

# Your Client Secret from GitLab registration
spring.security.oauth2.client.registration.gitlab.client-secret=gloas4d80d8c0b97...

# Scope: openid = we only want authentication (OIDC)
spring.security.oauth2.client.registration.gitlab.scope=openid

# Grant Type: we are using Authorization Code flow
spring.security.oauth2.client.registration.gitlab.authorization-grant-type=authorization_code

# Redirect URI: must match what you registered on GitLab
# {registrationId} = gitlab (auto-filled by Spring Boot)
spring.security.oauth2.client.registration.gitlab.redirect-uri=http://localhost:8080/login/oauth2/code/gitlab

# Authorization URI: URL to redirect user to for login on GitLab
spring.security.oauth2.client.provider.gitlab.authorization-uri=https://gitlab.com/oauth/authorize

# Token URI: URL Spring Boot calls internally to exchange auth code for token
spring.security.oauth2.client.provider.gitlab.token-uri=https://gitlab.com/oauth/token

# Issuer URI: identifies who the Authorization Server is
spring.security.oauth2.client.provider.gitlab.issuer-uri=https://gitlab.com

# JWK Set URI: URL where the PUBLIC KEY is stored
# Spring Boot fetches this to VERIFY the signature of the ID Token
spring.security.oauth2.client.provider.gitlab.jwk-set-uri=https://gitlab.com/oauth/discovery/keys

#==============================================================
# AUTH0 CONFIGURATION
#==============================================================

spring.security.oauth2.client.registration.auth0.client-id=pCDhGLi2bXTLLYb0PwPZGMVqFek6PiEx
spring.security.oauth2.client.registration.auth0.client-secret=crSWtxt6uBYJ_NJjbN8GX4mBULnYB622...

# openid + profile: ID Token will also carry name, picture etc.
spring.security.oauth2.client.registration.auth0.scope=openid, profile

spring.security.oauth2.client.registration.auth0.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.auth0.redirect-uri=http://localhost:8080/login/oauth2/code/auth0

spring.security.oauth2.client.provider.auth0.authorization-uri=https://dev-g4t3m70i6jqdcl6i.us.auth0.com/authorize
spring.security.oauth2.client.provider.auth0.token-uri=https://dev-g4t3m70i6jqdcl6i.us.auth0.com/oauth/token
spring.security.oauth2.client.provider.auth0.issuer-uri=https://dev-g4t3m70i6jqdcl6i.us.auth0.com/
spring.security.oauth2.client.provider.auth0.jwk-set-uri=https://dev-g4t3m70i6jqdcl6i.us.auth0.com/.well-known/jwks.json
```

---

### Understanding Each Property — What Does It Do?

```
PROPERTY BREAKDOWN:
===================

  .client-id          --> Your app's public identity.
                          Sent openly to Authorization Server.

  .client-secret      --> Your app's private proof of identity.
                          Sent only when calling the token endpoint.

  .scope              --> What you are asking for.
                          "openid" = just authentication (ID Token)
                          "openid, profile" = auth + name/picture too

  .authorization-     --> Which OAuth2 flow to use.
   grant-type            We use "authorization_code" (most secure)

  .redirect-uri       --> Where Authorization Server sends the
                          authorization code after user logs in.
                          Must match registration exactly.

  .authorization-uri  --> URL your app redirects the USER to,
                          so they can log in on GitLab/Auth0.

  .token-uri          --> URL Spring Boot calls INTERNALLY
                          (user never sees this) to exchange
                          authorization code for tokens.

  .issuer-uri         --> Who is the Authorization Server?
                          Used for validation purposes.

  .jwk-set-uri        --> Where is the PUBLIC KEY stored?
                          Spring Boot fetches this to verify
                          the JWT signature of the ID Token.
                          (Authorization Server signs with
                           private key, we verify with public key)
```

---

### Key Insight: These URIs Are Standard

```
IMPORTANT OBSERVATION:
======================

  For ANY Authorization Server (GitLab, Auth0, Google, etc.)
  the URI structure remains almost identical.

  Only this part changes:
  -----------------------
  https://gitlab.com/oauth/authorize
           ^^^^^^^^^
           just the domain changes

  https://dev-xyz.us.auth0.com/oauth/authorize
           ^^^^^^^^^^^^^^^^^^^
           just the domain changes

  The endpoints (/oauth/authorize, /oauth/token, etc.)
  remain the same across providers.

  So once you understand this config for 2 providers,
  you can configure ANY provider the same way.
```

---

### Change 3: SecurityConfig.java

Just tell Spring Boot: use OAuth2 login. That's it.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http.authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated())   // all requests need auth
            .csrf(csrf -> csrf.disable())
            .oauth2Login(Customizer.withDefaults()); // USE OAUTH2 LOGIN

        return http.build();
    }
}
```

```
WHAT .oauth2Login(Customizer.withDefaults()) DOES:
===================================================

  Tells Spring Boot Security:
  - Don't use form-based login
  - Don't use HTTP Basic
  - Use OAuth2 / OIDC login
  - Generate the default login page automatically
  - Handle all the redirects, token exchange, session
    creation automatically

  Spring Boot reads your application.properties
  and wires everything up behind the scenes.
```

---

### Basic Controller (Just for Testing)

```java
@RestController
public class UserDetailsController {

    @GetMapping("/")
    public String defaultHomePageMethod() {
        return "hello, you are logged in";
    }

    @GetMapping("/users")
    public String getUsersDetails() {
        return "fetched the details successfully";
    }
}
```

---

### Demo — What Happens When You Run It

```
DEMO FLOW:
==========

  1. Start Spring Boot app (localhost:8080)

  2. Hit localhost:8080 in browser
     --> Redirected to /login automatically
     --> Default login page appears:

         +----------------------------------+
         |      Login with OAuth 2.0        |
         |                                  |
         |  https://dev-xyz.us.auth0.com/   |  <-- Auth0
         |  https://gitlab.com              |  <-- GitLab
         +----------------------------------+

     (These two links come from your application.properties)

  3. Click Auth0
     --> Redirected to Auth0 login page
     --> Enter email + password
     --> Give consent

  4. Auth0 redirects back to your app
     --> You see: "hello, you are logged in"

  5. Now hit localhost:8080/users
     --> You see: "fetched the details successfully"
     --> No login asked again! Session is active.
```

---

### The Twist — Postman Doesn't Work!

```
BROWSER vs POSTMAN:
====================

  Browser:
  --------
  Hit /users --> Works fine!
  Because Spring Boot created an HTTP SESSION in the browser.
  Session ID is stored in the cookie automatically.
  Every subsequent request carries this cookie.
  Spring Boot sees the session --> grants access.

  Postman:
  --------
  Hit /users --> Redirected back to login page!
  Because Postman does NOT carry the session cookie.
  Spring Boot sees no session --> treats as unauthenticated.


  WHY?
  ====
  Spring Boot assumes OAuth2 login happens on a browser.
  So by default, it creates a STATEFUL SESSION.
  This works for browsers but NOT for API clients like Postman.

  ALSO a bigger problem:
  Spring Boot validates the SESSION, not the TOKEN,
  on every request. So even if the ID Token expires,
  if the session is still alive, access is still granted!
  This is a security risk.

  SOLUTION:
  Make it STATELESS.
  Return the ID Token in the response.
  Client passes the token with every request.
  We validate the token on every request.
  --> We'll do this in Part 5.
```

---

### Full Picture After Part 3

```
3 CHANGES SUMMARY:
==================

  pom.xml
  -------
  + spring-boot-starter-oauth2-client
    (brings all OAuth2/OIDC filters & providers)

  application.properties
  ----------------------
  + GitLab config (client-id, secret, scope, URIs)
  + Auth0 config  (client-id, secret, scope, URIs)

  SecurityConfig.java
  -------------------
  + .oauth2Login(Customizer.withDefaults())
    (tells Spring Boot to use OAuth2 login)

  RESULT:
  -------
  Complete OAuth2/OIDC flow works out of the box.
  No user table needed in Spring Boot DB.
  Users are managed entirely by GitLab / Auth0.
  Spring Boot just trusts their authentication.
```

---
# Spring Boot Security — OAuth2 Authentication Implementation
## Part 4: Internal Flow Deep-Dive — What Happens Behind the Scenes

---

This is the most important part of the lecture. The instructor goes deep into what actually happens inside Spring Boot Security when you click "Login with GitLab". You never write this code — but understanding it is critical, both for debugging and for interviews.

The instructor breaks this into **4 stages**. We'll follow the same order.

---

## Stage 1: Generating the Login Page

When you hit `localhost:8080`, Spring Boot needs to show you the login page with the list of Authorization Servers.

```
STAGE 1 — WHO GENERATES THE LOGIN PAGE?
========================================

  Request: GET localhost:8080
                    |
                    v
            [Tomcat - Servlet Container]
                    |
                    v
            [Filter Chain]
                    |
                    v
      [Security Filter Chain]
                    |
                    v
  +------------------------------------------+
  |   DefaultLoginPageGeneratingFilter        |
  |                                           |
  |   Method: generateLoginPageHtml()         |
  |                                           |
  |   Checks: is OAuth2 login enabled?        |
  |   YES --> iterate over all registered     |
  |           Authorization Servers from      |
  |           application.properties          |
  |                                           |
  |   Builds HTML with links:                 |
  |   - https://dev-xyz.us.auth0.com/  (auth0)|
  |   - https://gitlab.com            (gitlab)|
  +------------------------------------------+
                    |
                    v
         Login page shown to user:

         +----------------------------------+
         |      Login with OAuth 2.0        |
         |                                  |
         |  https://dev-xyz.us.auth0.com/   |
         |  https://gitlab.com              |
         +----------------------------------+

  WHERE DOES IT GET THE LIST FROM?
  =================================
  At application startup, Spring Boot reads
  application.properties and fills up a map:

  OAuth2AuthorizationRequestResolver
     --> clientRegistrationRepository
         --> contains all registration IDs
             (gitlab, auth0, ...)

  DefaultLoginPageGeneratingFilter
  just iterates over this map and renders the links.
```

---

## Stage 2: User Clicks an Authorization Server Link

When the user clicks "GitLab", a specific URL gets invoked and a filter catches it.

```
STAGE 2 — USER CLICKS "GITLAB"
================================

  URL invoked:
  /oauth2/authorization/gitlab
                         ^^^^^^
                    Registration ID

  (If user clicks Auth0, it would be:
   /oauth2/authorization/auth0)

                    |
                    v
            [Filter Chain]
                    |
                    v
  +----------------------------------------------+
  |  OAuth2AuthorizationRequestRedirectFilter     |
  |                                               |
  |  Catches: /oauth2/authorization/{regId}       |
  |                                               |
  |  Builds Authorization Request containing:     |
  |  - client_id (from application.properties)    |
  |  - redirect_uri                               |
  |  - scope (openid)                             |
  |  - response_type=code                         |
  |  - state (random value, CSRF protection)      |
  |                                               |
  |  Redirects USER to Authorization Server:      |
  |  https://gitlab.com/oauth/authorize?          |
  |    client_id=...&                             |
  |    redirect_uri=...&                          |
  |    scope=openid&                              |
  |    response_type=code                         |
  +----------------------------------------------+
                    |
                    v
         User lands on GitLab login page.
         Enters username + password.
         Gives consent to Spring Boot app.

  NOTE:
  This filter's job ends here.
  It redirected the user. Its work is done.
  Now GitLab takes over.
```

---

## Stage 3: Authorization Server Sends Back the Code

After the user logs in on GitLab, GitLab calls your Redirect URI with the Authorization Code.

```
STAGE 3 — GITLAB CALLS YOUR REDIRECT URI
==========================================

  GitLab calls:
  http://localhost:8080/login/oauth2/code/gitlab?code=AUTH_CODE_HERE
                                           ^^^^^^
                                      Registration ID

                    |
                    v
            [Filter Chain]
                    |
                    v
  +----------------------------------------------+
  |      OAuth2LoginAuthenticationFilter          |
  |                                               |
  |  Catches: /login/oauth2/code/{regId}          |
  |                                               |
  |  Step 1: Extracts authorization code          |
  |          from the request                     |
  |                                               |
  |  Step 2: Creates authentication object:       |
  |  OAuth2LoginAuthenticationToken(              |
  |      clientRegistration,                      |
  |      authorizationCode,   <-- code from URL   |
  |      redirectUri,                             |
  |      scope                                    |
  |  )                                            |
  |                                               |
  |  Step 3: Passes to AuthenticationManager      |
  +----------------------------------------------+
                    |
                    v
          [AuthenticationManager]
                    |
                    v
  Finds the right AuthenticationProvider
  that can handle this token + this scope

                    |
                    v
  +----------------------------------------------+
  |  OidcAuthorizationCodeAuthenticationProvider  |
  |                                               |
  |  WHY this provider?                           |
  |  - Scope contains "openid"                    |
  |  - Token is OAuth2LoginAuthenticationToken    |
  |  --> This provider supports both              |
  |                                               |
  |  Step 1: Calls Token URI of GitLab INTERNALLY |
  |          (user never sees this)               |
  |                                               |
  |  POST https://gitlab.com/oauth/token          |
  |       client_id=...                           |
  |       client_secret=...                       |
  |       code=AUTH_CODE_HERE                     |
  |       grant_type=authorization_code           |
  |                                               |
  |  Step 2: GitLab responds with:                |
  |  {                                            |
  |    "access_token": "opaque_token_here",       |
  |    "id_token": "eyJhbGci...(JWT)...",         |
  |    "refresh_token": "..."                     |
  |  }                                            |
  |                                               |
  |  Step 3: Extracts the ID Token (JWT)          |
  |          We are mainly interested in this     |
  |          for authentication.                  |
  |                                               |
  |  Step 4: Returns back to the filter           |
  +----------------------------------------------+
```

---

### What's Inside the Access Token vs ID Token?

```
ACCESS TOKEN (from GitLab):
============================
  - NOT a JWT (no dots separating header.payload.signature)
  - It is an OPAQUE token
  - Only GitLab's Resource Server understands it
  - We (Spring Boot app) don't decode it

ID TOKEN (from GitLab):
========================
  - IS a JWT (3 parts separated by dots)
  - Header.Payload.Signature
  - We can decode and read the payload

  Decoded ID Token payload contains:
  {
    "sub":      "unique_user_id",
    "name":     "James",
    "email":    "james@example.com",
    "picture":  "https://...",
    "iss":      "https://gitlab.com",
    "aud":      "our_client_id",
    "exp":      1234567890,
    "iat":      1234567800
  }

  This is all the info needed for authentication.
  Nothing more is needed.
  No extra API call required.
```

---

## Stage 4: Back in the Filter — Session Creation

After the provider returns the ID Token, control goes back to `OAuth2LoginAuthenticationFilter`.

```
STAGE 4 — SESSION CREATION & SECURITY CONTEXT
===============================================

  OAuth2LoginAuthenticationFilter receives
  the authentication result (with ID Token)
                    |
                    v
  +----------------------------------------------+
  |      OAuth2LoginAuthenticationFilter          |
  |  (continuing after provider returns)          |
  |                                               |
  |  Step 1: Store tokens in memory               |
  |  InMemoryOAuth2AuthorizedClientService        |
  |  stores:                                      |
  |  - Access Token                               |
  |  - ID Token                                   |
  |  - Refresh Token                              |
  |  (Can override this to store in DB instead)   |
  |                                               |
  |  Step 2: Create HTTP Session                  |
  |  - Generates a Session ID (JSESSIONID)        |
  |  - Stores it in browser cookie                |
  |                                               |
  |  Step 3: Store Authentication object          |
  |  SecurityContextHolder                        |
  |    .getContext()                              |
  |    .setAuthentication(auth)                   |
  |                                               |
  |  Step 4: Call onAuthenticationSuccess()       |
  |  (default: just does cleanup tasks)           |
  |                                               |
  |  Step 5: Redirect to default page             |
  |  localhost:8080/  --> "hello, you are         |
  |                         logged in"            |
  +----------------------------------------------+


  NOW WHEN USER HITS /users:
  ==========================

  Request: GET /users
                |
                v
  +----------------------------------------------+
  |      SecurityContextHolderFilter              |
  |                                               |
  |  Passes request to:                           |
  |  HttpSessionSecurityContextRepository         |
  |                                               |
  |  Looks for JSESSIONID cookie in request       |
  |  Finds it --> Fetches SecurityContext         |
  |  from that session                            |
  |                                               |
  |  Stores it in SecurityContextHolder           |
  |  --> Authentication object is now available  |
  |      for the entire request lifecycle         |
  +----------------------------------------------+
                |
                v
          Access GRANTED to /users
```

---

## Complete End-to-End Internal Flow

```
COMPLETE INTERNAL FLOW DIAGRAM:
================================

  USER                SPRING BOOT APP              GITLAB
   |                        |                         |
   |--GET localhost:8080/-->|                         |
   |                        |                         |
   |         DefaultLoginPageGeneratingFilter fires   |
   |<--login page (auth0, gitlab links)---------------|
   |                        |                         |
   |--clicks "GitLab"------>|                         |
   |   /oauth2/authorization/gitlab                   |
   |                        |                         |
   |     OAuth2AuthorizationRequestRedirectFilter     |
   |     builds auth request with client_id, scope    |
   |                        |                         |
   |<--redirect to GitLab login page------------------|
   |                        |                         |
   |--enters username+password on GitLab------------->|
   |                        |                         |
   |                        |<--GitLab calls redirect-|
   |                        |   URI with auth code    |
   |                        |   /login/oauth2/code/   |
   |                        |   gitlab?code=XYZ       |
   |                        |                         |
   |     OAuth2LoginAuthenticationFilter catches it   |
   |     Creates OAuth2LoginAuthenticationToken       |
   |     Passes to AuthenticationManager              |
   |                        |                         |
   |     OidcAuthorizationCodeAuthenticationProvider  |
   |     handles it                                   |
   |                        |                         |
   |                        |--POST /oauth/token----->|
   |                        |   client_id             |
   |                        |   client_secret         |
   |                        |   code=XYZ              |
   |                        |                         |
   |                        |<--access_token,---------|
   |                        |   id_token (JWT),       |
   |                        |   refresh_token         |
   |                        |                         |
   |     Filter stores tokens in InMemoryService      |
   |     Creates HTTP Session (JSESSIONID)            |
   |     Stores auth in SecurityContextHolder         |
   |     Calls onAuthenticationSuccess()              |
   |                        |                         |
   |<--redirect to localhost:8080/ "logged in"--------|
   |                        |                         |
   |--GET /users----------->|                         |
   |                        |                         |
   |     SecurityContextHolderFilter fires            |
   |     Finds JSESSIONID cookie                      |
   |     Fetches session --> SecurityContext          |
   |     Auth object found --> Access granted         |
   |                        |                         |
   |<--"fetched details successfully"-----------------|
```

---

### Why Postman Fails (Recap from a Filter Perspective)

```
POSTMAN PROBLEM — FILTER LEVEL:
================================

  Postman hits GET /users

  SecurityContextHolderFilter fires
        |
        v
  HttpSessionSecurityContextRepository
  looks for JSESSIONID in request cookie
        |
        v
  NOT FOUND (Postman doesn't carry browser cookies)
        |
        v
  No SecurityContext found
  No Authentication object
        |
        v
  Spring treats request as UNAUTHENTICATED
        |
        v
  Redirected back to /login page

  ALSO NOTE:
  Even in browser, once session is set,
  Spring validates the SESSION not the TOKEN.
  So if ID Token expires but session is alive,
  access is still granted --> Security risk!
```

---
# Spring Boot Security — OAuth2 Authentication Implementation
## Part 5: Making it Stateless — CustomSuccessHandler + OAuthValidationFilter

---

Now we solve the problem from Part 4. The goal is:

1. After successful login, **return the ID Token in the response** instead of creating a session
2. With every subsequent request, **client passes the token** in the Authorization header
3. We **validate the token** on every request instead of relying on a session

This requires **2 additions** on top of what we already have:
- `CustomOAuth2SuccessHandler` — to return the ID Token in the response after login
- `OAuthValidationFilter` — to validate the token on every incoming request
- Updated `SecurityConfig` — to wire both together and make session stateless

---

### The Problem Visualized

```
CURRENT FLOW (STATEFUL) - PROBLEM:
====================================

  Login succeeds
       |
       v
  Filter creates HTTP Session
  Stores JSESSIONID in browser cookie
       |
       v
  Every request --> session validated
  Token NEVER validated again!

  Problems:
  ---------
  1. Postman/API clients don't carry cookies
     --> Can't access any API after login

  2. ID Token may expire, but session still active
     --> Expired token user still gets access
     --> Security risk!

  3. OAuth is stateful by default in Spring Boot
     --> Not suitable for REST APIs


DESIRED FLOW (STATELESS) - SOLUTION:
======================================

  Login succeeds
       |
       v
  Return ID Token in response body
  NO session created
       |
       v
  Client stores the token
       |
       v
  Every request --> client sends token
  in Authorization: Bearer <token> header
       |
       v
  OAuthValidationFilter validates token
  on EVERY request
  --> Access granted only if token is valid
```

---

### Addition 1: CustomOAuth2SuccessHandler

The instructor looks inside `OAuth2LoginAuthenticationFilter` and finds that after authentication succeeds, it calls a method called `onAuthenticationSuccess()`. By default, this method just does some cleanup tasks. We **override this method** to also return the ID Token in the response body.

```
WHERE IN THE FILTER DOES THIS HOOK EXIST?
==========================================

  OAuth2LoginAuthenticationFilter
       |
       v
  attemptAuthentication()   <-- handles auth code, calls provider
       |
       v
  [OidcProvider fetches tokens from Authorization Server]
       |
       v
  sessionStrategy.onAuthentication()  <-- creates session (can't remove)
       |
       v
  SecurityContextHolder.setAuthentication()  <-- stores auth object
       |
       v
  successHandler.onAuthenticationSuccess()   <-- THIS IS OUR HOOK!
  (default: just cleanup)
  (we override: return ID Token in response body)
```

```java
@Component
public class CustomOAuth2SuccessHandler 
        implements AuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService clientService;

    @Autowired
    public CustomOAuth2SuccessHandler(
            OAuth2AuthorizedClientService clientService) {
        this.clientService = clientService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        // Step 1: Cast authentication to OAuth2AuthenticationToken
        OAuth2AuthenticationToken authToken =
                (OAuth2AuthenticationToken) authentication;

        // Step 2: Load the authorized client
        // (contains access token, refresh token info)
        OAuth2AuthorizedClient client =
                clientService.loadAuthorizedClient(
                        authToken.getAuthorizedClientRegistrationId(),
                        authToken.getName());

        if (client != null) {

            String idToken = null;

            // Step 3: Check if principal is OidcUser
            // (it will be, because we used openid scope)
            if (authToken.getPrincipal() instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) authToken.getPrincipal();

                // Step 4: Extract the ID Token value (JWT string)
                idToken = oidcUser.getIdToken().getTokenValue();
            }

            // Step 5: Write the ID Token into the response body as JSON
            response.setContentType("application/json");
            response.getWriter().write(
                    "{ \"id_token\": \"" + idToken + "\" }");
            response.getWriter().flush();

        } else {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Authorization failed");
        }
    }
}
```

```
WHAT THIS CLASS DOES - STEP BY STEP:
======================================

  1. Implements AuthenticationSuccessHandler
     --> gives us the onAuthenticationSuccess() hook

  2. Gets the Authentication object
     --> cast to OAuth2AuthenticationToken
     --> this has all user info + token info

  3. Loads the authorized client
     --> OAuth2AuthorizedClientService is the in-memory
         store where Spring Boot saved the tokens
         after the provider fetched them

  4. Checks if principal is OidcUser
     --> Since scope=openid, principal WILL be OidcUser
     --> OidcUser has getIdToken() method
     --> getIdToken().getTokenValue() gives us the JWT string

  5. Writes the ID Token as JSON in response body
     --> Client receives this after login
     --> Client stores it and uses it for future requests
```

---

### Addition 2: OAuthValidationFilter

Now we need a filter that runs on **every incoming request**, extracts the token from the `Authorization` header, validates it, and if valid, sets the authentication in `SecurityContextHolder`.

```
WHERE THIS FILTER SITS IN THE CHAIN:
======================================

  Incoming Request
       |
       v
  [Filter Chain]
       |
       v
  OAuthValidationFilter   <-- our custom filter
  (runs BEFORE UsernamePasswordAuthenticationFilter)
       |
       v
  Extracts Bearer token from Authorization header
       |
       v
  Validates token using OAuthTokenValidatorUtil
       |
       +-- INVALID --> return 401 Unauthorized
       |
       +-- VALID --> set Authentication in
                     SecurityContextHolder
                          |
                          v
                     Rest of filter chain continues
                          |
                          v
                     Controller reached
                     Access GRANTED
```

```java
public class OAuthValidationFilter 
        extends OncePerRequestFilter {

    private final OAuthTokenValidatorUtil tokenValidatorUtil;

    @Autowired
    public OAuthValidationFilter(
            OAuthTokenValidatorUtil tokenValidatorUtil) {
        this.tokenValidatorUtil = tokenValidatorUtil;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Step 1: Extract JWT from Authorization header
        String token = extractJwtFromRequest(request);

        if (token != null) {

            // Step 2: Validate the token
            String username = tokenValidatorUtil.isTokenValid(token);

            if (StringUtil.isNullOrEmpty(username)) {
                // Token invalid or expired
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid or expired token");
                return;
            }

            // Step 3: Token valid --> set authentication
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    username, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        // Step 4: Continue the filter chain
        filterChain.doFilter(request, response);
    }

    // Helper: extract token from "Authorization: Bearer <token>"
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

### The Token Validator Utility

This is the class that actually **validates the ID Token**. The key insight here is: the ID Token was **signed by the Authorization Server using its private key**. We need the **public key** to verify that signature. The JWK Set URI in `application.properties` is exactly where that public key lives.

```
HOW TOKEN VALIDATION WORKS:
=============================

  ID Token (JWT) is signed by GitLab/Auth0
  using their PRIVATE KEY (RS256 algorithm)

  To verify:
  - We need their PUBLIC KEY
  - Public key is available at the JWK Set URI
    (configured in application.properties)

  Spring Boot's JwtDecoder does this for us:
  - We give it the issuer (from the token itself)
  - It finds the JWK Set URI from application.properties
  - Fetches the public key
  - Verifies the token signature
  - Decodes the claims
  - Returns the Jwt object

  If signature is invalid --> decode throws exception
  If token is expired     --> decode throws exception
  If valid                --> returns Jwt with all claims
```

```java
@Component
public class OAuthTokenValidatorUtil {

    public String isTokenValid(String accessToken) {

        // Step 1: Find who issued this token
        // (extracts "iss" claim from JWT payload)
        String issuer = getIssuerIdFromToken(accessToken);

        // Step 2: Create a JwtDecoder for this issuer
        // Spring Boot finds the JWK Set URI from application.properties
        // based on the issuer, fetches public key, verifies signature
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);

        // Step 3: Decode + verify the token
        // Throws exception if invalid or expired
        Jwt jwt = decoder.decode(accessToken);

        // Step 4: Return the subject (unique user ID)
        if (jwt != null) {
            return (String) jwt.getClaims().get("sub");
        }
        return null;
    }

    // Helper: extract "iss" claim from JWT without full validation
    public static String getIssuerIdFromToken(String jwtToken) {
        try {
            // JWT has 3 parts: header.payload.signature
            String[] parts = jwtToken.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT token.");
            }

            // Decode the payload (Base64 URL encoded)
            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(parts[1]));

            // Parse JSON to get the "iss" field
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payloadMap =
                    mapper.readValue(payloadJson, Map.class);

            return (String) payloadMap.get("iss");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
```

```
WHY DO WE EXTRACT ISSUER FIRST?
=================================

  We have 2 Authorization Servers: GitLab and Auth0
  Each has a DIFFERENT JWK Set URI (different public key)

  So before validating, we need to know:
  "Who issued this token?"

  We get this from the "iss" claim inside the JWT payload.
  JWT payload is just Base64 encoded -- readable without
  a key (signature verification is separate).

  Once we know the issuer:
  - JwtDecoders.fromIssuerLocation(issuer)
  - Spring Boot matches issuer to the right
    jwk-set-uri in application.properties
  - Fetches correct public key
  - Verifies signature

  This works for ANY number of Authorization Servers
  as long as they are configured in application.properties.
```

---

### Updated SecurityConfig — Wiring Everything Together

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private OAuthTokenValidatorUtil tokenValidatorUtil;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CustomOAuth2SuccessHandler successHandler) throws Exception {

        http.authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated())

            // Make session STATELESS
            // Spring Boot will NOT create HTTP sessions
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .csrf(csrf -> csrf.disable())

            // Use OAuth2 login with our custom success handler
            .oauth2Login(oauth -> oauth
                .successHandler(successHandler))  // <-- our handler

            // Add our validation filter BEFORE the default
            // username/password filter in the chain
            .addFilterBefore(
                new OAuthValidationFilter(tokenValidatorUtil),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

```
WHAT CHANGED FROM PREVIOUS SecurityConfig:
============================================

  BEFORE:
  -------
  .oauth2Login(Customizer.withDefaults())
  --> Default success handler (just cleanup)
  --> Session created (STATEFUL)

  AFTER:
  ------
  .sessionManagement(STATELESS)
  --> No HTTP sessions created

  .oauth2Login(oauth -> oauth.successHandler(successHandler))
  --> Our custom handler returns ID Token in response

  .addFilterBefore(OAuthValidationFilter, ...)
  --> Validates Bearer token on every request
  --> Placed BEFORE UsernamePasswordAuthenticationFilter
      so it runs early in the chain
```

---

### Complete Stateless Flow — End to End

```
STATELESS FLOW DIAGRAM:
========================

PART A — LOGIN:

  User clicks "GitLab" on login page
       |
       v
  [Same flow as before -- Part 4 Stages 1,2,3]
  GitLab returns ID Token + Access Token
       |
       v
  OAuth2LoginAuthenticationFilter
       |
       v
  Stores tokens in InMemoryService (still happens)
  Creates session (still happens -- can't prevent)
       |
       v
  Calls successHandler.onAuthenticationSuccess()
       |
       v
  CustomOAuth2SuccessHandler
  --> Extracts ID Token from OidcUser
  --> Writes to response body:
      { "id_token": "eyJhbGci....(JWT)...." }
       |
       v
  CLIENT receives the ID Token
  Client stores it (localStorage, variable, etc.)


PART B — SUBSEQUENT REQUESTS:

  Client sends:
  GET /users
  Authorization: Bearer eyJhbGci....(ID Token)....
       |
       v
  [Filter Chain]
       |
       v
  OAuthValidationFilter (runs first)
       |
       v
  Extracts token from Authorization header
       |
       v
  OAuthTokenValidatorUtil.isTokenValid(token)
       |
       v
  getIssuerIdFromToken() --> "https://gitlab.com"
       |
       v
  JwtDecoders.fromIssuerLocation("https://gitlab.com")
  --> finds jwk-set-uri from application.properties
  --> fetches public key from GitLab
  --> verifies JWT signature
  --> checks expiry
       |
       +-- INVALID/EXPIRED --> 401 Unauthorized
       |
       +-- VALID
              |
              v
         Extract "sub" claim (unique user ID)
         Create UsernamePasswordAuthenticationToken
         Set in SecurityContextHolder
              |
              v
         filterChain.doFilter() --> continues
              |
              v
         Controller reached
         GET /users --> "fetched the details successfully"
```

---

### Comparison: Stateful vs Stateless

```
+-------------------------+------------------+----------------------+
|       Aspect            |    STATEFUL      |     STATELESS        |
+-------------------------+------------------+----------------------+
| Session created?        | YES              | NO                   |
+-------------------------+------------------+----------------------+
| Works in browser?       | YES              | YES                  |
+-------------------------+------------------+----------------------+
| Works in Postman/API    | NO               | YES                  |
| clients?                |                  |                      |
+-------------------------+------------------+----------------------+
| Token validated on      | NO (session      | YES (every request)  |
| every request?          | validated only)  |                      |
+-------------------------+------------------+----------------------+
| Expired token risk?     | YES              | NO                   |
+-------------------------+------------------+----------------------+
| Suitable for REST API?  | NO               | YES                  |
+-------------------------+------------------+----------------------+
| What client sends?      | Cookie           | Authorization:       |
|                         | (JSESSIONID)     | Bearer <token>       |
+-------------------------+------------------+----------------------+
```

---

### Interview Tips

```
+----------------------------------------------------------+
|                   INTERVIEW TIPS                         |
+----------------------------------------------------------+
|                                                          |
|  Q: What is the difference between OAuth2 and OIDC?      |
|  A: OAuth2 is for authorization (access to protected     |
|     data). OIDC is a layer on top of OAuth2 for          |
|     authentication. OIDC introduces the ID Token (JWT)   |
|     which contains user identity info.                   |
|                                                          |
|  Q: Why is OAuth2 stateful by default in Spring Boot?    |
|  A: Spring Boot assumes OAuth2 login happens in a        |
|     browser, so it creates an HTTP session after         |
|     successful login. Session ID is stored in cookie.    |
|                                                          |
|  Q: How do you make OAuth2 stateless in Spring Boot?     |
|  A: Set SessionCreationPolicy.STATELESS in               |
|     SecurityConfig. Implement a custom                   |
|     AuthenticationSuccessHandler to return the ID Token  |
|     in the response. Add a custom OncePerRequestFilter   |
|     to validate the Bearer token on every request.       |
|                                                          |
|  Q: What is the JWK Set URI?                             |
|  A: It is the URL where the Authorization Server's       |
|     public key is published. Spring Boot fetches this    |
|     public key to verify the JWT signature of the        |
|     ID Token. Authorization Server signs with private    |
|     key; we verify with public key (RS256).              |
|                                                          |
|  Q: Why do we extract the issuer from the token first?   |
|  A: Because we may have multiple Authorization Servers   |
|     (GitLab, Auth0). Each has a different public key.    |
|     We read the "iss" claim from the JWT payload to      |
|     identify who issued the token, then load the         |
|     correct JWK Set URI from application.properties      |
|     to fetch the right public key for verification.      |
|                                                          |
|  Q: What is onAuthenticationSuccess() and why did we     |
|     override it?                                         |
|  A: It is a hook method in                               |
|     OAuth2LoginAuthenticationFilter called after         |
|     successful authentication. Default implementation    |
|     just does cleanup. We overrode it to write the       |
|     ID Token into the HTTP response body so clients      |
|     can use it for stateless token-based authentication. |
|                                                          |
+----------------------------------------------------------+
```

---

**All 5 parts done, James!** The complete lecture is covered — OAuth2 vs OIDC, registration, the 3 config changes, internal filter flow, and making it stateless. Let me know if you want to revisit any part or go deeper on anything!