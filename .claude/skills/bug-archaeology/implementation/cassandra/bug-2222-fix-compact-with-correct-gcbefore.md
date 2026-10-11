# Bug 2222: HintedHandOffManager Compacts Hints with Integer.MAX_VALUE gcBefore, Deleting Live Hints

**Commit**: 0d12493721 · **Ticket**: CASSANDRA-4772
**Subsystem**: src/java/org/apache/cassandra/db/HintedHandOffManager.java
**Category**: Wrong constant / default value

---

## Root Cause

`HintedHandOffManager.compact()` submitted the hints column family for user-defined compaction passing `Integer.MAX_VALUE` as the `gcBefore` timestamp. This value is far in the future, so every tombstone — including tombstones for hint rows that had not yet been delivered — was treated as eligible for garbage collection. As a result, compacting the hints CF could permanently delete undelivered hints, causing data loss for nodes that were temporarily down.

The correct value is the current wall-clock time in seconds (`System.currentTimeMillis() / 1000`), which matches the standard `gcBefore` semantics used throughout the rest of the compaction code.

### Before / After

```java
// BEFORE – treats all tombstones as expired
return CompactionManager.instance.submitUserDefined(hintStore, descriptors, Integer.MAX_VALUE);
```

```java
// AFTER – only expires tombstones older than now
return CompactionManager.instance.submitUserDefined(hintStore, descriptors,
        (int) (System.currentTimeMillis() / 1000));
```

---

## How to Spot

Any call to a compaction API that accepts a `gcBefore` parameter should be scrutinised for sentinel values like `0`, `Integer.MAX_VALUE`, or `Long.MAX_VALUE`. These constants bypass the gc_grace_seconds safety window and can silently delete data that has not yet been fully replicated or delivered.

---

## Test

A new `testCompactionOfHintsCF` test verifies that after compacting the hints CF, hint rows protected by `gc_grace_seconds` are not removed.
