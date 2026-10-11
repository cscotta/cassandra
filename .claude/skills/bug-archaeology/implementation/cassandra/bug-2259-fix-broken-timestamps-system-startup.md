# Bug 2259: Broken system.schema_* Timestamps on Node Startup

**Commit**: 846b14019d · **Ticket**: CASSANDRA-4561
**Subsystem**: src/java/org/apache/cassandra/db/DefsTable.java, src/java/org/apache/cassandra/db/SystemTable.java
**Category**: Wrong constant / default value

---

## Root Cause

Schema rows in the system keyspace (`system.schema_keyspaces`, `system.schema_columnfamilies`, `system.schema_columns`) could be written with nanosecond-scale timestamps instead of the expected microsecond-scale timestamps used everywhere else in Cassandra. On startup, `SystemTable.finishStartup()` did not correct these out-of-range timestamps, so comparisons and TTL calculations that depended on microsecond timestamps would silently produce wrong results. The fix introduces `DefsTable.fixSchemaNanoTimestamps()`, called during `finishStartup()`, which detects columns whose timestamp exceeds the current microsecond clock, truncates the affected column family, and rewrites all live cells using a fresh microsecond timestamp.

### Before / After

```java
// BEFORE (SystemTable.finishStartup)
public static void finishStartup() throws IOException
{
    setupVersion();
    purgeIncompatibleHints();
}
```

```java
// AFTER
public static void finishStartup() throws IOException
{
    DefsTable.fixSchemaNanoTimestamps();  // repair bad timestamps before any use
    setupVersion();
    purgeIncompatibleHints();
}
```

---

## How to Spot

When schema or metadata tables are seeded or migrated, verify that the timestamp granularity matches the rest of the system. A mismatch between nanosecond and microsecond timestamps is invisible at write time but corrupts ordering and expiry logic later. Startup migration hooks should include a validity check for timestamp ranges.

---

## Test

No test added.
