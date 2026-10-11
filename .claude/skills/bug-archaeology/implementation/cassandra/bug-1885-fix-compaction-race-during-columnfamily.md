# Bug 1885: SSTable Reference Leak When Compaction Finishes After Column Family Drop

**Commit**: d40f3c8d7f · **Ticket**: CASSANDRA-5957
**Subsystem**: src/java/org/apache/cassandra/db/DataTracker.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`DataTracker.unreferenceSSTables()` — called when a column family (CF) is invalidated/dropped — only released references for SSTables that were not currently marked as being compacted. If a compaction started before the CF was dropped and then finished (or was interrupted) after invalidation, `unmarkCompacting()` did not call `unreferenceSSTables()` for those tables, leaving their references permanently held and their disk space unreleased.

Additionally, `removeOldSSTablesSize()` contained an unconditional `assert firstToCompact` that fired when the same SSTable was encountered twice during invalidation cleanup (once by the drop path, once by the finishing compaction), causing a false assertion failure.

The fix captures the `isValid` state once in `unmarkCompacting()`, then — after the CAS loop that removes the compacting mark — calls `unreferenceSSTables()` when the CF is no longer valid. A new `tolerateCompacted` flag passed through `postReplace()` into `removeOldSSTablesSize()` softens the assertion to allow already-compacted SSTables during drop-path cleanup.

### Before / After

```java
// BEFORE — no cleanup when compaction finishes after invalidation
public void unmarkCompacting(Collection<SSTableReader> unmark) {
    if (!cfstore.isValid()) { ... /* only handles the drop case, not in-flight compactions */ }
    // CAS loop to remove compacting mark
    // (no post-invalidation unreference)
}
```

```java
// AFTER — unreference SSTables if CF was invalidated while compacting
public void unmarkCompacting(Collection<SSTableReader> unmark) {
    boolean isValid = cfstore.isValid();
    if (!isValid) { ... }
    // CAS loop
    if (!isValid) {
        unreferenceSSTables();
    }
}
```

---

## How to Spot

Whenever a resource-tracking structure distinguishes "currently in use" from "idle" state, confirm that cleanup code runs for resources in the "in use" state when the owning object is destroyed concurrently. Look for cleanup methods that guard themselves with an `isValid()` / `isAlive()` predicate — each such guard is a potential leak if the transition to invalid happens between the guard check and the use.

---

## Test

No test added.
