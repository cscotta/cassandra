# Bug 0522: Deadlock During CommitLog Initialization on Startup Error

**Commit**: 3a8300e0b8 · **Ticket**: CASSANDRA-15295
**Subsystem**: src/java/org/apache/cassandra/db/commitlog/CommitLog.java
**Category**: Deadlock

---

## Root Cause

`CommitLog.construct()` called `log.start()` inline during static initialisation of the `CommitLog.instance` singleton field. If `start()` threw an exception (e.g., a disk error during segment manager startup), the exception propagated out of the static initialiser, leaving `CommitLog.instance` in an uninitialised state. Any subsequent attempt to access `CommitLog.instance` — including from error-handling code or the `shutdownBlocking()` path — would re-trigger the static initialiser, which could re-enter `start()` and attempt to acquire internal locks already held by the interrupted initialisation sequence, producing a deadlock.

The fix separates construction from startup: `construct()` no longer calls `start()`, and `start()` is made `synchronized` and idempotent via a `started` boolean. `shutdownBlocking()` is similarly guarded so calling it before a successful start is a no-op.

### Before / After

```java
// BEFORE — start() called eagerly inside static initialiser
private static CommitLog construct() {
    CommitLog log = new CommitLog(CommitLogArchiver.construct());
    MBeanWrapper.instance.registerMBean(log, "...");
    return log.start();  // exception here leaves instance partially init'd
}
```

```java
// AFTER — start() is separate, idempotent, and synchronized
private static CommitLog construct() {
    CommitLog log = new CommitLog(CommitLogArchiver.construct(),
                                  DatabaseDescriptor.getCommitLogSegmentMgrProvider());
    MBeanWrapper.instance.registerMBean(log, "...");
    return log;  // no start here
}

synchronized public CommitLog start() {
    if (started) return this;
    try {
        segmentManager.start();
        executor.start();
        started = true;
    } catch (Throwable t) { started = false; throw t; }
    return this;
}
```

---

## How to Spot

Static initialisers that call methods which can throw, and whose exception path re-enters the same class, are classic deadlock setups in Java. Any `static final Foo INSTANCE = new Foo().start()` pattern warrants scrutiny: if `start()` fails, the class becomes permanently broken and further access hangs. Look for JVM class-loading locks combined with internal `synchronized` methods in the same class.

---

## Test

`CommitLogInitWithExceptionTest` verifies that when `segmentManager.start()` throws, `CommitLog.start()` propagates the exception without leaving the instance in a state that deadlocks on subsequent calls.
