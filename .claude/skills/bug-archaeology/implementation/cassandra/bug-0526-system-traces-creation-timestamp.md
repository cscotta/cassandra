# Bug 0526: system_traces Creation Timestamp Uses Current Time, Breaks Schema Evolution

**Commit**: 7e878c1eb6 · **Ticket**: CASSANDRA-15398
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java, MigrationManager.java, tracing/TraceKeyspace.java
**Category**: Wrong constant / default value

---

## Root Cause

Before CASSANDRA-6016 (Cassandra 2.0.2), `system_traces` keyspace and tables were created with a timestamp of `currentTime + 1`, meaning each node bounce wrote schema mutations with a fresh wall-clock timestamp. When these nodes were upgraded to 3.0, the old `maybeAddOrUpdateKeyspace()` method used `forceAnnounceNewColumnFamily()`, which issued schema mutations with timestamp `0`. Timestamp `0` is smaller than any historical wall-clock timestamp, so the forced schema update was silently discarded as stale by the gossip/schema merge logic on nodes that had the old high-timestamp entries. Schema changes to `system_traces` therefore failed to propagate on clusters upgraded from pre-2.0.2.

The fix introduces per-keyspace `GENERATION` constants — `TraceKeyspace.GENERATION = 1577836800000000L` (Jan 1 2020), chosen to be definitively larger than any 2.0-era timestamp — and a new `evolveSystemKeyspace()` method in `MigrationManager` that only emits schema mutations when the current definition differs from the expected one, using the generation as the mutation timestamp.

### Before / After

```java
// BEFORE — timestamp 0 is silently ignored on nodes with old high-timestamp schema entries
MigrationManager.forceAnnounceNewColumnFamily(expectedTable);
// with timestamp 0 inside announceNewColumnFamily(cfm, false, false, 0)
```

```java
// AFTER — generation chosen to exceed any legacy timestamp
public static final long GENERATION = 1577836800000000L; // Jan 1 2020 in micros
// evolveSystemKeyspace compares definitions and uses GENERATION as mutation timestamp
evolveSystemKeyspace(TraceKeyspace.metadata(), TraceKeyspace.GENERATION)
    .ifPresent(MigrationManager::announceGlobally);
```

---

## How to Spot

Any schema mutation applied with a fixed timestamp risks being silently discarded if nodes in the cluster hold schema entries with a higher timestamp from a previous version. When system keyspace tables are evolved on upgrade, the mutation timestamp must be chosen to exceed all possible historical timestamps from prior releases. Fixed zero-timestamps are always suspect for this class of bug.

---

## Test

`MigrationManagerTest` tests three scenarios: creating a new keyspace (mutation present), re-checking an up-to-date keyspace (no mutation), and detecting a changed table definition (mutation present with updated generation).
