# Bug 0923: PurgeFunction Passes gcBefore as nowInSec, Causing Assertion Error During Compaction

**Commit**: 11da411fb6 · **Ticket**: CASSANDRA-11353
**Subsystem**: Compaction / Tombstone purging
**Category**: Wrong constant / default value

---

## Root Cause
`PurgeFunction` stored and used `gcBefore` (an epoch-seconds value in the past representing the garbage-collection cutoff) where the code actually needed `nowInSec` (the current time in seconds). Two distinct purposes were conflated in the same field. `Row.purge(purger, nowInSec)` uses the second argument to determine whether expiring cells have actually expired yet; passing `gcBefore` instead of the current time caused the comparison to use a stale time anchor, which in edge cases (e.g., `gcBefore == localDeletionTime`) could satisfy an internal assertion that expected the cell to still be live. A secondary bug in `AbstractCell.toTombstone()` omitted the `path()` argument when converting an expiring cell to a tombstone, producing a tombstone with a null path for collection cells.

### Before / After
```java
// BEFORE — PurgeFunction constructor stored gcBefore as the time field
public PurgeFunction(boolean isForThrift, int gcBefore,
                     int oldestUnrepairedTombstone, boolean onlyPurgeRepairedTombstones) {
    this.gcBefore = gcBefore;
    ...
}
// Used as:
return row.purge(purger, gcBefore);

// AFTER — nowInSec added as a separate parameter
public PurgeFunction(boolean isForThrift, int nowInSec, int gcBefore,
                     int oldestUnrepairedTombstone, boolean onlyPurgeRepairedTombstones) {
    this.nowInSec = nowInSec;
    ...
}
// Used as:
return row.purge(purger, nowInSec);

// AbstractCell — path() was omitted
// BEFORE
return BufferCell.tombstone(column, timestamp(), localDeletionTime() - ttl());
// AFTER
return BufferCell.tombstone(column, timestamp(), localDeletionTime() - ttl(), path());
```

---

## How to Spot
When a class stores a time-like integer, verify that every use site is using the correct semantic (current time vs. expiry cutoff). Compaction code handles at least two time-related concepts—`nowInSec` (current time for TTL evaluation) and `gcBefore` (cutoff for tombstone purging)—and confusing them produces silent data-correctness bugs. Review every call to `Row.purge` and similar methods to confirm which time value is passed.

---

## Test
`TTLExpiryTest.testBug10944` — reproduces the assertion error by setting `gcGraceSeconds=1` so that `gcBefore` equals `localDeletionTime`, and verifies that compaction correctly retains the remaining SSTable rather than asserting.
