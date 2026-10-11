# Bug 1257: CommitLog getCompletedTasks Incorrectly Increments Counter on Every Read

**Commit**: ac70e37d39 · **Ticket**: CASSANDRA-9339
**Subsystem**: src/java/org/apache/cassandra/db/commitlog/AbstractCommitLogService.java
**Category**: Logic error in condition

---

## Root Cause

`AbstractCommitLogService.getCompletedTasks()` called `written.incrementAndGet()` instead of `written.get()`. Every JMX or monitoring poll of the completed-tasks metric silently mutated the counter, inflating the reported value by one for each observation. This is a confusion between a read accessor and an atomic update: `incrementAndGet` returns the post-increment value and is appropriate only when recording that a task completed, not when reading the current tally.

### Before / After

```java
// BEFORE
public long getCompletedTasks()
{
    return written.incrementAndGet();
}
```

```java
// AFTER
public long getCompletedTasks()
{
    return written.get();
}
```

---

## How to Spot

Whenever a method is named `getXxx` or `readXxx` and its return type is a primitive counter, verify that it only reads the underlying atomic variable (`get()`) and never mutates it (`incrementAndGet`, `getAndIncrement`, etc.). Increment calls belong in the code path that marks work done, not in the observer. A quick grep for `incrementAndGet` inside getter-named methods will surface this class of mistake immediately.

---

## Test

No test added.
