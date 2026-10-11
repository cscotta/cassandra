# Bug 1926: CommitLogReplayer compares raw timestamp units against millisecond restore point, causing incorrect PITR cutoff

**Commit**: 742e5baf63 · **Ticket**: CASSANDRA-5909
**Subsystem**: src/java/org/apache/cassandra/db/commitlog/CommitLogReplayer.java, src/java/org/apache/cassandra/db/commitlog/CommitLogArchiver.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

Point-in-time recovery compared `families.maxTimestamp()` (a raw Cassandra cell timestamp, whose unit depends on the client — typically microseconds) directly against `restoreTarget` (parsed from the config file as milliseconds since epoch). When clients wrote with microsecond precision, every mutation's timestamp was orders-of-magnitude larger than the millisecond restore target, so the replayer would stop replaying far too early — or immediately. The fix introduces a configurable `precision` field in `CommitLogArchiver` and converts the cell timestamp to milliseconds before comparing: `precision.toMillis(families.maxTimestamp()) > restoreTarget`. Additionally, the date parser was not timezone-aware; it now explicitly uses GMT, and a reusable static `SimpleDateFormat` is shared rather than allocating a new one per parse.

### Before / After

```java
// BEFORE (CommitLogReplayer.java)
if (families.maxTimestamp() > restoreTarget)
    return true;
```

```java
// AFTER (CommitLogReplayer.java)
if (CommitLog.instance.archiver.precision.toMillis(families.maxTimestamp()) > restoreTarget)
    return true;
```

```java
// BEFORE (CommitLogArchiver.java) — new SimpleDateFormat per parse, no timezone
restorePointInTime = Strings.isNullOrEmpty(targetTime) ? Long.MAX_VALUE
    : new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").parse(targetTime).getTime();
```

```java
// AFTER (CommitLogArchiver.java) — shared static format, explicit GMT timezone
public static final SimpleDateFormat format = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
static { format.setTimeZone(TimeZone.getTimeZone("GMT")); }
// ...
restorePointInTime = Strings.isNullOrEmpty(targetTime) ? Long.MAX_VALUE
    : format.parse(targetTime).getTime();
```

---

## How to Spot

Whenever comparing timestamps from two different sources, confirm they share the same unit. Cassandra cell timestamps are typically microseconds while Java `System.currentTimeMillis()` and `Date.getTime()` are milliseconds — mixing them silently produces wildly wrong comparisons. Also watch for `SimpleDateFormat` constructed without an explicit timezone; local-timezone assumptions break in any environment where the JVM timezone differs from the data's intended timezone.

---

## Test

A new `testRecoverPIT` test in `RecoveryManagerTest` writes rows with microsecond timestamps bracketing a configured restore point, performs commit log recovery, and asserts that exactly the expected number of rows (those before the cutoff) are recovered.
