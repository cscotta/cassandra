# Bug 0136: CommitLogArchiver Field Missing volatile in CommitLog

**Commit**: 63cad45bfc · **Ticket**: CASSANDRA-19960
**Subsystem**: src/java/org/apache/cassandra/db/commitlog
**Category**: Race condition / missing synchronization

---

## Root Cause

`CommitLog.archiver` is a non-`final`, non-`volatile` field that is written by one thread (e.g., a test setup or archiver reset path) and read by other threads (replay, segment management). Without the `volatile` keyword, the Java Memory Model does not guarantee that a write performed by one thread will ever be visible to any other thread, so readers may see a stale or null reference. The single-character fix adds `volatile` to the field declaration so that every write is immediately flushed to main memory and every read picks up the latest value.

### Before / After

```java
// BEFORE
public CommitLogArchiver archiver;
```

```java
// AFTER
public volatile CommitLogArchiver archiver;
```

---

## How to Spot

Whenever a mutable singleton or lazily-assigned field is accessed from multiple threads without explicit synchronization or an `AtomicReference`, check whether the field is at least `volatile`. The absence of `volatile`, `final`, or a lock on a shared reference field is a strong indicator of a visibility race that can produce subtle, intermittent failures under load or JIT optimization.

---

## Test

No test added (the fix is a one-word visibility guarantee; the accompanying CASSANDRA-19448 commit added the surrounding test infrastructure).
