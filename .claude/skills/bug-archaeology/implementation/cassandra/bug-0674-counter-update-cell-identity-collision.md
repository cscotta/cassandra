# Bug 0674: Counter Update Cell Misidentified as Pre-2.1 Local Shard During 2.x Reads

**Commit**: ba71289778 · **Ticket**: CASSANDRA-13691
**Subsystem**: `src/java/org/apache/cassandra/db/context/CounterContext.java`, `src/java/org/apache/cassandra/db/LegacyLayout.java`
**Category**: Wrong serialization / deserialization

---

## Root Cause

Counter "update" cells — temporary in-flight cells created by `UpdateParameters.addCounter()` before `CounterMutation` finalises them — were encoded using `createLocal()`, which produces a regular local shard with a runtime-generated `CounterId`. On a 3.0 node serving reads to a 2.1 coordinator, the 2.1 node may encounter a genuine pre-2.0 counter cell whose first shard happens to be a local shard, and `isLocal()` would return `true` for it, incorrectly classifying it as an in-flight update. This broke serialization: the legacy path for counter-update cells extracts only the increment value, discarding the full context of an old counter cell. The fix introduces a hard-coded sentinel `UPDATE_CLOCK_ID` (CounterId from int 0), making update cells unambiguously distinguishable from any real counter cell regardless of its shard composition.

### Before / After

```java
// BEFORE – createLocal() used for update cells; any local-first context matched
public boolean isLocal(ByteBuffer context) {
    return ContextState.wrap(context).isLocal();
}
// UpdateParameters:
builder.addCell(BufferCell.live(..., CounterContext.instance().createLocal(increment)));
```

```java
// AFTER – sentinel ID distinguishes update cells from real pre-2.0 local shards
static final CounterId UPDATE_CLOCK_ID = CounterId.fromInt(0);
public ByteBuffer createUpdate(long count) { /* writes UPDATE_CLOCK_ID */ }
public boolean isUpdate(ByteBuffer context) {
    return ContextState.wrap(context).getCounterId().equals(UPDATE_CLOCK_ID);
}
// UpdateParameters:
builder.addCell(BufferCell.live(..., CounterContext.instance().createUpdate(increment)));
```

---

## How to Spot

Any code that uses a structural property (e.g., "is the first shard local?") to distinguish a transient in-flight state from a durable on-disk state is fragile if those structures can overlap. The safer pattern is to use a sentinel value that cannot appear in any legitimate durable record. Reviewers should be alert to `isLocal()` / `isGlobal()` checks that double as state-machine discriminators across protocol versions.

---

## Test

Creates a single-shard update context with `createUpdate()` and verifies `isUpdate()` returns true; also creates a multi-shard context whose first shard has a different local id and confirms `isUpdate()` returns false, exercising both branches of the disambiguation logic.
