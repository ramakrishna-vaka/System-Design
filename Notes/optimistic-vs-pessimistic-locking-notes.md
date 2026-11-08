# Optimistic Locking vs Pessimistic Locking

## 1. Why do we need locking?

In a concurrent application, multiple users or requests may try to read and update the same database record at the same time.

Without concurrency control, one update might overwrite another update. This is commonly called a **lost update**.

### Example

An employee's salary is ₹50,000.

- HR A reads the salary: ₹50,000.
- HR B also reads the salary: ₹50,000.
- HR A increases it to ₹55,000 and saves.
- HR B updates using the old value and saves.

If the application does not detect the conflict, HR B's update can overwrite HR A's update.

Locking strategies help coordinate concurrent updates and protect data consistency.

---

## 2. Optimistic Locking

### What is it?

Optimistic locking assumes that conflicts between concurrent updates are relatively uncommon.

It allows multiple transactions to read and work with the same record. When a transaction updates the record, the database checks whether another transaction has changed it since it was read.

- If the record has not changed, the update succeeds.
- If another transaction has already changed it, the stale update fails with an optimistic locking conflict.

**Simple meaning:** Let everyone work, but check for conflicts when they save.

### How does it work?

A common implementation uses a **version column**.

Example table:

| id | name | salary | version |
|---:|---|---:|---:|
| 1 | Ravi | 50000 | 0 |

Two transactions, A and B, both read the record with `version = 0`.

1. Transaction A changes the salary to ₹55,000.
2. Transaction B changes the salary to ₹60,000.
3. A updates the row successfully. The version becomes `1`.
4. B tries to update using the old version (`0`).
5. The database finds that the current version is `1`, not `0`, so B's update affects zero rows.
6. The application reports an optimistic locking conflict instead of silently overwriting A's update.

Conceptually, the SQL generated for A is:

```sql
UPDATE employees
SET salary = 55000, version = 1
WHERE id = 1 AND version = 0;
```

B's update also uses `WHERE id = 1 AND version = 0`. Since the version is now `1`, B's update does not match the row.

### Implementing it with Spring Data JPA

Add `@Version` to a version field in the entity:

```java
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    private Long id;

    private String name;

    private BigDecimal salary;

    @Version
    private Long version;

    // constructors, getters and setters
}
```

Hibernate/JPA manages the version value and checks it during updates of the managed entity.

A typical service method:

```java
@Transactional
public Employee incrementSalary(Long id, BigDecimal amount) {
    Employee employee = employeeRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Employee not found"));

    employee.setSalary(employee.getSalary().add(amount));

    return employeeRepository.saveAndFlush(employee);
}
```

`saveAndFlush()` forces pending changes to be sent to the database before the method returns. A conflict can therefore become visible at the flush. A transaction can still fail at commit in some situations.

### What happens on conflict?

The application may receive an exception such as:

- `OptimisticLockException` (JPA)
- `ObjectOptimisticLockingFailureException` (Spring)

The application can ask the user to refresh and retry, or apply a suitable retry strategy where safe.

### Real-life analogies / use cases

- **Ticket or issue editing:** Two team members open the same ticket. One saves changes first; the other is warned that the item has changed and may need to refresh. This illustrates optimistic concurrency control. Jira is an issue-tracking tool, but do not claim every Jira operation uses JPA-style version locking.
- Editing employee profiles.
- Updating product descriptions or catalog metadata.
- Editing application configuration.
- Updating records that are read frequently but changed relatively infrequently.

### Advantages

- No database row lock is held while the user is simply reading or editing.
- Good concurrency when conflicts are uncommon.
- Transactions generally do not wait for one another just to read the same row.
- Useful for read-heavy workloads.

### Disadvantages

- An update can fail if another transaction changed the record first.
- The application needs a conflict-handling strategy.
- Repeated conflicts can cause retries and poor user experience.
- It is not automatically suitable for every high-contention operation.

---

## 3. Pessimistic Locking

### What is it?

Pessimistic locking assumes that conflicts are possible and protects a record by acquiring a database lock before making changes.

When one transaction holds a write lock on a row, another transaction that requests an incompatible lock on that row generally has to wait, or it may fail because of a timeout or deadlock.

**Simple meaning:** Lock the record first; make competing transactions wait.

### How does it work?

Suppose the employee's salary is ₹50,000.

1. Transaction A requests the employee row with a write lock.
2. A reads the salary and updates it.
3. Transaction B requests a conflicting lock on the same row.
4. B waits while A holds the lock.
5. A commits, and its lock is released.
6. B can then acquire the lock and read the latest committed value.
7. B applies its update and commits.

If A increases the salary by ₹5,000 and B increases it by ₹10,000, both increments can be applied sequentially, resulting in ₹65,000, assuming both transactions succeed and each calculates its increment from the value it reads under the lock.

### Implementing it with Spring Data JPA

Define a repository method with `PESSIMISTIC_WRITE`:

```java
public interface EmployeeRepository
        extends JpaRepository<Employee, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Employee e WHERE e.id = :id")
    Optional<Employee> findByIdForUpdate(@Param("id") Long id);
}
```

Use that method inside a transaction:

```java
@Transactional
public Employee incrementSalary(Long id, BigDecimal amount) {
    Employee employee = employeeRepository.findByIdForUpdate(id)
        .orElseThrow(() -> new RuntimeException("Employee not found"));

    employee.setSalary(employee.getSalary().add(amount));

    return employeeRepository.saveAndFlush(employee);
}
```

The database/provider obtains the lock as part of the query. The lock is normally held until the transaction completes (commit or rollback).

### Real-life analogies / use cases

- **Bank account updates:** Serialize operations that modify the same account balance when the chosen transaction design requires a row lock.
- **Inventory reservation:** Coordinate requests competing for the last available unit.
- **Seat or room booking:** Prevent concurrent transactions from claiming the same resource.
- Critical workflows where conflicting updates are frequent or expensive to resolve.

These are scenarios where pessimistic locking may be useful. Production systems may also use atomic conditional updates, unique constraints, reservations, or other concurrency-control techniques.

### Advantages

- Prevents competing transactions from simultaneously modifying a locked row.
- Avoids stale-write conflicts for operations protected by the lock.
- Useful when contention is high and conflicts are costly.

### Disadvantages

- Transactions may wait, increasing latency.
- Locks can reduce throughput when many requests contend for the same rows.
- Long-running transactions can block other work.
- Deadlocks and lock timeouts are possible and must be handled.

---

## 4. Optimistic vs Pessimistic Locking

| Aspect | Optimistic locking | Pessimistic locking |
|---|---|---|
| Assumption | Conflicts are uncommon | Conflicts are possible / likely |
| Mechanism | Checks a version when updating | Acquires a database lock |
| Concurrent reads | Usually allowed | Usually allowed, depending on lock type and database |
| Competing update | One update may fail due to a stale version | Competing transaction generally waits or times out |
| Main cost | Conflict handling and retries | Waiting, blocking, and lock management |
| Best suited for | Low-contention, read-heavy data | High-contention data where conflicts are costly |

### Easy way to remember

- **Optimistic = Check before accepting the update.**
- **Pessimistic = Lock before allowing the update.**

---

## 5. Important Spring Boot / JPA points

### `@Version`

- Enables version-based optimistic locking for normal JPA-managed entity updates.
- The version is incremented by the persistence provider.
- Do not manually increment the version field.
- A stale update causes an optimistic locking exception.
- Bulk JPQL or native SQL updates bypass normal entity dirty-checking and do not automatically provide the same version check. Handle version checks explicitly for such updates.

### `@Lock(LockModeType.PESSIMISTIC_WRITE)`

- Requests a database-level write lock for the selected entity/row.
- Use it inside a transaction so the lock remains held while the operation is performed.
- The exact SQL and lock behavior depend on the database and JPA provider.
- Other transactions may wait, time out, or encounter a deadlock.

### Why is `@Transactional` important?

For pessimistic locking, the transaction defines how long the lock is held. The lock is normally released when the transaction commits or rolls back.

For optimistic locking, the update and version check must participate in the transaction. A failure can occur during flush or commit.

### Does `saveAndFlush()` solve concurrency by itself?

No. `saveAndFlush()` sends pending changes to the database earlier; it does not itself provide optimistic or pessimistic locking. The version check comes from `@Version`, while the database lock comes from the pessimistic lock request.

---

## 6. A practical concurrency experiment

Use two concurrent requests against the same employee record.

### Optimistic test

1. Both requests read the same employee and version.
2. Add a short test-only delay after reading and before updating, so the requests overlap.
3. Both try to update the salary.
4. One update succeeds; the other should fail with an optimistic locking conflict because it uses a stale version.

### Pessimistic test

1. Request A obtains the row write lock.
2. Request B requests the same row and waits.
3. A updates and commits.
4. B obtains the lock, reads the latest value, updates, and commits.

For an increment-based test, if the initial salary is ₹50,000 and the two successful increments are ₹1,000 and ₹2,000, the final salary should be ₹53,000.

**Important:** A sleep is only a demonstration aid, not a deterministic concurrency test. Remove test delays from production code. For reliable automated tests, coordinate transactions with barriers/latches and use the same database engine as the application.

---

## 7. Which one should we choose?

There is no universally better option.

Choose **optimistic locking** when:
- Concurrent updates to the same record are relatively rare.
- You want to avoid holding database locks while users work.
- The application can handle a conflict by refreshing or retrying.

Choose **pessimistic locking** when:
- Many transactions compete for the same records.
- A conflict is expensive to recover from.
- Waiting briefly is preferable to allowing competing operations to proceed and then rejecting one.

The choice depends on contention, transaction duration, database behavior, and how the application handles conflicts.

---

## 8. Short video-ready explanation

"Optimistic locking assumes conflicts are rare. So, multiple users can read and work on the same record. When they save, the application checks whether someone else has modified that record in the meantime. If the version has changed, the stale update is rejected.

Pessimistic locking takes the opposite approach. It assumes conflicts may happen, so it locks the record before updating it. If another transaction tries to update the same record, it has to wait until the lock is released.

So the simple difference is: optimistic locking detects conflicts when saving, whereas pessimistic locking prevents competing updates by locking the record first."

## 9. Quick interview questions

**Q1. Does optimistic locking lock the database row while reading?**  
Usually no. It relies on a version check when updating rather than holding a row write lock throughout the transaction.

**Q2. What happens if two transactions update the same version?**  
One update succeeds and increments the version. The other update's version condition no longer matches, so it fails with an optimistic locking conflict.

**Q3. Does pessimistic locking always make the second transaction wait?**  
Not necessarily. Depending on database settings, timeout, and deadlock conditions, it may wait, time out, or fail.

**Q4. Can we use both `@Version` and pessimistic locking?**  
Yes, but they serve different purposes. Pessimistic locking coordinates access through a database lock; `@Version` can still provide version tracking and stale-state protection. Use the combination deliberately.

**Q5. Is optimistic locking always faster?**  
No. It can perform well when conflicts are rare. Under heavy contention, repeated conflicts and retries may make it less efficient.

**Q6. Is pessimistic locking always safer?**  
It prevents certain concurrent update conflicts while the lock is held, but it can introduce blocking, deadlocks, and timeouts. Correct transaction design is still essential.
