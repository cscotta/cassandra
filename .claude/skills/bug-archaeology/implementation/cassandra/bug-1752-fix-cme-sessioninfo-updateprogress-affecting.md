# Bug 1752: Fix CME in SessionInfo updateProgress Affecting Netstats

**Commit**: 468cc72fac · **Ticket**: CASSANDRA-6577
**Subsystem**: src/java/org/apache/cassandra/streaming/SessionInfo.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`SessionInfo` maintained `receivingFiles` and `sendingFiles` as plain `HashMap` instances. The `updateProgress` method was protected with `synchronized`, but iterating callers (e.g., netstats display) accessed the maps without holding the same lock, producing `ConcurrentModificationException`. The fix replaces both maps with `ConcurrentHashMap` and removes the now-redundant `synchronized` keyword from `updateProgress`, letting the concurrent map handle thread safety for all access patterns.

### Before / After

```java
// BEFORE
this.receivingFiles = new HashMap<>();
this.sendingFiles = new HashMap<>();
...
public synchronized void updateProgress(ProgressInfo newProgress)
```

```java
// AFTER
this.receivingFiles = new ConcurrentHashMap<>();
this.sendingFiles = new ConcurrentHashMap<>();
...
public void updateProgress(ProgressInfo newProgress)
```

---

## How to Spot

When a collection is protected by `synchronized` on write but read from external callers without holding the same lock, a concurrent-safe collection should be used instead of patching individual methods. Review every field that is a mutable collection and check whether all read paths are also synchronized.

---

## Test

No test added.
