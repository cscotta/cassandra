# Bug 2175: Broken Timestamps in Schema CFs Caused Endless Compaction Loop

**Commit**: dd1633ba0b · **Ticket**: CASSANDRA-4880
**Subsystem**: src/java/org/apache/cassandra/db/DefsTable.java, src/java/org/apache/cassandra/db/RowMutation.java, src/java/org/apache/cassandra/net/MessagingService.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

Schema column family cells were being written with timestamps in the wrong unit. The code stored microsecond timestamps but then compared them against a freshly computed microsecond value using a simple `> timestamp` check. If pre-existing cells had timestamps stored in milliseconds (from an older code path), those values looked far in the past and were never flagged for cleanup. Conversely, a schema CF written with a millisecond timestamp interpreted as microseconds would appear to be in the year 1970, causing `DefsTable.fixSchemaNanoTimestamps()` to continuously rewrite schema cells and trigger endless minor compactions. The fix adds heuristic timestamp-unit detection (distinguishing millisecond vs. microsecond epoch values) and introduces a new messaging version (`VERSION_117`) to gate schema migration from nodes that do not yet have the fix.

### Before / After

```java
// BEFORE — compared raw timestamp against a microsecond value; unit mismatch went undetected
long timestamp = FBUtilities.timestampMicros();
if (column.timestamp() > timestamp)
{
    needsCleanup = true;
    break row_check_loop;
}
```

```java
// AFTER — detects unit via calendar heuristic before deciding to rewrite
Date now = new Date();
Date columnDate = new Date(column.timestamp());
if (columnDate.after(now))
{
    Date micros = new Date(column.timestamp() / 1000); // assume microseconds
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(micros);
    if ((micros.before(now) && calendar.get(Calendar.YEAR) == 1970) || micros.after(now))
    { needsCleanup = true; break row_check_loop; }
}
else { needsCleanup = true; break row_check_loop; }
```

---

## How to Spot

Timestamp comparison bugs frequently arise when the same concept (wall-clock time) is stored in different units by different code versions. Any code that compares a stored timestamp against a freshly generated one should explicitly document and enforce the expected unit, and mixed-unit comparisons should be caught by integration tests that round-trip through serialization.

---

## Test

No test added.
