# Bug 0589: Commitlog Flush Lag Logged for Marker Syncs, Not Disk Flushes

**Commit**: 214a3abfcc · **Ticket**: CASSANDRA-14451
**Subsystem**: `src/java/org/apache/cassandra/db/commitlog/AbstractCommitLogService.java`
**Category**: Logic error in condition

---

## Root Cause

The lag-tracking and warning log in `SyncRunnable` accumulated `syncCount` and computed lag on every iteration of the loop, regardless of whether that iteration actually performed a full disk flush or only wrote a lightweight segment marker. As a result, the "exceeded configured commit interval" warning fired even when the slow path was never taken, and the statistics mixed marker-only iterations with genuine disk-flush iterations — producing misleading or completely suppressed warnings. The fix restricts lag accounting and logging to iterations where `flushToDisk` is true, and extracts the logic into `maybeLogFlushLag` so it can be independently tested.

### Before / After

```java
// BEFORE — syncCount and lag computed unconditionally on every loop iteration
syncCount++;
totalSyncDuration += now - pollStarted;
if (firstLagAt > 0)
{
    NoSpamLogger.log(...);  // fires on marker syncs too
}
```

```java
// AFTER — only count and log when a real disk flush was performed
if (flushToDisk)
    maybeLogFlushLag(pollStarted, now);
// maybeLogFlushLag increments syncCount and totalSyncDuration only on flush paths
```

---

## How to Spot

When a metric counter or a lag warning sits outside a conditional block that controls the expensive operation being measured, the counter is likely being incremented for cases it should not cover. Review every `syncCount++` or `totalDuration +=` to confirm it is inside the same guard as the operation it is supposed to measure.

---

## Test

Three new unit tests directly call `maybeLogFlushLag` with controlled timestamps and assert the return value (did/did-not log) and the `totalSyncDuration` accumulator across single and repeated invocations.
