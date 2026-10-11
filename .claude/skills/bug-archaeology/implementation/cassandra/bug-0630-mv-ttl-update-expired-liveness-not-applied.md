# Bug 0630: MV Old Entry Not Shadowed When Base Row Updated with TTL

**Commit**: 461af5b9a6 · **Ticket**: CASSANDRA-14071
**Subsystem**: `src/java/org/apache/cassandra/db/LivenessInfo.java`, `org/apache/cassandra/db/view/ViewUpdateGenerator.java`
**Category**: Logic error in condition

---

## Root Cause

When a materialized view (MV) uses a non-primary-key base column as a view primary key column, updating that base column requires the old view entry to be invalidated. `ViewUpdateGenerator.deleteOldEntryInternal` writes a special "expired liveness" marker (using `Integer.MAX_VALUE` as the TTL flag) to shadow the old view row without emitting a full tombstone, allowing it to co-exist with a row tombstone. The `supersedes()` logic in `LivenessInfo` previously treated all `ExpiringLivenessInfo` instances identically, comparing only `localExpirationTime`. Because an expired liveness entry created with TTL `Integer.MAX_VALUE` has the same wall-clock expiration as a legitimately long-lived entry, the conflict resolution could pick the live entry over the expired tombstone, leaving the stale view row visible. The fix introduces a dedicated `ExpiredLivenessInfo` subclass that overrides `isExpired()` and `isLive()`, and updates `supersedes()` to give expired liveness priority over any live liveness at the same timestamp.

### Before / After

```java
// BEFORE – expired marker can lose conflict resolution to live liveness
if (isExpiring() == other.isExpiring())
    return localExpirationTime() > other.localExpirationTime();
return isExpiring();
```

```java
// AFTER – expired liveness wins at same timestamp regardless of expiration time
if (isExpired() ^ other.isExpired())
    return isExpired();   // expired (tombstone) always wins on tie
if (isExpiring() == other.isExpiring())
    return localExpirationTime() > other.localExpirationTime();
return isExpiring();
```

---

## How to Spot

When a special sentinel value (here `Integer.MAX_VALUE` as a TTL) is repurposed as a flag and fed into a comparison function that has no awareness of the flag, the comparison will treat the sentinel as a normal value and produce logically incorrect ordering. Reviewers should check every path that reads a TTL field for a guard that distinguishes real TTLs from sentinel-flag values before performing arithmetic or ordering comparisons.

---

## Test

`ViewLongTest.testExpiredLivenessInfoWithDefaultTTLWithFlush/WithoutFlush` inserts and updates rows in a table with `default_time_to_live`, waits for TTL expiry, and asserts both the base table and the materialized view return no rows; it also validates that an intermediate update that shadows an old MV entry and then gets updated again leaves the correct final row visible.
