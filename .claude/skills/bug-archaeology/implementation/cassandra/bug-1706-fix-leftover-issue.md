# Bug 1706: Fix CASSANDRA-6714 leftover issue

**Commit**: e0c125fce0 · **Ticket**: CASSANDRA-6714
**Subsystem**: db/commitlog/CommitLogReplayer
**Category**: Wrong constant / default value

---

## Root Cause

After the CASSANDRA-6714 refactor, `CommitLogReplayer` still called `desc.getMessagingVersion()` to obtain the version when computing the start offset for replay. The correct method after the refactor is `desc.getVersion()`. Calling the wrong accessor returned an incorrect version number, which could cause the replayer to compute a wrong start offset and either skip valid mutations or attempt to replay already-applied data.

### Before / After

```java
// BEFORE
int offset = getStartOffset(segmentId, desc.getMessagingVersion());
```

```java
// AFTER
int offset = getStartOffset(segmentId, desc.getVersion());
```

---

## How to Spot

After a rename/refactor that replaces an accessor, search for all remaining callers of the old method name — a stale call site is easily missed when the old method still compiles (e.g., via a deprecated overload or an unrelated method with the same name).

---

## Test

No test added.
