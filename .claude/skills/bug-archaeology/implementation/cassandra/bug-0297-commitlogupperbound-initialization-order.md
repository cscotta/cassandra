# Bug 0297: commitLogUpperBound Unset When writeBarrier Is Initialized in AbstractMemtableWithCommitlog

**Commit**: 60b54425ed · **Ticket**: CASSANDRA-17587
**Subsystem**: src/java/org/apache/cassandra/db/memtable/AbstractMemtableWithCommitlog.java
**Category**: Race condition / missing synchronization

---

## Root Cause

In `AbstractMemtableWithCommitlog#setDiscarding`, the `writeBarrier` field was assigned before `commitLogUpperBound`, reversing the intended initialization order. Code that reads `commitLogUpperBound` after observing `writeBarrier` (e.g., during flush) could therefore see a null upper bound. The fix swaps the assignment order so that `commitLogUpperBound` is always fully written before `writeBarrier` is published. Additionally, the `getCommitLogUpperBound()` method was renamed to `getFinalCommitLogUpperBound()` and hardened with assertions to make it illegal to call it before the bound is sealed.

### Before / After

```java
// BEFORE
assert this.writeBarrier == null;
this.writeBarrier = writeBarrier;
this.commitLogUpperBound = commitLogUpperBound;
```

```java
// AFTER
assert this.writeBarrier == null;
this.commitLogUpperBound = commitLogUpperBound;
this.writeBarrier = writeBarrier;
```

---

## How to Spot

When two related fields must be consistent from the perspective of other threads, the "publishing" field (the one that acts as a visibility signal) must always be written last. Review any initialization sequence where one field is a sentinel or barrier: if the barrier is set before the data it guards, any concurrent reader that checks the barrier will see stale or null dependent state. Renaming the accessor to express its post-initialization-only validity (`getFinal*`) is a good defensive measure.

---

## Test

`CommitLogCQLTest#testSwitchMemtable` runs 10 concurrent writer threads that continuously insert rows and call `dumpMemtable()` for 15 seconds, asserting that no exceptions are thrown during rapid memtable switching.
