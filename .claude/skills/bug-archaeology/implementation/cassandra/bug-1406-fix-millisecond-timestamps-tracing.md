# Bug 1406: Fix millisecond timestamps in Tracing

**Commit**: be79142297 · **Ticket**: CASSANDRA-8297
**Subsystem**: src/java/org/apache/cassandra/tracing/Tracing.java
**Category**: Wrong constant / default value

---

## Root Cause

The `Tracing` subsystem wrote trace events using `ExpiringColumn` with `System.currentTimeMillis()` as the timestamp and a separately-managed `TTL` constant. Cassandra's internal column timestamp is expected to be in **microseconds** (not milliseconds), so all trace events were recorded with timestamps 1000x too small. This caused trace events to appear out of order or at incorrect relative times when inspected, since the microsecond-resolution comparator would see millisecond values as extremely old. The fix replaces `System.currentTimeMillis()` with `FBUtilities.timestampMicros()` and moves TTL management into the table schema (via `default_time_to_live=86400` in `CFMetaData`) rather than embedding it in each individual `ExpiringColumn` write.

### Before / After

```java
// BEFORE
private static final int TTL = 24 * 3600;

private static void addColumn(ColumnFamily cf, ByteBuffer name, ByteBuffer value)
{
    cf.addColumn(new ExpiringColumn(name, value, System.currentTimeMillis(), TTL));
}
```

```java
// AFTER
private static void addColumn(ColumnFamily cf, ByteBuffer name, ByteBuffer value)
{
    cf.addColumn(name, value, FBUtilities.timestampMicros());
    // TTL now expressed as schema default_time_to_live=86400
}
```

---

## How to Spot

Wherever `System.currentTimeMillis()` is used as a Cassandra column timestamp (rather than as a wall-clock value for display or TTL computation), the result is wrong by a factor of 1000. Reviewers should distinguish between two uses of time in Cassandra code: wall-clock time in milliseconds (for external timestamps, `started_at` fields, etc.) and internal column timestamps in microseconds. Any call that passes `System.currentTimeMillis()` into a column mutation, `ExpiringColumn`, or `RowMutation` should be verified against `FBUtilities.timestampMicros()` or `ClientState.getTimestamp()`.

---

## Test

No test added.
