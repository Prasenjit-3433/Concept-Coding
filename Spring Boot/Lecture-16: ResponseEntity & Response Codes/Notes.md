# Step 1 — What is a Response?

---

## Why are we learning this?

The instructor mentions that before jumping into **Exception Handling** in Spring Boot, we need to have a solid understanding of **responses** and **response codes** — because exception handling is deeply tied to what kind of response you send back when something goes wrong. So this is a foundation topic.

---

## A Response has 3 Parts

Every HTTP response that goes from your server back to the client is made up of 3 things:

```
┌─────────────────────────────────────────────────┐
│                  HTTP RESPONSE                  │
│                                                 │
│  1. STATUS CODE  →  200, 201, 404, 500 etc.     │
│                                                 │
│  2. HEADER       →  Additional info (optional)  │
│                     e.g. content-type,          │
│                     custom headers              │
│                                                 │
│  3. BODY         →  Actual data you send back   │
│                     e.g. User object, message   │
└─────────────────────────────────────────────────┘
```

- **Status Code** — tells the client *what happened* with their request. Was it successful? Did something go wrong? Was the resource not found? Each number means something specific.
- **Header** — optional extra information. Things like content type, authentication tokens, custom metadata, etc.
- **Body** — the actual data payload. Could be a User object, a list, a message string, or even nothing at all (in cases like delete).

---

## How do you send a Response in Spring Boot?

Spring Boot gives you a class called **`ResponseEntity<T>`** to build and send responses.

The `T` here is a **generic type** — it represents the **type of your body**.

- If `T` is `String` → your body can only be a String
- If `T` is `User` → your body has to be a User object
- If `T` is `Void` → you're not sending any body

```
ResponseEntity<T>
        │
        └──> T = type of the Body
```

---

## Basic Example

```java
@GetMapping(path = "/get-user")
public ResponseEntity<String> getUser() {

    return ResponseEntity.ok("My Response body Object can go here");
}
```

Here `.ok(...)` is a shortcut — it sets the status to **200 OK** and puts your string as the body.

---

## With Headers + Status + Body (full form)

```java
@GetMapping(path = "/get-user")
public ResponseEntity<String> getUser() {

    HttpHeaders headers = new HttpHeaders();
    headers.add("My-Header1", "SomeValue1");
    headers.add("My-Header2", "SomeValue2");

    return ResponseEntity
              .status(HttpStatus.OK)   // set status
              .headers(headers)        // set headers
              .body("My Response body Object can go here"); // set body
}
```

You create an `HttpHeaders` object, add your key-value pairs into it, and pass it while building the response.

---

## Flow so far

```
Client sends Request
        │
        ▼
  Controller Method
        │
        ▼
  Build ResponseEntity
  ┌─────────────────┐
  │ .status(...)    │  ← What happened?
  │ .headers(...)   │  ← Any extra info?
  │ .body(...)      │  ← What data to send?
  └─────────────────┘
        │
        ▼
  Response sent back to Client
```

---

That's Step 1 — clean and simple. The instructor's whole point here is: **a response is not just data, it's data + status + headers**, and `ResponseEntity` is the tool Spring Boot gives you to control all three.

# Step 2 — ResponseEntity Deep Dive

---

## Why does `.body()` always come last?

If you look at the full form again:

```java
return ResponseEntity
          .status(HttpStatus.OK)
          .headers(headers)
          .body("My Response body here");
```

You might wonder — why can't I do `.body()` first and then `.headers()`? Why is `.body()` always at the end?

The instructor explains this clearly — **it's the Builder Design Pattern.**

---

## Builder Pattern inside ResponseEntity

```
ResponseEntity.status(...)
      │
      │  returns BodyBuilder object
      ▼
.headers(...)
      │
      │  returns BodyBuilder object (still building...)
      ▼
.body(...)
      │
      │  returns final ResponseEntity<T> object  ✅
      ▼
   DONE — response is ready to send
```

- `.status()` → returns a **BodyBuilder** (not the final object yet, still building)
- `.headers()` → returns a **BodyBuilder** (still building)
- `.body()` → returns the **final `ResponseEntity<T>`** object

So until you call `.body()`, you're still in the "building" phase. That's why body must always come last — because **body is what finalizes and produces the ResponseEntity.**

> 💡 The instructor points out — this is a real-world practical example of the Builder Design Pattern being used inside the Spring Framework itself.

---

## What if you don't want to send any Body?

There are many cases where you don't want to send any data in the body — the most common example is a **DELETE** operation. Request was successful, resource was deleted, nothing to return.

You can't just do `.body(null)` every time — that looks messy. So Spring gives you `.build()`.

```java
@GetMapping(path = "/get-user")
public ResponseEntity<Void> getUser() {

    HttpHeaders headers = new HttpHeaders();
    headers.add("My-Header1", "SomeValue1");

    return ResponseEntity
              .status(HttpStatus.OK)
              .headers(headers)
              .build();  // ← no body
}
```

Internally, `.build()` does exactly this:

```
.build()  →  calls .body(null) internally
```

So it's just a cleaner way of saying "I don't have a body to send."

---

## `.build()` vs `.body()` — Quick Rule

```
┌──────────────────────────────────────────────┐
│  .body("some data")  → response HAS a body   │
│                                              │
│  .build()            → response has NO body  │
│                        (internally = null)   │
└──────────────────────────────────────────────┘
```

Whenever you see `.build()` in someone's code, you now know — they're intentionally sending no body in the response.

---

## Do you ALWAYS need ResponseEntity?

No! The instructor makes this very clear.

You can directly return an object from your controller method — without wrapping it in ResponseEntity:

```java
@RestController
@RequestMapping(value = "/api/")
public class UserController {

    @GetMapping(path = "/get-user")
    public User getUser() {

        User responseObj = new User("XYZ", 28);
        return responseObj;  // directly returning object
    }
}
```

What happens here? Spring Boot **internally wraps it** in a ResponseEntity for you.

```
return responseObj;
      │
      ▼  Spring Boot wraps it
ResponseEntity<User>  with status 200 OK  (default)
```

> **Default status when you don't use ResponseEntity = 200 OK**

But if you want to **specifically control the status code** (like returning 201 Created after a POST), you must use ResponseEntity explicitly. Otherwise you're stuck with 200.

---

## What is `@ResponseBody`?

This is something the instructor says most people overlook, but it has very important meaning.

**The rule is:** When you return a plain String or a POJO (plain Java object) directly from a controller method, Spring needs to know — *"Should I treat this as a response body, or as the name of a View (like a JSP file)?"*

That's exactly what `@ResponseBody` tells Spring — **treat this return value as the response body, not as a view name.**

---

## Why didn't we use `@ResponseBody` in our examples above?

Because we used `@RestController`. Look at what's inside it:

```
@RestController
      │
      └── internally contains @Controller + @ResponseBody
```

So `@RestController` **automatically applies `@ResponseBody` to every method** in that class. You don't need to add it manually.

---

## What if you use `@Controller` instead of `@RestController`?

```java
@Controller  // ← not @RestController
@RequestMapping(value = "/api/")
public class UserController {

    @GetMapping(path = "/get-user")
    public String getUser() {
        return "XYZ";
    }
}
```

If you run this — you'll get a **404 Not Found** error. But why?

```
return "XYZ";
      │
      │  @Controller without @ResponseBody
      ▼
Spring thinks "XYZ" is a VIEW NAME
      │
      ▼
Spring tries to find a file called "XYZ"
(like XYZ.jsp or XYZ.html)
      │
      ▼
File doesn't exist → 404 Not Found ❌
```

So Spring Boot was literally searching for a file named "XYZ" — not treating it as a response body.

---

## Fix — add `@ResponseBody` explicitly

```java
@Controller
@RequestMapping(value = "/api/")
public class UserController {

    @GetMapping(path = "/get-user")
    @ResponseBody  // ← now Spring knows it's a response body
    public String getUser() {
        return "XYZ";
    }
}
```

---

## Full Picture — `@Controller` vs `@RestController`

```
┌─────────────────────────────────────────────────────┐
│  @Controller                                        │
│  → return value treated as VIEW NAME by default     │
│  → need @ResponseBody on each method manually       │
│    if you want to return data                       │
├─────────────────────────────────────────────────────┤
│  @RestController                                    │
│  → @ResponseBody applied to ALL methods             │
│    automatically                                    │
│  → return value always treated as response body     │
└─────────────────────────────────────────────────────┘
```

---

## Quick Summary of Step 2

```
┌────────────────────────────────────────────────────────┐
│  Builder Pattern  →  .status() → .headers() → .body()  │
│                      body always comes last            │
│                                                        │
│  .body(data)      →  sends response WITH body          │
│  .build()         →  sends response WITHOUT body       │
│                                                        │
│  ResponseEntity   →  optional, but needed if you want  │
│                      to control status code            │
│  Default status   →  200 OK                            │
│                                                        │
│  @RestController  →  @ResponseBody auto-applied        │
│  @Controller      →  need @ResponseBody manually       │
└────────────────────────────────────────────────────────┘
```

---

That's Step 2 done! Now we have a solid understanding of how responses are built in Spring Boot. From Step 3 onwards, we go deep into **what status codes to use and when** — starting with the **2xx Success codes.** 🚀

# Step 3 — 2xx Success Codes

---

## What does 2xx mean?

Simple — the request came in, the server received it, processed it, and everything went well. **Success.**

The instructor says there are many 2xx codes, but he covers the **5 most frequently used ones** in real projects.

---

## Overview

```
┌─────────────────────────────────────────────────────────┐
│                    2xx — SUCCESS                        │
│                                                         │
│  200  →  OK                                             │
│  201  →  Created                                        │
│  202  →  Accepted                                       │
│  204  →  No Content                                     │
│  206  →  Partial Content                                │
└─────────────────────────────────────────────────────────┘
```

---

## 200 — OK

The most commonly used status code. Used in **GET** and **POST (idempotent calls).**

**For GET** — straightforward. Request is successful, and you're returning data in the response body.

```
Client  →  GET /user/123
                │
                ▼
           Server finds user
                │
                ▼
Client  ←  200 OK + user data in body
```

**For POST (idempotent call)** — this is the interesting case the instructor explains.

> **What is an idempotent POST call?**

Let's say you have a POST API to add a new user. First call comes in — user is created, you return **201 Created** (we'll see this next). Now the **same exact request comes again** (duplicate call).

What should happen? You don't create a new row — the user already exists. So what do you return?

You return **200 OK** — meaning: *"Request is valid, I processed it, the data is already there, here it is — but no new row was created."*

```
1st POST call  →  New user created  →  201 Created
                                            │
                                            ▼
2nd POST call  →  User already exists  →  200 OK
(same request)    same data returned
                  no new row created
```

> This situation — where calling the same API multiple times gives the same result without creating duplicates — is called an **idempotent call.**

---

## 201 — Created

Used with **POST** when a brand new resource is successfully created.

```
Client  →  POST /user  (with new user data)
                │
                ▼
           Server creates new row in DB
                │
                ▼
Client  ←  201 Created + new user data in body
```

So the clear distinction between 200 and 201 in POST:

```
┌──────────────────────────────────────────────┐
│  POST - new resource created   →  201        │
│  POST - resource already existed →  200      │
└──────────────────────────────────────────────┘
```

---

## 202 — Accepted

Used with **POST** when the server has received and accepted your request — but **processing is not yet complete.**

The instructor gives a great real-world example — **batch processing like export/import.**

```
Client  →  POST /export-data
                │
                ▼
           Server accepts request
           Spawns a new background thread
           to process the export (may take
           10-15 mins for huge data)
                │
                ▼
Client  ←  202 Accepted
           (processing still happening
            in background)
```

The key point here — the server doesn't wait for the full processing to finish before responding. It says *"I got your request, I've started working on it, but I'm not done yet."*

```
┌──────────────────────────────────────────────────────┐
│  200  →  Request done, response body ready           │
│  202  →  Request accepted, still processing          │
│          (background job, batch operation, etc.)     │
└──────────────────────────────────────────────────────┘
```

---

## 204 — No Content

Used with **DELETE.** Request is successful, processing is fully complete — but **you're not returning any data in the body.**

```
Client  →  DELETE /user/123
                │
                ▼
           Server deletes the user
                │
                ▼
Client  ←  204 No Content
           (no body in response)
```

The instructor gives a clean rule here:

```
┌──────────────────────────────────────────────────────┐
│  Request successful + body returned   →  200 OK      │
│  Request successful + NO body         →  204 No      │
│                                          Content     │
└──────────────────────────────────────────────────────┘
```

> **Note:** 204 means processing is fully done — it's not like 202 where things are still happening in the background. Everything is complete, you just have nothing to return.

---

## 206 — Partial Content

Used with **POST** during **bulk operations** where some requests passed and some failed.

The instructor's example — bulk addition of 100 users:

```
Client  →  POST /users/bulk  (100 users)
                │
                ▼
           Server processes all 100
           95 users → SUCCESS ✅
            5 users → FAILED  ❌
                │
                ▼
Client  ←  206 Partial Content
           (not fully successful,
            not fully failed either)
```

This is useful when you can't say it's a full success (200/201) or a full failure (4xx/5xx) — it's somewhere in between. **Partial success.**

---

## Full 2xx Picture

```
┌──────┬──────────────────┬────────────────────────────────────────────┐
│ Code │ Reason           │ When to use                                │
├──────┼──────────────────┼────────────────────────────────────────────┤
│ 200  │ OK               │ GET success (with body)                    │
│      │                  │ POST - resource already existed (idempotent)│
├──────┼──────────────────┼────────────────────────────────────────────┤
│ 201  │ Created          │ POST - brand new resource created in DB    │
├──────┼──────────────────┼────────────────────────────────────────────┤
│ 202  │ Accepted         │ POST - request accepted, processing ongoing│
│      │                  │ (batch jobs, export/import)                │
├──────┼──────────────────┼────────────────────────────────────────────┤
│ 204  │ No Content       │ DELETE - success but no body to return     │
├──────┼──────────────────┼────────────────────────────────────────────┤
│ 206  │ Partial Content  │ POST - bulk operation, partial success     │
└──────┴──────────────────┴────────────────────────────────────────────┘
```

---

## 🎯 Interview Tips

> The instructor doesn't call these out explicitly as interview tips, but based on the depth he goes into — these are the things interviewers love to ask:

**"What's the difference between 200 and 201?"**
→ Both are success. 200 = request successful + returning existing data. 201 = request successful + **new resource was created.**

**"When would you return 200 vs 201 in a POST API?"**
→ First call creates a new resource → 201. Duplicate/idempotent call where resource already exists → 200.

**"What's the difference between 202 and 204?"**
→ 202 = accepted but **still processing** (async). 204 = fully done, just **no body** to return.

**"When do you use 206?"**
→ Bulk operations where some records passed and some failed — partial success.

---

That's Step 3 done — all 5 success codes covered with real examples!

# Step 4 — 3xx Redirection Codes

---

## What does 3xx mean?

When the server sends back a 3xx response, it's essentially telling the client — **"I can't fully serve you here, you need to go somewhere else."** The action item shifts to the client.

The instructor says 3xx codes are most frequently used when you're **migrating from a legacy API to a new API.**

---

## Overview

```
┌─────────────────────────────────────────────────────────┐
│                  3xx — REDIRECTION                      │
│                                                         │
│  301  →  Moved Permanently                              │
│  308  →  Permanent Redirect                             │
│  304  →  Not Modified                                   │
└─────────────────────────────────────────────────────────┘
```

---

## 301 — Moved Permanently

Used when an old API is **permanently moved** to a new URI. All future requests should go to the new URI.

The instructor walks through a real Postman demo for this — let's break it down step by step.

---

### The Code

```java
// OLD API
@GetMapping("/old-get-user")
public ResponseEntity<Void> oldGetUser() {

    HttpHeaders headers = new HttpHeaders();
    headers.add("Location", "/api/new-get-user"); // ← new API location

    return ResponseEntity
              .status(HttpStatus.MOVED_PERMANENTLY) // 301
              .headers(headers)
              .build();
}

// NEW API
@GetMapping("/new-get-user")
public ResponseEntity<String> newGetUser() {

    return ResponseEntity
              .status(HttpStatus.OK)
              .body("success");
}
```

---

### How does the redirection actually work?

```
Client hits  →  /old-get-user
                      │
                      ▼
              Server returns 301
              with Header:
              Location: /api/new-get-user
                      │
                      ▼
         Client reads the Location header
         and automatically follows it
                      │
                      ▼
         Client hits  →  /new-get-user
                      │
                      ▼
              Server returns 200 OK
              body: "success"
                      │
                      ▼
         Client sees 200 + "success" ✅
```

So when you hit the old API from Postman (with "auto follow redirects" ON), you directly see 200 and "success" — even though you never explicitly called the new API. Postman handles the redirect automatically, just like any HTTP client would.

---

### What happens if you turn OFF "auto follow redirects" in Postman?

```
Client hits  →  /old-get-user
                      │
                      ▼
              Server returns 301
              Header: Location: /api/new-get-user
              Body: (empty)
                      │
                      ▼
         Client sees 301 directly ← stops here
         (doesn't follow the Location header)
```

You see 301 in Postman, empty body, and the Location header sitting right there. This is exactly what the instructor showed — turning off auto-follow redirects reveals what's actually happening under the hood.

> 💡 **Key insight** — All HTTP clients (browsers, Postman, etc.) understand 3xx codes. When they see 301, they know to look at the `Location` header and follow it. That's the "action" the client takes.

---

## 308 — Permanent Redirect

308 does the **same job as 301** — permanently redirect to a new URI. But there's one important difference.

```
┌────────────────────────────────────────────────────────────┐
│  301 — Moved Permanently                                   │
│  → HTTP method CAN change during redirect                  │
│  → Old API is POST? New API can be GET. That's allowed.    │
│                                                            │
│  308 — Permanent Redirect                                  │
│  → HTTP method CANNOT change during redirect               │
│  → Old API is POST? New API MUST also be POST.             │
└────────────────────────────────────────────────────────────┘
```

The instructor says 308 is the **improved, stricter version** of 301. It enforces that the HTTP method stays the same when redirecting — which makes more sense in most real migration scenarios.

```
Example:

301 →  Old: POST /old-create-user
       New: GET  /new-get-user   ✅ allowed (but weird)

308 →  Old: POST /old-create-user
       New: POST /new-create-user  ✅ correct
       New: GET  /new-get-user     ❌ NOT allowed
```

---

## 304 — Not Modified

This one is really interesting — and the instructor specifically calls out **where a lot of engineers use this code wrongly.**

---

### What is 304 actually for?

It's a **caching optimization** mechanism. The goal is — if the client already has the latest data, don't make the server fetch and send the same data again. Save bandwidth, save processing.

Here's the full flow:

```
STEP 1 — First GET call

Client  →  GET /user/123
                │
                ▼
           Server fetches data
           Returns user data
           + Last-Modified: 13th Sept   ← in response header
                │
                ▼
Client  ←  200 OK + user data
           Client CACHES this response
           and stores "Last-Modified: 13th Sept"
```

```
STEP 2 — Second GET call (client uses cache header)

Client  →  GET /user/123
           Header: If-Modified-Since: 13th Sept
                │
                ▼
           Server checks:
           "When was this resource last updated in DB?"
                │
          ┌─────┴──────┐
          │            │
     DB has      DB updated
     13th Sept   after 13th Sept
    (no change)  (data changed)
          │            │
          ▼            ▼
    304 Not       200 OK +
    Modified      new data
    (no body)     returned
          │
          ▼
    Client uses
    its cached data
```

So 304 is purely a **GET + caching** thing. Server is saying — *"You already have the latest data. I'm not going to waste time fetching and sending it again."*

---

### ⚠️ Where engineers go wrong with 304

The instructor says he's seen many engineers use 304 incorrectly with **PATCH** calls. Here's the wrong scenario:

```
PATCH /user/123
Body: { name: "SJ" }

DB already has name = "SJ"
→ No update needed
→ Engineer returns 304 Not Modified  ❌ WRONG
```

Why is this wrong? Because 304 is **not** meant for "nothing changed in the database due to your update request." That's a completely different scenario.

304 is specifically designed for the **client caching flow** — where the client passes `If-Modified-Since` in the header and the server compares timestamps.

**What should you return instead?**

```
PATCH /user/123  →  name already same in DB
                →  no update done
                →  return 200 OK  ✅
                   or 204 No Content ✅
```

> 🎯 **Interview Tip** — If an interviewer asks "When do you use 304?", the answer is: *"304 is used in GET requests where the client is caching responses and passes the `If-Modified-Since` header. The server compares the resource's last update time — if nothing changed, it returns 304 and the client uses its cached data. It should NOT be used for PATCH scenarios where the data in DB happens to be the same."*

---

## Full 3xx Picture

```
┌──────┬──────────────────────┬──────────────────────────────────────────────┐
│ Code │ Reason               │ When to use                                  │
├──────┼──────────────────────┼──────────────────────────────────────────────┤
│ 301  │ Moved Permanently    │ Old API permanently moved to new URI         │
│      │                      │ HTTP method can change during redirect       │
├──────┼──────────────────────┼──────────────────────────────────────────────┤
│ 308  │ Permanent Redirect   │ Same as 301 but HTTP method must stay same   │
│      │                      │ Stricter & improved version of 301           │
├──────┼──────────────────────┼──────────────────────────────────────────────┤
│ 304  │ Not Modified         │ GET + client caching flow only               │
│      │                      │ Client passes If-Modified-Since header       │
│      │                      │ Server says "you already have latest data"   │
│      │                      │ DON'T use with PATCH for "no DB change"      │
└──────┴──────────────────────┴──────────────────────────────────────────────┘
```

---

## 🎯 Interview Tips

**"What's the difference between 301 and 308?"**
→ Both permanently redirect to a new URI. 301 allows the HTTP method to change during redirect. 308 is stricter — the HTTP method must remain the same.

**"How does 301 redirection work in practice?"**
→ Server returns 301 with a `Location` header pointing to the new URI. The client reads the Location header and re-sends the request to the new URI automatically.

**"When do you use 304?"**
→ Only for GET requests with client-side caching. Client passes `If-Modified-Since` in the header. Server checks if resource was updated — if not, returns 304 and client uses its cached copy.

**"Can you use 304 for a PATCH where nothing changed in the DB?"**
→ No — that's a common mistake. Use 200 or 204 instead. 304 is strictly for the GET + caching flow.

---

That's Step 4 done! The 3xx codes are very practical — especially for API migrations and caching.

# Step 5 — 4xx Validation / Client Error Codes

---

## What does 4xx mean?

The server received the request — but something is **wrong with the request itself.** Either the client didn't send the right data, or there's a business logic reason to reject it.

The key point the instructor makes here:

```
┌──────────────────────────────────────────────────────────┐
│  4xx errors are EXPECTED failures                        │
│                                                          │
│  → It's NOT the server's fault                           │
│  → It's either the client's fault                        │
│    OR a business logic decision to reject the request    │
│                                                          │
│  The server handled the request correctly —              │
│  it just chose to reject it for a valid reason           │
└──────────────────────────────────────────────────────────┘
```

This is very different from 5xx — where the server itself breaks. We'll see that in Step 6.

---

## Overview

```
┌─────────────────────────────────────────────────────────┐
│               4xx — VALIDATION ERRORS                   │
│                                                         │
│  400  →  Bad Request                                    │
│  401  →  Unauthorized                                   │
│  403  →  Forbidden                                      │
│  404  →  Not Found                                      │
│  405  →  Method Not Allowed                             │
│  422  →  Unprocessable Entity                           │
│  429  →  Too Many Requests                              │
│  409  →  Conflict                                       │
└─────────────────────────────────────────────────────────┘
```

---

## 400 — Bad Request

Used when the **client hasn't sent the required details** to process the request.

```
Example:
Your API needs 3 fields — name, age, email
Client sends request with only name and age
→ Missing email
→ Server returns 400 Bad Request
```

```
Client  →  POST /user
           Body: { name: "John", age: 25 }
                            ❌ missing email
                │
                ▼
           Server checks required fields
           Email is missing!
                │
                ▼
Client  ←  400 Bad Request
           "email is required"
```

> Used with GET, POST, PATCH, DELETE — basically any API where the client is expected to send specific data.

---

## 401 — Unauthorized

Used when an API **requires authentication** but the client hasn't provided it (or provided it incorrectly).

```
Example:
API requires a Bearer Token in the header
Client sends request without any token
→ Server returns 401 Unauthorized
```

```
Client  →  GET /user/123
           Header: (no token provided)
                │
                ▼
           Server checks for auth token
           Token missing!
                │
                ▼
Client  ←  401 Unauthorized
```

```
┌──────────────────────────────────────────────────────────┐
│  Authentication types that trigger 401 if missing:       │
│                                                          │
│  → Bearer Token  (Authorization: Bearer <token>)         │
│  → Basic Auth    (username + password)                   │
│  → API Key       (passed in header or query param)       │
└──────────────────────────────────────────────────────────┘
```

---

## 403 — Forbidden

This is where a lot of people confuse 401 and 403. The instructor explains the difference very clearly.

```
┌──────────────────────────────────────────────────────────┐
│  401 — You haven't proven WHO you are                    │
│        (not authenticated)                               │
│                                                          │
│  403 — We know WHO you are, but you don't have           │
│        PERMISSION to do this                             │
│        (authenticated but not authorized)                │
└──────────────────────────────────────────────────────────┘
```

The instructor's example:

```
Scenario:
Only ADMIN can update an address.
A regular user (authenticated) tries to update an address.

Client (regular user)  →  PATCH /user/123/address
                                    │
                                    ▼
                          Server checks permission
                          User is NOT admin
                                    │
                                    ▼
Client  ←  403 Forbidden
           "you don't have permission
            to perform this action"
```

```
Flow:
                    Request comes in
                          │
                          ▼
                   Is user authenticated?
                    ┌─────┴─────┐
                   NO          YES
                    │           │
                    ▼           ▼
                  401      Does user have
              Unauthorized   permission?
                          ┌────┴────┐
                         NO       YES
                          │         │
                          ▼         ▼
                        403      Process
                      Forbidden  Request ✅
```

---

## 404 — Not Found

Used when the **resource the client is looking for doesn't exist** in the database.

```
Example:
Client wants details of user with ID: 123
But no such user exists in DB
→ Server returns 404 Not Found
```

```
Client  →  GET /user/123
                │
                ▼
           Server queries DB
           SELECT * FROM users WHERE id = 123
           → No rows found
                │
                ▼
Client  ←  404 Not Found
           "user with id 123 not found"
```

The instructor points out — 404 is mostly used with **GET, PATCH, DELETE** — not typically with POST. Why?

```
┌──────────────────────────────────────────────────────────┐
│  POST   → you're CREATING something new                  │
│           nothing to "not find" yet                      │
│                                                          │
│  GET    → you're FETCHING something → can be not found   │
│  PATCH  → you're UPDATING something → can be not found   │
│  DELETE → you're DELETING something → can be not found   │
└──────────────────────────────────────────────────────────┘
```

---

## 405 — Method Not Allowed

Used when the client hits the **right URL but with the wrong HTTP method.**

```
Example:
You have a GET /user API
Client hits it with POST method instead
→ Server returns 405 Method Not Allowed
```

```
Client  →  POST /user/get-user   ← wrong method
                │
                ▼
           Spring's Dispatcher Servlet
           sees the URL exists but
           no POST mapping for it
                │
                ▼
Client  ←  405 Method Not Allowed
```

> 💡 The instructor points out something important here — in Spring Boot, the **Dispatcher Servlet** itself throws this error. The request doesn't even reach your controller. So if you're wondering why your controller code isn't being hit, this is why.

---

## 422 — Unprocessable Entity

This is for **business logic failures** — where the request is perfectly valid technically (all fields present, proper format), but your business rules say it cannot be processed.

```
Example:
Your company hasn't got a license to operate in France.
A user from France tries to create an account.
All fields are correct, request format is fine.
But business rule says → France users NOT allowed.
→ Server returns 422 Unprocessable Entity
```

```
Client  →  POST /user
           Body: { name: "Pierre", country: "France" }
                            ✅ all fields present
                            ✅ correct format
                │
                ▼
           Server passes technical validation ✅
           Hits business logic check:
           "Is country supported?"
           France → NOT supported ❌
                │
                ▼
Client  ←  422 Unprocessable Entity
           "country not supported"
```

---

### 400 vs 422 — Key Difference

This is a very common interview question:

```
┌──────────────────────────────────────────────────────────┐
│  400 Bad Request                                         │
│  → Request itself is malformed                           │
│  → Missing required fields, wrong data type, etc.        │
│  → Technical validation failure                          │
│                                                          │
│  422 Unprocessable Entity                                │
│  → Request is technically fine                           │
│  → But YOUR BUSINESS RULES reject it                     │
│  → Business logic / domain validation failure            │
└──────────────────────────────────────────────────────────┘
```

---

## 429 — Too Many Requests

Used for **rate limiting.** When a client is making too many requests in a given time window.

```
Example:
Rule: 1 user can make max 10 calls per minute
User:12345 makes the 11th call in the same minute
→ Server returns 429 Too Many Requests
```

```
Minute window for User:12345

Call 1   ✅
Call 2   ✅
Call 3   ✅
...
Call 10  ✅
Call 11  ❌  →  429 Too Many Requests
                "rate limit exceeded,
                 try again after X seconds"
```

```
┌──────────────────────────────────────────────────────────┐
│  Rate Limiter sits in front of your API                  │
│                                                          │
│  Client → Rate Limiter → API                             │
│                                                          │
│  Rate Limiter tracks:                                    │
│  → which user                                            │
│  → how many calls                                        │
│  → in what time window                                   │
│                                                          │
│  If limit exceeded → 429, request blocked                │
│  Request doesn't even reach your API                     │
└──────────────────────────────────────────────────────────┘
```

---

## 409 — Conflict

Used when a **request is already in progress** for the same resource, and another identical request comes in before the first one finishes.

The instructor says 409 is mostly used with **POST, PATCH, DELETE** — not with GET (because GET is just reading data, no conflict possible).

```
Example:
PATCH /user/123 → Request 1 starts processing
                  (still in progress, not done yet)

PATCH /user/123 → Request 2 comes in
                  for the SAME resource
                  → 409 Conflict
```

---

### How is this handled in practice?

The instructor explains the **locking mechanism:**

```
Request 1 arrives for /user/123
        │
        ▼
  Server puts a LOCK on user:123
  (stored in cache like Redis)
  lock: { userId: 123, status: "locked" }
        │
        ▼
  Processing Request 1...

Meanwhile →  Request 2 arrives for /user/123
                      │
                      ▼
               Server checks lock
               user:123 is LOCKED ❌
                      │
                      ▼
             Client ← 409 Conflict
             "request already in progress"

Request 1 completes
        │
        ▼
  Server removes lock on user:123  ✅
  (or TTL auto-expires the lock)
```

---

### TTL on the lock

The instructor also mentions something practical — you set a **TTL (Time To Live)** on the lock so it auto-releases even if something goes wrong:

```
┌──────────────────────────────────────────────────────────┐
│  Lock with TTL                                           │
│                                                          │
│  → Set lock when request starts                          │
│  → TTL = 5s / 10s (depends on your API's avg time)       │
│  → Lock auto-releases after TTL                          │
│    even if request somehow didn't clean it up            │
│                                                          │
│  OR                                                      │
│                                                          │
│  → Manually release lock when request completes          │
└──────────────────────────────────────────────────────────┘
```

---

## Full 4xx Picture

```
┌──────┬──────────────────────┬──────────────────────────────────────────────┐
│ Code │ Reason               │ When to use                                  │
├──────┼──────────────────────┼──────────────────────────────────────────────┤
│ 400  │ Bad Request          │ Client didn't send required fields/data      │
│      │                      │ Technical/structural validation failure      │
├──────┼──────────────────────┼──────────────────────────────────────────────┤
│ 401  │ Unauthorized         │ No authentication provided                   │
│      │                      │ Missing/invalid token or credentials         │
├──────┼──────────────────────┼──────────────────────────────────────────────┤
│ 403  │ Forbidden            │ Authenticated but lacks permission           │
│      │                      │ e.g. non-admin trying admin operation        │
├──────┼──────────────────────┼──────────────────────────────────────────────┤
│ 404  │ Not Found            │ Requested resource doesn't exist in DB       │
│      │                      │ Used with GET, PATCH, DELETE (not POST)      │
├──────┼──────────────────────┼──────────────────────────────────────────────┤
│ 405  │ Method Not Allowed   │ Right URL, wrong HTTP method                 │
│      │                      │ Thrown by Dispatcher Servlet in Spring Boot  │
├──────┼──────────────────────┼──────────────────────────────────────────────┤
│ 422  │ Unprocessable Entity │ Business logic/domain validation failure     │
│      │                      │ Request is valid but business rules reject it│
├──────┼──────────────────────┼──────────────────────────────────────────────┤
│ 429  │ Too Many Requests    │ Rate limiting — client exceeded call quota   │
├──────┼──────────────────────┼──────────────────────────────────────────────┤
│ 409  │ Conflict             │ Same resource already being processed        │
│      │                      │ Used with POST, PATCH, DELETE (not GET)      │
└──────┴──────────────────────┴──────────────────────────────────────────────┘
```

---

## 🎯 Interview Tips

**"What's the difference between 401 and 403?"**
→ 401 = not authenticated (server doesn't know who you are). 403 = authenticated but not authorized (server knows who you are, but you don't have permission).

**"What's the difference between 400 and 422?"**
→ 400 = request is malformed or missing required fields (technical failure). 422 = request is technically fine but business rules reject it (domain/business failure).

**"When would you use 409?"**
→ When the same resource is already being modified by an in-progress request. Use a lock with TTL to manage this.

**"Why does 405 not reach your controller in Spring Boot?"**
→ The Dispatcher Servlet intercepts it before it reaches the controller, because no matching HTTP method mapping is found.

**"When do you use 429?"**
→ Rate limiting — when a client exceeds the maximum allowed number of requests in a given time window.

---

That's Step 5 done — the biggest and most interview-heavy section covered in full!

# Step 6 — 5xx Server Error Codes + Step 7 — 1xx Informational Codes

---

## What does 5xx mean?

This is where things go wrong **on the server's side** — not the client's fault. The client sent a perfectly valid request, passed all validations, everything was correct — but the server still failed to process it.

The instructor makes a very important point here:

```
┌──────────────────────────────────────────────────────────┐
│  4xx → Expected failures                                 │
│        We KNOW these can happen                          │
│        We write code to handle them                      │
│                                                          │
│  5xx → Unexpected failures                               │
│        These should NOT happen                           │
│        Something broke inside the server                 │
│        We didn't plan for this                           │
└──────────────────────────────────────────────────────────┘
```

The instructor says — *"When we write code, we write it in a way that if the request is valid, it should always succeed. If it's still failing — we don't know why. That's why it ends up as a 500."*

> **Goal as a developer — keep 5xx errors as LOW as possible in your system.**

---

## Overview

```
┌─────────────────────────────────────────────────────────┐
│                 5xx — SERVER ERRORS                     │
│                                                         │
│  500  →  Internal Server Error                          │
│  501  →  Not Implemented                                │
│  502  →  Bad Gateway                                    │
└─────────────────────────────────────────────────────────┘
```

---

## 500 — Internal Server Error

The most generic server error code. Used when **something went wrong at the server and no more specific error code fits.**

```
Common causes:
┌──────────────────────────────────────────────────────────┐
│  → NullPointerException                                  │
│  → ClassCastException                                    │
│  → Database is down                                      │
│  → Unhandled exceptions in your code                     │
│  → Any unexpected runtime error                          │
└──────────────────────────────────────────────────────────┘
```

```
Client  →  GET /user/123
           (valid request, token present,
            user exists in DB)
                │
                ▼
           Server starts processing
           Hits a NullPointerException
           deep inside the code 💥
                │
                ▼
Client  ←  500 Internal Server Error
           "something went wrong"
```

The instructor's point — you're not purposely writing code to fail here. You wrote the code thinking it would work. But something unexpected broke at runtime. That's the nature of 500 — it's a **catch-all for things that shouldn't have failed but did.**

```
┌──────────────────────────────────────────────────────────┐
│  Think of it this way:                                   │
│                                                          │
│  4xx → "You did something wrong"  (client's fault)       │
│  5xx → "We did something wrong"   (server's fault)       │
│                                                          │
│  500 specifically → "We have no idea what went wrong,    │
│                      but something did"                  │
└──────────────────────────────────────────────────────────┘
```

---

## 501 — Not Implemented

Used when an **API exists but its implementation is not yet complete.** The endpoint is registered, but the actual logic hasn't been built yet.

```
Example:
Team is building /api/user in chunks.
Endpoint is created, but logic is incomplete.
→ Temporarily return 501 until it's ready.
```

```
Client  →  POST /api/user
                │
                ▼
           Controller exists ✅
           But implementation
           is not done yet ❌
                │
                ▼
Client  ←  501 Not Implemented
           "this API is under development,
            will be available soon"
```

```
┌──────────────────────────────────────────────────────────┐
│  When to use 501:                                        │
│                                                          │
│  → API is in development, not yet ready                  │
│  → Placeholder endpoint — future functionality           │
│  → Team is working on it in chunks                       │
│                                                          │
│  It tells the client:                                    │
│  "This will work someday, just not today"                │
└──────────────────────────────────────────────────────────┘
```

---

## 502 — Bad Gateway

Used when your **server is acting as a proxy/gateway and cannot communicate with the upstream service** it's trying to reach.

The instructor gives a very practical real-world example with **Nginx:**

```
Normal flow (everything working):

Client  →  Nginx (Reverse Proxy)  →  Your Spring Boot App
                                            │
                                            ▼
Client  ←  Nginx                 ←  Response from App
```

```
When 502 happens:

Client  →  Nginx (Reverse Proxy)  →  Your Spring Boot App
                                            ❌
                                    Can't reach the app
                                    (wrong port config,
                                     app is down, timeout)
                │
                ▼
Client  ←  502 Bad Gateway
           "upstream server gave
            invalid/no response"
```

```
┌──────────────────────────────────────────────────────────┐
│  Common causes of 502:                                   │
│                                                          │
│  → Misconfigured port number in Nginx config             │
│  → Spring Boot app is down / crashed                     │
│  → Connection timeout between proxy and app              │
│  → Upstream service returned garbage response            │
└──────────────────────────────────────────────────────────┘
```

---

## Full 5xx Picture

```
┌──────┬──────────────────────┬──────────────────────────────────────────────┐
│ Code │ Reason               │ When to use                                  │
├──────┼──────────────────────┼──────────────────────────────────────────────┤
│ 500  │ Internal Server Error│ Generic catch-all for unexpected server      │
│      │                      │ failures — NPE, DB down, unhandled exception │
├──────┼──────────────────────┼──────────────────────────────────────────────┤
│ 501  │ Not Implemented      │ API exists but logic not yet built           │
│      │                      │ Placeholder for future functionality         │
├──────┼──────────────────────┼──────────────────────────────────────────────┤
│ 502  │ Bad Gateway          │ Proxy/gateway can't reach upstream service   │
│      │                      │ e.g. Nginx can't talk to Spring Boot app     │
└──────┴──────────────────────┴──────────────────────────────────────────────┘
```

---

---

# Step 7 — 1xx Informational Codes

---

## What does 1xx mean?

The instructor says 1xx is the **least frequently used** category — in his 9 years of experience, he's never had to use it in production. But it's still good to understand.

```
┌──────────────────────────────────────────────────────────┐
│  1xx → Informational                                     │
│                                                          │
│  → NOT a final response                                  │
│  → Just an interim / in-between response                 │
│  → Server is communicating progress or status            │
│    BEFORE sending the actual final response              │
└──────────────────────────────────────────────────────────┘
```

---

## Overview

```
┌─────────────────────────────────────────────────────────┐
│              1xx — INFORMATIONAL                        │
│                                                         │
│  100  →  Continue                                       │
└─────────────────────────────────────────────────────────┘
```

---

## 100 — Continue

Used mostly with **POST** — specifically for large payloads. The idea is — before sending a huge request body, the client first **checks with the server** whether it's ready and capable of handling it.

Think of it like asking *"Are you ready to receive this?"* before sending something big.

---

### Full flow — step by step

```
STEP 1 — Client sends a "check" request first

Client  →  POST /upload
           Headers:
           Content-Length: 1048576   (size of the file)
           Content-Type: multipart/form-data
           Expect: 100-continue      ← key header
           Body: (NOT sent yet)
```

```
STEP 2 — Server sees the Expect header

           Server checks:
           → Is client authenticated? ✅
           → Does client have permission? ✅
           → Is Content-Type correct? ✅
           → Can server handle this size? ✅
           All good!
                │
                ▼
Client  ←  100 Continue
           "go ahead, I'm ready"
```

```
STEP 3 — Client now sends the actual request

Client  →  POST /upload
           Headers:
           Content-Length: 1048576
           Content-Type: multipart/form-data
           (NO Expect header this time)
           Body: (actual file data now sent) ✅
                │
                ▼
           Server processes the full request
                │
                ▼
Client  ←  200 OK  (or 201, depending on use case)
           ← THIS is the final response
```

---

### Why is this useful?

```
┌──────────────────────────────────────────────────────────┐
│  Without 100 Continue:                                   │
│                                                          │
│  Client uploads huge file (say 1GB)                      │
│  Server receives it all                                  │
│  Then rejects it — "you're not authenticated"            │
│  → 1GB of bandwidth wasted ❌                             │
│                                                          │
│  With 100 Continue:                                      │
│                                                          │
│  Client asks first — "can I send this?"                  │
│  Server validates everything upfront                     │
│  If rejected → client never sends the big payload        │
│  → Zero bandwidth wasted ✅                               │
└──────────────────────────────────────────────────────────┘
```

---

### Important — 100 is NOT the final response

```
┌──────────────────────────────────────────────────────────┐
│  100 Continue → INTERIM response                         │
│                 just says "go ahead"                     │
│                 processing hasn't happened yet           │
│                                                          │
│  200/201 etc. → FINAL response                           │
│                 actual processing is done                │
└──────────────────────────────────────────────────────────┘
```

That's exactly why 1xx is called **Informational** — it's the server communicating *status or progress* before the real work even begins.

---

## Full 1xx Picture

```
┌──────┬──────────────────────┬──────────────────────────────────────────────┐
│ Code │ Reason               │ When to use                                  │
├──────┼──────────────────────┼──────────────────────────────────────────────┤
│ 100  │ Continue             │ Client checking if server is ready           │
│      │                      │ before sending large payload (POST)          │
│      │                      │ Client sends Expect: 100-continue header     │
│      │                      │ Server validates & responds with 100         │
│      │                      │ Client then sends the actual body            │
└──────┴──────────────────────┴──────────────────────────────────────────────┘
```

---

---

# Complete Response Codes — Master Summary

```
┌──────────────────────────────────────────────────────────────────────┐
│                    ALL HTTP RESPONSE CODES                           │
├──────────┬───────────────────────────────────────────────────────────┤
│   1xx    │  INFORMATIONAL — interim, not final                       │
│          │  100 → Continue (large payload pre-check)                 │
├──────────┼───────────────────────────────────────────────────────────┤
│   2xx    │  SUCCESS — request processed successfully                 │
│          │  200 → OK (GET success, idempotent POST)                  │
│          │  201 → Created (new resource created)                     │
│          │  202 → Accepted (processing still ongoing)                │
│          │  204 → No Content (success, no body)                      │
│          │  206 → Partial Content (bulk, partial success)            │
├──────────┼───────────────────────────────────────────────────────────┤
│   3xx    │  REDIRECTION — client must take action                    │
│          │  301 → Moved Permanently (method can change)              │
│          │  308 → Permanent Redirect (method cannot change)          │
│          │  304 → Not Modified (GET + caching only)                  │
├──────────┼───────────────────────────────────────────────────────────┤
│   4xx    │  CLIENT ERROR — expected, request is the problem          │
│          │  400 → Bad Request (missing/wrong fields)                 │
│          │  401 → Unauthorized (not authenticated)                   │
│          │  403 → Forbidden (authenticated, no permission)           │
│          │  404 → Not Found (resource doesn't exist)                 │
│          │  405 → Method Not Allowed (wrong HTTP method)             │
│          │  422 → Unprocessable Entity (business logic failure)      │
│          │  429 → Too Many Requests (rate limit exceeded)            │
│          │  409 → Conflict (same resource already in progress)       │
├──────────┼───────────────────────────────────────────────────────────┤
│   5xx    │  SERVER ERROR — unexpected, server is the problem         │
│          │  500 → Internal Server Error (generic, catch-all)         │
│          │  501 → Not Implemented (API not built yet)                │
│          │  502 → Bad Gateway (proxy can't reach upstream)           │
└──────────┴───────────────────────────────────────────────────────────┘
```

---

## 🎯 Final Interview Tips Round-up

**"What's the difference between 4xx and 5xx?"**
→ 4xx = client/request problem, expected failure, we handle it in code. 5xx = server broke unexpectedly, something we didn't plan for.

**"What causes a 500 error?"**
→ NullPointerException, DB being down, unhandled exceptions, ClassCastException — anything that breaks at runtime unexpectedly.

**"What's 502 and when does it happen?"**
→ When your server acts as a gateway/proxy and can't communicate with the upstream service. Classic example — Nginx misconfigured and can't reach your Spring Boot app.

**"What is 501 used for?"**
→ When an API endpoint exists but the implementation isn't done yet. It's a temporary placeholder telling clients this feature is coming.

**"What is 100 Continue and why is it useful?"**
→ It's a pre-flight check before sending large payloads. Client asks "are you ready?" — server validates auth, permissions, content type — if all good, returns 100. Then client sends the actual big payload. Saves bandwidth if the server would have rejected it anyway.

---

## That's the complete lecture! 🎉

Here's a quick recap of the full flow of what we covered across all 7 steps:

```
Step 1 → What is a Response (3 parts)
Step 2 → ResponseEntity, Builder Pattern, @ResponseBody
Step 3 → 2xx Success Codes
Step 4 → 3xx Redirection Codes
Step 5 → 4xx Client Error Codes
Step 6 → 5xx Server Error Codes
Step 7 → 1xx Informational Codes
```

The instructor's next topic after this is **Exception Handling in Spring Boot** — and now with this solid foundation of response codes, that topic will make a lot more sense. 🚀