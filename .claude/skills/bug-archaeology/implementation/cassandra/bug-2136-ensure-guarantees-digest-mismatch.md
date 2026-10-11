# Bug 2136: Digest Mismatch Repair Bypasses Consistency Level Endpoint Filtering

**Commit**: 3d787b78c1 · **Ticket**: CASSANDRA-5113
**Subsystem**: src/java/org/apache/cassandra/service/ReadCallback.java, StorageProxy.java, RowDataResolver.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When a digest mismatch occurred during a read, the repair-phase full-data read used `RepairCallback` which sent requests to _all_ endpoints rather than only those required to satisfy the requested consistency level. This meant the repair read ignored the endpoint filtering that `ReadCallback` normally applies via `filterEndpoints()`. Additionally, `RowRepairResolver` (renamed `RowDataResolver`) had `getData()` and `isDataPresent()` throwing `UnsupportedOperationException`, preventing it from being used as a proper resolver inside the standard `ReadCallback` path. The fix unifies the repair path through `ReadCallback.withNewResolver()`, which preserves the original consistency-level-filtered endpoint set.

### Before / After

```java
// BEFORE: repair used a separate RepairCallback, bypassing CL endpoint filtering
RowRepairResolver resolver = new RowRepairResolver(command.table, command.key, command.filter());
RepairCallback repairHandler = new RepairCallback(resolver, handler.endpoints);
// ... and in ReadCallback constructor:
this.endpoints = resolver instanceof RowRepairResolver ? endpoints : filterEndpoints(endpoints);
```

```java
// AFTER: repair reuses ReadCallback with its original filtered endpoints
RowDataResolver resolver = new RowDataResolver(command.table, command.key, command.filter());
ReadCallback<ReadResponse, Row> repairHandler = handler.withNewResolver(resolver);
// ... and in ReadCallback constructor:
this.endpoints = filterEndpoints(endpoints);
```

---

## How to Spot

When a digest-mismatch repair path diverges from the normal read path in its endpoint selection or callback mechanism, check whether it still enforces the original consistency level's quorum requirements. Any repair handler that is handed raw `handler.endpoints` (all replicas) rather than the filtered quorum set is suspect.

---

## Test

No new test added for the CL-guarantee aspect. Existing `RowResolverTest` updated to reference the renamed `RowDataResolver`.
