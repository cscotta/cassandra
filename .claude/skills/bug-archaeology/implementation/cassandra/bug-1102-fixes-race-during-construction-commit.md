# Bug 1102: Fix Race During Construction of Commit Log

**Commit**: 01a11fd262 · **Ticket**: CASSANDRA-10049
**Subsystem**: src/java/org/apache/cassandra/db/commitlog/CommitLog.java, src/java/org/apache/cassandra/db/commitlog/CommitLogSegmentManager.java, src/java/org/apache/cassandra/db/commitlog/CommitLogArchiver.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`CommitLogSegmentManager` started its background management thread from inside its own constructor, and `CommitLogArchiver` similarly used constructor-side initialization that could reference partially constructed state. The management thread could therefore begin executing — and calling back into the owning `CommitLog` object — before the `CommitLog` constructor had finished assigning all its fields. This is the classic "thread escape from constructor" race: the `this` reference is visible to the new thread before the object is fully initialised. The fix moves all thread starts out of constructors and into an explicit `start()` method called only after construction of every object in the dependency graph is complete.

### Before / After

```java
// BEFORE — thread starts inside constructor
public CommitLogSegmentManager(final CommitLog commitLog)
{
    this.commitLog = commitLog;
    start();   // thread can observe partially-constructed CommitLog
}
```

```java
// AFTER — thread started explicitly after construction
CommitLogSegmentManager(final CommitLog commitLog)
{
    this.commitLog = commitLog;
    // start() called externally via CommitLog.start()
}

void start() { ... }
```

---

## How to Spot

Any class that spawns a background thread inside its constructor is susceptible to the "this escape" race: the new thread may read fields that the constructor has not yet written. The pattern is easy to identify — look for `new Thread(...)`, `executor.start()`, or similar calls inside constructors, especially when the runnable captures `this`. The safe pattern is a two-phase init: construct, then call `start()` from the factory method or caller after all fields are set.

---

## Test

No test added (existing `CommitLogStressTest` and related tests were updated to use the new API).
