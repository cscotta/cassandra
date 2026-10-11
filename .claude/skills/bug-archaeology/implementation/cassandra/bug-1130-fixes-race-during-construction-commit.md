# Bug 1130: Race During Construction of Commit Log

**Commit**: f8f6fd2d7e · **Ticket**: CASSANDRA-10049
**Subsystem**: src/java/org/apache/cassandra/db/commitlog/
**Category**: Race condition / missing synchronization

---

## Root Cause

The `CommitLogSegmentManager` and `AbstractCommitLogService` constructors both called `start()` internally, spawning worker threads before the owning `CommitLog` object was fully initialized and published. Because `CommitLog.instance` is a static field assigned at class-initialization time, threads started inside the constructor could observe a partially constructed `CommitLog` or could call back into the singleton before it was fully set up. Additionally, `CommitLogReplayer` referenced `CommitLog.instance` statically rather than receiving the instance it was meant to replay, creating hidden coupling that broke when multiple `CommitLog` instances were used in tests. The fix moves all thread starts out of constructors into an explicit `start()` method called only after full construction.

### Before / After

```java
// BEFORE: CommitLogSegmentManager constructor starts thread immediately
public CommitLogSegmentManager(final CommitLog commitLog)
{
    this.commitLog = commitLog;
    start();  // thread starts before caller finishes initializing CommitLog
}

// CommitLogReplayer uses global singleton
long restoreTime = CommitLog.instance.archiver.restorePointInTime;
```

```java
// AFTER: thread start separated; caller controls when it begins
public CommitLogSegmentManager(final CommitLog commitLog)
{
    this.commitLog = commitLog;
    // no start() here
}

public void start() { ... }  // called explicitly after full construction

// CommitLogReplayer uses the injected instance
long restoreTime = commitLog.archiver.restorePointInTime;
```

---

## How to Spot

When a class spawns threads or registers JMX beans inside its constructor, any access to `this` or static singletons from those threads races against the remainder of the constructor in the caller. Review constructors of long-lived service classes for `thread.start()`, `executor.start()`, or `new Thread(...).start()` calls; if any exist before the constructor returns, ask whether the object is fully visible at that point. The companion smell is methods that reference a global singleton (`Foo.instance`) instead of using an injected reference — this hard-wires an assumption that the singleton is already initialized and makes the code fragile under test or staged startup.

---

## Test

The stress test (`CommitLogStressTest`) and unit test (`CommitLogTest`) were updated to call `.start()` explicitly after construction, and `CommitLogTestReplayer` was updated to accept the `CommitLog` instance rather than rely on the static singleton, covering the race at construction time.
