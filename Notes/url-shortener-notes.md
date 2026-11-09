# URL Shortener --- Backend Design Notes

## 1. What is a URL shortener?

A URL shortener converts a long URL into a shorter URL that redirects
users to the original destination.

Example:

``` text
Original:
https://www.example.com/products/electronics/laptops/model-123?campaign=social

Short:
https://short.ly/aB92xKp7LmQ3
```

When someone opens the short URL, the application looks up its short
code, retrieves the original URL, records the click if required, and
redirects the browser.

Common examples include Bitly, TinyURL, `youtu.be`, and Amazon's
`amzn.to` links.

## 2. Why are URL shorteners useful?

-   **Readability and sharing:** Short links are easier to share in
    messages, presentations, and printed material.
-   **QR codes:** A short URL can be encoded into a QR code, making the
    QR code less dense and easier to scan.
-   **Click tracking:** The shortener can record visits before
    redirecting to the destination.
-   **Campaign analytics:** Different short codes can point to the same
    destination while measuring traffic from different channels.
-   **Editable destinations:** Some services let the owner change the
    destination without changing the short URL.

A shortener is not the only way to track visits. A destination website
can track visits to its own pages. A shortener is useful when the
destination belongs to another service or when tracking needs to happen
before redirecting.

## 3. High-level architecture

``` mermaid
flowchart TD
    A[Client] -->|POST /api/urls| B[URL Shortener API]
    B --> C[Short Code Generator]
    C --> D[(PostgreSQL)]
    B -->|Short URL| A

    A -->|GET /{shortCode}| E[Redirect API]
    E --> F[(PostgreSQL lookup)]
    F -->|Original URL| E
    E --> G[Increment click count]
    E -->|HTTP redirect| H[Original destination]
```

The system has two main flows:

1.  **Create short URL:** Accept an original URL, generate a code, save
    the mapping, and return the short URL.
2.  **Resolve short URL:** Accept a code, find the original URL, record
    a visit, and redirect the client.

## 4. Core database model

A basic table can store one mapping per short code.

``` sql
CREATE TABLE url_data (
    short_code VARCHAR(12) PRIMARY KEY,
    url TEXT NOT NULL,
    views BIGINT NOT NULL DEFAULT 0
);
```

Example row:

  short_code     url                                  views
  -------------- ---------------------------------- -------
  aB92xKp7LmQ3   https://example.com/products/123        42

Important points:

-   `short_code` is the primary key, so the database guarantees that it
    is unique.
-   The original URL is stored as text because URLs can be long.
-   `views` stores a simple aggregate click count.
-   In a production system, consider fields such as `created_at`,
    `expires_at`, `user_id`, and `is_active`.

### JPA entity sketch

``` java
@Entity
@Table(name = "url_data")
public class UrlData {

    @Id
    @Column(name = "short_code", nullable = false, length = 12)
    private String shortCode;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    private long views;

    // Constructors, getters and setters
}
```

If `shortCode` is the `@Id`, it already has a primary-key uniqueness
guarantee. A separate `unique = true` declaration is not required.

## 5. Generating short codes

The short code should be:

-   Short enough to share easily.
-   Difficult to guess or enumerate.
-   Unique in the database.
-   URL-safe.

### Recommended approach: SecureRandom + Base62

Use 62 URL-safe characters:

``` text
0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ
```

Generate each character using Java's `SecureRandom`.

``` java
import java.security.SecureRandom;

public class ShortCodeGenerator {

    private static final String CHARACTERS =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final int CODE_LENGTH = 12;

    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);

        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            code.append(CHARACTERS.charAt(index));
        }

        return code.toString();
    }
}
```

`SecureRandom.nextInt(62)` selects an index uniformly from 0 through 61.

### How large is the code space?

For an alphabet of 62 characters and a code length of 12:

\[ N = 62\^{12} `\approx 3.23`{=tex} `\times 10`{=tex}\^{21} \]

That is approximately 71.45 bits of randomness.

A collision is possible, but the namespace is large. The database
constraint is still essential.

### Collision probability

The approximate probability of at least one collision after generating
`n` random codes is:

\[ P(`\text{collision}`{=tex}) `\approx 1`{=tex} -
e\^{-`\frac{n(n-1)}{2N}`{=tex}} \]

For 12-character Base62 codes, approximate values are:

    Number of generated codes   Probability of at least one collision
  --------------------------- ---------------------------------------
                    1 million                           0.0000000155%
                  100 million                               0.000155%
                    1 billion                                 0.0155%
                   10 billion                                   1.54%

These are birthday-paradox probabilities across the entire set of
generated codes, not the probability that one particular generation
collides.

Each additional Base62 character multiplies the namespace by 62 and
reduces collision probability substantially.

## 6. Why not use timestamps, sequential IDs, or URL hashes?

  -----------------------------------------------------------------------
  Approach                Advantages              Disadvantages
  ----------------------- ----------------------- -----------------------
  SecureRandom + Base62   Harder to predict;      Collisions are
                          independent of clocks;  possible, so retry
                          different codes can     logic is needed
                          represent the same URL  

  Timestamp + random      Timestamp can help make Partially predictable;
  value                   values distinct         clock precision and
                                                  coordination add
                                                  complexity; database
                                                  uniqueness is still
                                                  needed

  Sequential ID encoded   Simple; uniqueness can  Predictable and
  as Base62               come from the database  enumerable; reveals
                          sequence                approximate creation
                                                  order or volume

  Hash of original URL    Same input can produce  Truncation can collide;
                          the same output         URL normalization
                                                  matters; same URL
                                                  always maps to the same
                                                  code unless extra data
                                                  is added
  -----------------------------------------------------------------------

For this project, use `SecureRandom` with a database uniqueness
constraint.

A hash is appropriate only if deterministic mapping or deduplication is
an explicit product requirement. It is not automatically a better
collision solution.

Randomness makes codes difficult to guess, but it does not provide
authorization. Private links need access control, expiry, and abuse
protection.

## 7. Handling concurrent duplicate codes

A preliminary availability check is not sufficient:

``` java
if (!urlDataRepository.existsById(shortCode)) {
    urlDataRepository.save(urlData);
}
```

Why is this unsafe?

Two application requests can interleave:

1.  Request A checks code `abc123` --- available.
2.  Request B checks code `abc123` --- available.
3.  Request A inserts `abc123` --- succeeds.
4.  Request B inserts `abc123` --- fails.

This is a **race condition**, specifically a
time-of-check-to-time-of-use (TOCTOU) problem.

### Correct principle

Let the database enforce uniqueness atomically. Do not rely on a
separate check to guarantee that a value remains available.

There are two useful approaches.

### Option A: JPA `saveAndFlush()` + unique constraint

``` java
try {
    UrlData urlData = new UrlData(url, shortCode);
    urlDataRepository.saveAndFlush(urlData);

    return shortCode;
} catch (DataIntegrityViolationException exception) {
    // Retry only if the failure is the short-code unique constraint.
}
```

`saveAndFlush()` asks JPA/Hibernate to flush pending changes to the
database immediately. The database then checks the primary-key or unique
constraint.

Important distinction:

-   `saveAndFlush()` does not check availability first.
-   It attempts the insert.
-   PostgreSQL rejects a duplicate key.
-   The application can generate another code and retry.

`save()` may defer SQL execution until a later flush or transaction
commit. `saveAndFlush()` makes the insert-time constraint check happen
at that point.

**Transaction caveat:** A constraint violation can mark the current
transaction as rollback-only. Do not assume you can catch the exception
and continue using the same transaction. Keep each insertion attempt in
its own transaction (for example, using a separate transactional bean
with `REQUIRES_NEW`) or use the PostgreSQL approach below.

Also, do not treat every `DataIntegrityViolationException` as a code
collision in production. Check that it is specifically a unique-key
violation (PostgreSQL SQLSTATE `23505`); other integrity failures should
be surfaced.

### Option B: PostgreSQL `INSERT ... ON CONFLICT DO NOTHING`

This is a PostgreSQL-specific approach that handles the expected
collision without using an exception as normal control flow.

``` sql
INSERT INTO url_data (short_code, url, views)
VALUES (:shortCode, :url, 0)
ON CONFLICT (short_code) DO NOTHING;
```

The insert result tells the application whether a row was inserted:

-   One row affected: success.
-   Zero rows affected: the short code already exists; generate another
    and retry.

A repository method can use a native query:

``` java
@Modifying
@Query(value = """
       INSERT INTO url_data (short_code, url, views)
       VALUES (:shortCode, :url, 0)
       ON CONFLICT (short_code) DO NOTHING
       """, nativeQuery = true)
int insertIfCodeAvailable(
        @Param("shortCode") String shortCode,
        @Param("url") String url
);
```

The service retries when the returned count is zero.

This approach is particularly clean for PostgreSQL because the database
performs the conflict handling atomically.

### Why `synchronized` is not enough

A Java `synchronized` block coordinates threads that share the same JVM.
It does not coordinate separate application instances or containers.

The database constraint works across all application instances connected
to that database.

## 8. Service flow for generating a short URL

Pseudocode:

``` text
generateShortUrl(originalUrl):
    repeat up to MAX_RETRIES:
        shortCode = generateRandomBase62Code()
        inserted = insertIfCodeAvailable(shortCode, originalUrl)

        if inserted:
            return domain + "/" + shortCode

    throw an error
```

Example retry configuration:

``` java
private static final int MAX_RETRIES = 5;
```

If all attempts fail, return an appropriate server error. Do not return
a code that was not successfully stored.

The retry loop should retry only on a short-code collision, not on
unrelated database failures.

## 9. Resolving a short URL and redirecting

When a browser requests:

``` http
GET /aB92xKp7LmQ3
```

The application:

1.  Extracts the short code.
2.  Looks up the corresponding row.
3.  Returns `404 Not Found` if no mapping exists.
4.  Increments the view count.
5.  Redirects to the original URL.

Repository lookup:

``` java
Optional<UrlData> findById(String shortCode);
```

Atomic view increment:

``` java
@Modifying
@Transactional
@Query("""
       UPDATE UrlData u
       SET u.views = u.views + 1
       WHERE u.shortCode = :shortCode
       """)
int incrementViewsByShortCode(@Param("shortCode") String shortCode);
```

Using a database-side increment avoids the classic read-modify-write
lost-update problem that can occur when multiple requests read the same
view count and then save their own incremented values.

### Redirect status codes

-   **302 Found:** Temporary redirect. A common choice for a basic URL
    shortener.
-   **301 Moved Permanently:** Permanent redirect. Browsers and
    intermediaries may cache it, which can reduce future requests
    reaching the shortener and therefore affect click tracking.
-   **307 Temporary Redirect:** Temporary redirect that preserves the
    HTTP method.
-   **308 Permanent Redirect:** Permanent redirect that preserves the
    HTTP method.

For a simple shortener, a 302 redirect is a reasonable default. If the
destination can change or accurate per-request tracking matters, avoid
permanently cached redirects.

## 10. Click tracking and analytics

A basic implementation can maintain a `views` counter.

However, define what a click means:

-   Every request to the short URL?
-   Every human visit?
-   Unique visitors?
-   Visits within a time window?

A simple counter generally counts requests, not unique people or
purchases. Bots, link-preview services, repeated visits, and automated
scanners can increase the count.

For richer analytics, use a separate click-events table:

``` sql
CREATE TABLE click_event (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(12) NOT NULL,
    clicked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    referrer TEXT,
    user_agent TEXT
);
```

Potential analytics include:

-   Total requests.
-   Clicks over time.
-   Referrer/source.
-   Device or browser category.
-   Campaign-level comparisons.

Be careful with IP addresses and other personal data. Define retention
and privacy policies before storing them.

### Why multiple short codes can point to the same URL

Suppose a business shares the same landing page through three channels:

``` text
Email       → short.ly/email123
Instagram   → short.ly/insta456
Poster QR   → short.ly/poster789
```

All three codes can map to the same destination URL. This lets the
shortener compare traffic by campaign or channel.

This is one reason not to automatically deduplicate every identical
original URL.

## 11. QR codes and URL shorteners

A QR code is a visual representation of encoded data. For a URL QR code:

1.  A QR generator receives a URL.
2.  It encodes the URL text into a QR image.
3.  A phone camera or QR scanner decodes the image.
4.  The browser opens the decoded URL.

A QR code does not inherently require a short URL.

Two possible flows:

**Direct QR code**

``` text
Original URL → QR generator → QR code
```

**Trackable/editable QR code**

``` text
Original URL → URL shortener → Short URL → QR generator → QR code
```

The second flow allows the shortener to record scans/visits and
potentially change the destination later.

A QR code generator may create and manage its own short redirect URL.
The user often only needs to provide the original destination URL.

## 12. API design

A simple API surface could be:

  Method   Endpoint                            Purpose
  -------- ----------------------------------- ----------------------
  POST     `/api/urls`                         Create a short URL
  GET      `/{shortCode}`                      Resolve and redirect
  GET      `/api/urls/{shortCode}/analytics`   Retrieve analytics

Example request:

``` http
POST /api/urls
Content-Type: application/json
```

``` json
{
  "url": "https://example.com/products/123"
}
```

Example response:

``` json
{
  "shortCode": "aB92xKp7LmQ3",
  "shortUrl": "https://short.ly/aB92xKp7LmQ3",
  "originalUrl": "https://example.com/products/123"
}
```

Validate submitted URLs. At minimum, accept only allowed schemes such as
`http` and `https`; do not blindly redirect to arbitrary schemes such as
`javascript:`.

## 13. Production considerations

A learning-project implementation can start with a single Spring Boot
application and PostgreSQL. A production service may also need:

-   **Expiration:** Optional `expires_at` for links that should stop
    working.
-   **Custom aliases:** User-selected short codes, with collision
    validation.
-   **Rate limiting:** Reduce abuse and automated code guessing.
-   **Abuse and malware protection:** Prevent the service from becoming
    a redirect mechanism for malicious destinations.
-   **Caching:** Cache frequently accessed code-to-URL mappings, while
    considering invalidation and expiry.
-   **Analytics pipeline:** Move high-volume click events to
    asynchronous processing or a queue.
-   **Database indexing:** The short code primary key already provides
    an index for lookup.
-   **Monitoring:** Track redirect latency, database errors, collision
    retries, and traffic.
-   **Availability and durability:** Backups, replication, and recovery
    procedures.
-   **Security:** Do not treat an unguessable code as authorization for
    sensitive content.

## 14. Key concepts learned

  -----------------------------------------------------------------------
  Concept                             Where it applies
  ----------------------------------- -----------------------------------
  `SecureRandom`                      Generating unpredictable short
                                      codes

  Base62 encoding/alphabet            Compact URL-safe identifiers

  Primary key / unique constraint     Enforcing uniqueness

  Collision probability               Choosing a suitable code length

  Race condition / TOCTOU             Understanding why check-then-insert
                                      is unsafe

  `saveAndFlush()`                    Forcing JPA to flush an insert
                                      immediately

  `ON CONFLICT DO NOTHING`            Atomic PostgreSQL conflict handling

  Transactions                        Understanding rollback behavior
                                      after database errors

  Atomic SQL update                   Correctly incrementing view counts

  HTTP redirects                      Sending the browser to the original
                                      destination

  QR encoding                         Turning a URL into a scannable
                                      image
  -----------------------------------------------------------------------

## 15. Suggested implementation order

1.  Create the `UrlData` entity and PostgreSQL table.
2.  Implement the `SecureRandom` Base62 code generator.
3.  Implement short URL creation with collision-safe insertion and
    retry.
4.  Implement lookup by short code.
5.  Implement the HTTP redirect endpoint.
6.  Implement atomic view-count updates.
7.  Add an analytics endpoint.
8.  Validate URLs and handle invalid or missing codes.
9.  Add tests for duplicate codes, concurrent creation, redirects, and
    analytics.
10. Consider expiry, custom aliases, caching, and richer click
    analytics.

------------------------------------------------------------------------

## Final design decisions for this learning project

-   **Language/framework:** Java 21 + Spring Boot.
-   **Database:** PostgreSQL.
-   **Identifier:** 12-character Base62 code generated using
    `SecureRandom`.
-   **Uniqueness:** Database primary-key constraint.
-   **Collision handling:** Retry after a collision; PostgreSQL
    `ON CONFLICT DO NOTHING` is a clean database-specific option.
-   **Redirect:** Start with HTTP 302.
-   **Basic analytics:** Atomic database-side increment of a view
    counter.
-   **Duplicate original URLs:** Allow separate short codes unless
    product requirements explicitly call for deduplication.
