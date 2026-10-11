# Bug 1959: CAS contention loop ran forever due to inverted elapsed-time check

**Commit**: 4b4ccc3ccf · **Ticket**: CASSANDRA-5830
**Subsystem**: src/java/org/apache/cassandra/service/StorageProxy.java
**Category**: Logic error in condition

---

## Root Cause

The Compare-And-Set (CAS / lightweight transaction) retry loop in `StorageProxy` was meant to keep retrying as long as the elapsed time since `start` was less than the configured contention timeout. The condition was written as `start - System.nanoTime() < timeout`: since `System.nanoTime()` grows monotonically, this expression is always a large negative number, which is always less than the positive `timeout`. The loop therefore never exited due to the timeout, effectively spinning forever under contention. The fix reverses the operands to `System.nanoTime() - start < timeout`, which correctly measures elapsed nanoseconds.

### Before / After

```java
// BEFORE
while (start - System.nanoTime() < timeout)
```

```java
// AFTER
while (System.nanoTime() - start < timeout)
```

---

## How to Spot

Elapsed-time guards must always compute `now - start`. Writing `start - now` produces a permanently negative value that satisfies any positive bound, creating an infinite loop. In code review, confirm the operand order in every `System.nanoTime()` / `System.currentTimeMillis()` timeout check.

---

## Test

No test added.
