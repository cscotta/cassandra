# Bug 0141: Commit Log Point-in-Time Restore Ignores Millisecond/Microsecond Precision

**Commit**: 9f18aab1dc · **Ticket**: CASSANDRA-19448
**Subsystem**: src/java/org/apache/cassandra/db/commitlog
**Category**: Wrong serialization / deserialization

---

## Root Cause

`CommitLogArchiver` stored the point-in-time restore target as `restorePointInTime` in **milliseconds** (obtained via `SimpleDateFormat.parse().getTime()`), while Cassandra mutation timestamps are stored in **microseconds**. The replay comparison in `CommitLogReplayer.pointInTimeExceeded` converted the mutation's microsecond timestamp to milliseconds via `archiver.precision.toMillis(upd.maxTimestamp())` and compared to `restorePointInTime`. For `MILLISECONDS` precision this lost sub-millisecond accuracy, and for `MICROSECONDS` precision the conversion path was simply wrong. The fix changes the internal representation to microseconds throughout (`restorePointInTimeInMicroseconds`), parses the configure-time string with `DateTimeFormatter` supporting optional `.SSS` and `.SSSSSS` fractional parts, and compares directly in microseconds.

### Before / After

```java
// BEFORE – restorePointInTime stored as milliseconds from SimpleDateFormat
restorePointInTime = format.parse(targetTime).getTime(); // milliseconds

// In pointInTimeExceeded:
if (archiver.precision.toMillis(upd.maxTimestamp()) > restoreTarget) // lossy
    return true;
```

```java
// AFTER – stored and compared in microseconds
restorePointInTimeInMicroseconds = getRestorationPointInTimeInMicroseconds(targetTime);
// Parsing via DateTimeFormatter with optional fractional seconds

public static long getRestorationPointInTimeInMicroseconds(String restorationPointInTime) {
    Instant instant = format.parse(restorationPointInTime, Instant::from);
    return instant.getEpochSecond() * 1_000_000 + instant.getNano() / 1000;
}

// In pointInTimeExceeded:
if (archiver.precision.toMicros(upd.maxTimestamp()) > archiver.restorePointInTimeInMicroseconds)
    return true;
```

---

## How to Spot

Whenever timestamps from different subsystems are compared, verify that both sides use the same epoch unit. Cassandra's internal timestamp is microseconds since epoch, but Java's `Date.getTime()` and `System.currentTimeMillis()` return milliseconds — mixing these without explicit conversion is a systemic error class in this codebase.

---

## Test

`CommitLogArchiverTest.testRestoreInDifferentPrecision` inserts rows at known microsecond timestamps bracketing the restore point, force-archives and truncates, then replays and asserts that exactly the expected rows are recovered under each of the three supported precisions (seconds, milliseconds, microseconds).
