# Caching

## 1. What is Caching?

**Caching** is the process of temporarily storing frequently accessed data in a faster storage layer so that future requests can be served without repeating the expensive operation.

### Without Cache

```text
Client
  ↓
Application
  ↓
Database
  ↓
Response
```

Every request reaches the database.

### With Cache

```text
Client
  ↓
Application
  ↓
Cache
 ┌───────────────┐
 │ Cache HIT     │ → Return data
 │ Cache MISS    │ → Database
 └───────────────┘
```

The goal is to avoid unnecessary expensive operations.

---

# 2. Why Do We Need Caching?

Caching can improve:

* Response latency
* Database load
* Application throughput
* Scalability
* Overall system performance

For example, suppose retrieving a post requires a database query:

```text
Request → Application → Database → Response
```

If the same post is requested 10,000 times, querying the database 10,000 times is unnecessary.

Instead:

```text
First request
    ↓
Cache MISS
    ↓
Database
    ↓
Store result in Cache

Next 9,999 requests
    ↓
Cache HIT
    ↓
Return from Cache
```

The database may only need to handle the initial request.

---

# 3. Cache Hit vs Cache Miss

## Cache Hit

A **cache hit** occurs when the requested data already exists in the cache.

```text
Request
   ↓
Cache
   ↓
Data exists
   ↓
CACHE HIT
   ↓
Return data
```

No database query is required.

---

## Cache Miss

A **cache miss** occurs when the requested data does not exist in the cache.

```text
Request
   ↓
Cache
   ↓
Data doesn't exist
   ↓
CACHE MISS
   ↓
Database
   ↓
Store in Cache
   ↓
Return data
```

---

# 4. Cache Hit Ratio

The **cache hit ratio** tells us how often requests are successfully served from the cache.

```text
Cache Hit Ratio = Cache Hits / Total Requests
```

For example:

```text
Total requests = 10,000
Cache hits     = 9,000

Hit Ratio = 9,000 / 10,000
          = 90%
```

A higher hit ratio generally means the cache is doing a better job.

However, a high hit ratio isn't automatically good. We also need to consider:

* Data freshness
* Memory usage
* Cache maintenance cost
* Cache misses
* Database load

---

# 5. Where Can Caching Exist?

Caching can happen at multiple layers.

```text
User
  ↓
Browser Cache
  ↓
CDN / Edge Cache
  ↓
Application Server
  ↓
Redis / Application Cache
  ↓
Database
```

Different layers solve different problems.

### Browser Cache

Stores resources on the user's device.

Examples:

* Images
* JavaScript
* CSS
* Static files

---

### CDN Cache

A CDN stores content at geographically distributed edge locations.

```text
User
  ↓
Nearest CDN Edge
  ↓
Cache HIT → Response

Cache MISS
  ↓
Origin Server
```

Useful for:

* Images
* Videos
* JavaScript
* CSS
* Static files
* Some HTTP responses

---

### Application Cache

The application itself can maintain cached data.

For example:

```text
Spring Boot
    ↓
Local Memory Cache
    ↓
Database
```

This is usually local to one application instance.

---

### Redis

Redis is commonly used as a shared application-level cache.

```text
Application
     ↓
   Redis
     ↓
 Database
```

Redis is an **in-memory data store** that can be used for caching, among many other use cases.

---

# 6. Redis vs CDN

They are both related to caching, but they operate at different levels.

| CDN                               | Redis                                      |
| --------------------------------- | ------------------------------------------ |
| Network / HTTP level              | Application level                          |
| Usually before application server | Usually behind application server          |
| Static content / HTTP responses   | Objects / query results / application data |
| Managed by CDN infrastructure     | Accessed by backend application            |
| Geographically distributed        | Shared application data store              |

Typical architecture:

```text
Client
   ↓
CDN
   ↓
Application Server
   ↓
Redis
   ↓
Database
```

A CDN cache hit can prevent the request from reaching the application server.

A Redis cache hit still requires the request to reach the application server.

---

# 7. Cache-Aside Pattern

The most common caching strategy is **Cache-Aside**, also called **Lazy Loading**.

The application is responsible for reading and writing the cache.

## Read

```text
Application
     ↓
   Cache
     ↓
   HIT?
   /  \
 YES   NO
  ↓     ↓
Return  Database
         ↓
       Cache
         ↓
       Return
```

### Example

```java
Post post = redis.get("post:123");

if (post == null) {
    post = database.getPost(123);
    redis.set("post:123", post);
}

return post;
```

The exact implementation depends on the framework/library.

---

# 8. Cache Key

Every cached value needs a key.

Example:

```text
post:123
user:42
product:1001
```

The key should clearly identify the cached data.

Bad:

```text
123
```

Better:

```text
post:123
```

Even better when necessary:

```text
post:v1:123
```

Good cache keys help with:

* Debugging
* Invalidation
* Namespace separation
* Versioning
* Avoiding collisions

---

# 9. TTL — Time To Live

**TTL** determines how long a cached entry should remain valid.

Example:

```text
post:123
TTL = 10 minutes
```

After 10 minutes, Redis automatically removes/expires the entry.

```text
0 min
 ↓
Cache created

5 min
 ↓
Still valid

10 min
 ↓
Expired
```

TTL is usually configured **per cache entry/key**, not by clearing the entire cache every 5 minutes.

---

# 10. Why Do We Need TTL if We Invalidate the Cache?

Ideally, when data changes:

```text
Update Database
      ↓
Delete Cache
```

But invalidation can fail.

Examples:

### Application crashes

```text
Update DB
   ↓
Application crashes
   ↓
Cache was never deleted
```

### Redis unavailable

```text
Update DB
   ↓
Delete Redis entry
   ↓
Redis unavailable
```

### Another service updates the database

```text
Service A
   ↓
Updates DB

Service B
   ↓
Doesn't know cache needs invalidation
```

### Developer misses an invalidation path

```text
Update Path A → Cache invalidated

Update Path B → Cache NOT invalidated
```

Therefore:

> **Invalidation provides immediate freshness. TTL provides a safety net.**

---

# 11. Choosing TTL

There is no universal TTL.

The important question is:

> **How stale can this data safely be?**

Example:

| Data               |  Possible TTL |
| ------------------ | ------------: |
| Trending data      | 10–60 seconds |
| Search results     |  30 sec–5 min |
| Product details    |      5–30 min |
| User profile       |     10–60 min |
| Configuration      |  30 min–hours |
| Country/state list |    Hours–days |

These are examples, not fixed rules.

---

# 12. Cache Invalidation

Cache invalidation means removing or updating cached data when the underlying data changes.

Example:

```text
Before update:

Database
post:123 → "Hello"

Redis
post:123 → "Hello"
```

Application updates the database:

```text
Database
post:123 → "Hello World"
```

The old cache is now stale.

So:

```text
Update DB
   ↓
Delete post:123 from Redis
```

Next request:

```text
Redis
   ↓
MISS
   ↓
Database
   ↓
"Hello World"
   ↓
Store in Redis
```

---

# 13. Cache Stampede

A **cache stampede** occurs when a popular cache entry expires and many requests try to rebuild it simultaneously.

Suppose:

```text
post:123
TTL = 10 minutes
```

At the moment it expires:

```text
1000 requests
      ↓
   Redis MISS
      ↓
1000 Database Queries
```

This can overload the database.

---

# 14. Preventing Cache Stampede

Common approaches:

### 1. Distributed Lock

Allow only one request to rebuild the cache.

```text
1000 Requests
      ↓
    Redis
      ↓
   Cache MISS
      ↓
One request gets lock
      ↓
   Database
      ↓
Update Cache
      ↓
Other requests use Cache
```

---

### 2. Request Coalescing / Single Flight

Combine concurrent requests for the same missing key so only one database request is made.

---

### 3. Background Refresh

Refresh popular entries before they expire.

```text
Cache
  ↓
Almost expired
  ↓
Background refresh
  ↓
New value
```

---

### 4. TTL Jitter

Instead of giving every key exactly the same TTL:

```text
TTL = 10 minutes
```

use something like:

```text
10 min + random variation
```

This reduces the chance of many keys expiring at exactly the same time.

TTL jitter is particularly useful for avoiding large-scale expiration patterns.

---

# 15. Distributed Lock

A normal Java lock:

```java
synchronized
```

only coordinates threads inside the same JVM.

Consider:

```text
Application 1
Application 2
Application 3
```

Each has its own JVM.

A Java `synchronized` lock cannot coordinate all three.

A distributed lock can use Redis:

```text
Application 1 ─┐
Application 2 ─┼──→ Redis Lock
Application 3 ─┘
```

Only one application instance can acquire the lock for a particular key.

Example conceptual key:

```text
lock:post:123
```

Distributed locks require careful handling of:

* Lock ownership
* Expiration
* Application crashes
* Lock release
* Retry behavior
* Race conditions

---

# 16. Cache Penetration

Cache penetration occurs when requests repeatedly ask for data that **doesn't exist**.

Example:

```text
Request → post:999999
             ↓
          Cache MISS
             ↓
          Database
             ↓
         Not Found
```

If this happens repeatedly:

```text
1000 requests
      ↓
1000 cache misses
      ↓
1000 DB queries
```

The cache provides no protection.

Possible solutions:

* Cache negative results
* Validate input
* Bloom filters

---

# 17. Cache Avalanche

A cache avalanche occurs when a large number of cache entries expire around the same time.

```text
Thousands of keys
       ↓
Expire together
       ↓
Thousands of cache misses
       ↓
Database receives huge traffic
```

Possible solutions:

* TTL jitter
* Different TTLs
* Cache warming
* Background refresh
* Rate limiting
* High availability
* Load shedding

---

# 18. Cache-Aside Write Flow

The common approach:

```text
WRITE

Client
  ↓
Application
  ↓
Update Database
  ↓
Delete Cache
```

Then:

```text
READ

Client
  ↓
Application
  ↓
Redis
  ↓
MISS
  ↓
Database
  ↓
Redis
  ↓
Response
```

This is generally a good starting point because it is simple and gives the application explicit control.

---

# 19. Other Write Strategies

## Write-Through

Application writes to cache, and the cache synchronously writes to the database.

```text
Application
     ↓
   Cache
     ↓
 Database
```

### Advantage

Cache and database are updated together.

### Disadvantage

Every write goes through the cache layer and can add write complexity/latency.

---

## Write-Behind / Write-Back

Application writes to cache first.

The database is updated later.

```text
Application
     ↓
   Cache
     ↓
Async DB Update
```

### Advantage

Very fast writes.

### Disadvantage

If the cache/data layer fails before persistence, data can potentially be lost.

This is more complex and should be used deliberately.

---

# 20. Redis Data Structures

Redis supports several useful data structures.

### String

```text
key → value
```

Example:

```text
user:123:name → "Ramakrishna"
```

---

### Hash

Useful for objects.

```text
user:123

name → Ramakrishna
age  → 24
role → engineer
```

---

### List

Ordered collection.

Useful for:

* Queues
* Activity feeds
* Recent items

---

### Set

Unique values.

Useful for:

* Tags
* Unique users
* Membership

---

### Sorted Set

Values associated with scores.

Useful for:

* Leaderboards
* Ranking
* Priority-based data

---

# 21. Redis Is More Than a Cache

Redis can also be used for:

```text
Redis
 ├── Caching
 ├── Sessions
 ├── Rate Limiting
 ├── Distributed Locks
 ├── Counters
 ├── Leaderboards
 ├── Pub/Sub
 └── Queues
```

Important:

> Redis is a data store. Caching is one of its major use cases.

---

# 22. Rate Limiting vs Caching

Rate limiting and caching are different concepts.

### Caching

Answers:

> "Can I avoid doing this expensive operation again?"

### Rate Limiting

Answers:

> "How many requests should this client be allowed to make?"

For example:

```text
User
 ↓
Rate Limiter
 ↓
Application
 ↓
Redis Cache
 ↓
Database
```

Redis can be used to implement both caching and rate limiting, but they solve different problems.

---

# 23. Cache Consistency

Caching introduces another copy of the data.

```text
Database
   +
Cache
```

Now the system needs to deal with:

```text
Database value ≠ Cache value
```

This creates the fundamental caching trade-off:

> **Performance vs Freshness / Consistency**

The more aggressively we cache, the more carefully we need to think about stale data.

---

# 24. When Should We Cache?

Good candidates:

* Frequently read data
* Expensive database queries
* Expensive computations
* Data that doesn't change frequently
* Data where slightly stale values are acceptable

Examples:

```text
Product details
User profile
Configuration
Popular posts
Search results
Reference data
```

---

# 25. When Should We NOT Cache?

Avoid caching when:

* Data changes extremely frequently
* Every request requires the latest value
* The data is rarely accessed
* Cache invalidation becomes more complex than the performance benefit
* Data is highly user-specific and cache isolation is difficult

Caching is not automatically beneficial.

---

# 26. Important Caching Trade-offs

Caching improves:

```text
Latency ↓
Database Load ↓
Throughput ↑
```

But introduces:

```text
Stale Data
Invalidation Complexity
Memory Usage
Cache Failures
Consistency Problems
Operational Complexity
```

The important engineering question is not:

> "Can I cache this?"

It is:

> **"Is the performance benefit worth the consistency and operational complexity?"**

---

# 27. Mental Model

A simple mental model:

```text
                 ┌─────────────┐
                 │   Client    │
                 └──────┬──────┘
                        ↓
                 ┌─────────────┐
                 │     CDN     │
                 └──────┬──────┘
                        ↓
                 ┌─────────────┐
                 │ Application │
                 └──────┬──────┘
                        ↓
                 ┌─────────────┐
                 │    Redis    │
                 │    Cache    │
                 └──────┬──────┘
                        ↓
                 ┌─────────────┐
                 │  Database   │
                 └─────────────┘
```

Think of each cache as a layer designed to prevent an expensive operation from happening repeatedly.

---

# 28. Key Concepts to Learn Next

Recommended order:

1. **Caching fundamentals**
2. Cache hit / miss
3. Cache key design
4. TTL
5. Cache invalidation
6. Cache-aside pattern
7. Cache stampede
8. Distributed locks
9. Cache penetration
10. Cache avalanche
11. Write-through
12. Write-behind
13. Redis data structures
14. Redis persistence
15. Redis replication
16. Redis Sentinel
17. Redis Cluster
18. Rate limiting
19. Cache monitoring
20. Production caching strategies

---

# 29. InfluenceX Example

For InfluenceX, suppose we have:

```text
GET /posts/123
```

Without caching:

```text
Request
   ↓
Spring Boot
   ↓
PostgreSQL
   ↓
Hibernate
   ↓
PostResponse
```

With Redis caching:

```text
Request
   ↓
Spring Boot
   ↓
Redis
   │
   ├── HIT → Return PostResponse
   │
   └── MISS
         ↓
     PostgreSQL
         ↓
     Store in Redis
         ↓
     Return PostResponse
```

For an update:

```text
PUT /posts/123
       ↓
Update PostgreSQL
       ↓
Delete Redis key
       ↓
post:123
```

Next GET:

```text
GET /posts/123
       ↓
Redis MISS
       ↓
PostgreSQL
       ↓
Latest data
       ↓
Redis
       ↓
Response
```

This is the basic **cache-aside + TTL + invalidation** model.

---

# 30. One-Line Summary

> **Caching is about storing expensive-to-compute or frequently accessed data closer to the consumer so repeated requests can be served faster, at the cost of additional complexity around freshness, invalidation, memory, and consistency.**
