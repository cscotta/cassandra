# Bug 0284: Race Condition Opening Keyspace While Being Removed

**Commit**: 570732375e · **Ticket**: CASSANDRA-17658
**Subsystem**: db/Keyspace, schema/Schema
**Category**: Race condition / missing synchronization

---

## Root Cause

`Keyspace.setInitialized()` and `Keyspace.unsetInitialized()` read/wrote the static `initialized` flag without synchronization, while `Schema.dropKeyspace` first checked `Keyspace.isInitialized()` and then called `getKeyspaceInstance()` (a separate, non-atomic step) under no common lock. A concurrent schema change could observe `initialized == true`, retrieve a stale or null instance, and try to open a keyspace that was simultaneously being removed, leading to data-structure corruption or NPE. The fix wrapped `setInitialized`/`unsetInitialized` in a `synchronized (Schema.instance)` block and reworked `dropKeyspace` to call the new `Keyspace.open(name, schema, allowIfRemoving)` overload inside the `blockingUnloadIfPresent` callback to eliminate the TOCTOU gap.

### Before / After

```java
// BEFORE — Keyspace.java
public static void setInitialized()
{
    initialized = true;          // unsynchronized
}

// BEFORE — Schema.dropKeyspace
Keyspace ks = initialized ? getKeyspaceInstance(keyspace.name) : null;
// ...
maybeRemoveKeyspaceInstance(keyspace.name, dropData);
unload(keyspace);
```

```java
// AFTER — Keyspace.java
public static void setInitialized()
{
    synchronized (Schema.instance)
    {
        initialized = true;
    }
}

// AFTER — Schema.dropKeyspace
Keyspace keyspace = initialized ? Keyspace.open(keyspaceMetadata.name, this, false) : null;
// ...
Keyspace unloadedKeyspace = maybeRemoveKeyspaceInstance(keyspaceMetadata.name, ks -> {
    ks.unload(dropData);
    unload(keyspaceMetadata);
});
assert unloadedKeyspace == keyspace;
```

---

## How to Spot

Any code that tests a boolean flag and then acts on the result in a separate, unsynchronized step is a classic TOCTOU race. During review, check that all reads and writes of shared static flags are guarded by the same monitor, and that the "check" and the "act" occur within a single critical section so no interleaving is possible.

---

## Test

`SchemaTest` and a new `distributed/test/SchemaTest` exercise concurrent keyspace drops to verify no NPE or incorrect open occurs.
