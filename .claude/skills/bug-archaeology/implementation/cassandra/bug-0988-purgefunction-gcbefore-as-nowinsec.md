# Bug 0988: PurgeFunction Passes gcBefore as nowInSec, Causing Assertion Error During Compaction

**Commit**: 1d7bacc45f · **Ticket**: CASSANDRA-10944
**Subsystem**: Compaction / Tombstone purging
**Category**: Wrong constant / default value

---

## Root Cause
`PurgeFunction` stored only `gcBefore` (an epoch-seconds boundary for purging tombstones) but then passed it to `Row.purge(purger, nowInSec)` as if it were the current time in seconds. When `gcBefore` equalled the cell's `localDeletionTime`, the assertion inside `BufferCell.purge` that guards against creating a tombstone with a negative TTL (`localDeletionTime - ttl`) fired, crashing compaction. The fix adds a separate `nowInSec` field to `PurgeFunction` (passed at construction time) and uses it where the current wall-clock time is expected, while keeping `gcBefore` solely for the purge eligibility check.

### Before / After
```java
// BEFORE — gcBefore used as nowInSec
public PurgeFunction(boolean isForThrift, int gcBefore, int oldestUnrepairedTombstone, ...)
{
    this.gcBefore = gcBefore;
    ...
}
public Row applyToRow(Row row) { return row.purge(purger, gcBefore); }  // wrong

// AFTER — nowInSec passed separately
public PurgeFunction(boolean isForThrift, int nowInSec, int gcBefore, int oldestUnrepairedTombstone, ...)
{
    this.nowInSec = nowInSec;
    ...
}
public Row applyToRow(Row row) { return row.purge(purger, nowInSec); }  // correct
```

---

## How to Spot
Any site that constructs `PurgeFunction` (or a subclass) should pass two separate time values: the current time (`nowInSec`) for row-level expiry decisions and `gcBefore` for tombstone eligibility. If the constructor only takes one time parameter and uses it for both purposes, that is the bug pattern.

---

## Test
`TTLExpiryTest.testBug10944` added — sets `gcGraceSeconds=1` so that `gcBefore` equals the cell's `localDeletionTime`, reproducing the assertion crash before the fix.
