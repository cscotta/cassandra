# Bug 2232: Re-Created Keyspace Disappears After 1.1.5 Upgrade Due to Timestamp Clamping in Wrong Layer

**Commit**: 988ea81d40 · **Ticket**: CASSANDRA-4698
**Subsystem**: src/java/org/apache/cassandra/db/ColumnSerializer.java, src/java/org/apache/cassandra/db/RowMutation.java, src/java/org/apache/cassandra/service/MigrationManager.java
**Category**: State not cleaned up

---

## Root Cause

A previous workaround for CASSANDRA-4561 clamped future-timestamped schema columns to "now" inside `ColumnSerializer.deserialize()`, a general-purpose method called for every column read. When a keyspace was dropped and then re-created, the schema mutation for the new keyspace was serialized with a `System.nanoTime()`-derived timestamp that could appear in the past on the receiving node after the clamping had already been applied during the upgrade. The column representing the re-created keyspace therefore landed with a timestamp older than the deletion tombstone, making the keyspace appear deleted again.

The fix removes the clamping from the generic `ColumnSerializer` (where it silently corrupted non-schema data) and introduces a dedicated `deserializeFixingTimestamps()` method on `RowMutation.Serializer` that applies timestamp correction only when deserializing schema migrations inside `MigrationManager`.

### Before / After

```java
// BEFORE — ColumnSerializer.deserialize() clamped ALL columns
long ts = dis.readLong();
long now = FBUtilities.timestampMicros();
if (ts > now)
    ts = now;
```

```java
// AFTER — MigrationManager uses a targeted helper
schema.add(RowMutation.serializer().deserializeFixingTimestamps(in, version));
// ColumnSerializer.deserialize() no longer touches the timestamp at all
```

---

## How to Spot

When a timestamp-clamping workaround is placed in a shared serialization path rather than in the specific consumer that needs it, it will silently corrupt unrelated data. Look for "fix future timestamp" heuristics inside generic `deserialize` methods — they should live at the call site that actually needs the correction.

---

## Test

No test added.
