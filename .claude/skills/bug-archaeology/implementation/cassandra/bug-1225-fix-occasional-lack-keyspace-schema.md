# Bug 1225: Occasional Lack of `system` Keyspace in Schema Tables

**Commit**: f1b22dfc40 · **Ticket**: CASSANDRA-8487
**Subsystem**: src/java/org/apache/cassandra/db/SystemKeyspace.java
**Category**: Race condition / missing synchronization

---

## Root Cause

During system keyspace initialisation, the code first deleted old schema rows for the `system` keyspace and then inserted the current definitions at `timestampMicros() + 1`. Because the two `timestampMicros()` calls could land at different wall-clock instants, the tombstone written by the DELETE could carry a timestamp that was _higher_ than the one used for the subsequent INSERT, causing the tombstone to silently shadow the new rows. The result was that the `system` keyspace appeared absent from schema tables on restart. The fix captures a single timestamp before the DELETE and reuses it (plus one) for the INSERT, making the ordering deterministic.

### Before / After

```java
// BEFORE
executeOnceInternal(String.format("DELETE FROM system.%s WHERE keyspace_name = ?", cfname), ksmd.name);
// ...
ksmd.toSchema(FBUtilities.timestampMicros() + 1).apply();
```

```java
// AFTER
long timestamp = FBUtilities.timestampMicros();
executeOnceInternal(
    String.format("DELETE FROM system.%s USING TIMESTAMP ? WHERE keyspace_name = ?", cfname),
    timestamp, ksmd.name);
// ...
ksmd.toSchema(timestamp + 1).apply();
```

---

## How to Spot

Whenever a DELETE followed by an INSERT must have a guaranteed ordering in Cassandra's timestamp-based conflict resolution, both operations need a shared timestamp captured once before either is issued. Reviewers should flag any pattern where two `timestampMicros()` calls bracket a tombstone and its replacement write, since any wall-clock advance between the calls can allow the tombstone to win.

---

## Test

No test added.
