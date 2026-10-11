# Bug 0997: PartitionUpdate toString Triggers Premature Build

**Commit**: 270e986264 · **Ticket**: CASSANDRA-10897
**Subsystem**: Storage engine / PartitionUpdate
**Category**: Logic error in condition

---

## Root Cause
`PartitionUpdate` inherits `toString()` from `AbstractBTreePartition`, which iterates over all rows in the partition. Iterating an unbuilt `PartitionUpdate` calls `build()` as a side-effect, which sorts and freezes the internal BTree. Once built, the update can no longer accept further mutations. If `toString()` was invoked—for example during logging—before all mutations had been applied, the `PartitionUpdate` would be silently frozen at that point, causing subsequent `add()` calls to fail or produce wrong results. The fix overrides `toString()` in `PartitionUpdate` to check `isBuilt` and, when not yet built, return a summary string that does not iterate rows.

### Before / After
```java
// BEFORE — inherited toString() iterates rows, triggering build()
// No override in PartitionUpdate; AbstractBTreePartition iterates BTree rows unconditionally.

// AFTER — override avoids row iteration when not yet built
@Override
public String toString() {
    if (isBuilt)
        return super.toString();

    StringBuilder sb = new StringBuilder();
    sb.append(String.format("[%s.%s] key=%s columns=%s",
                            metadata.ksName, metadata.cfName,
                            metadata.getKeyValidator().getString(partitionKey().getKey()),
                            columns()));
    sb.append("\n    deletionInfo=").append(deletionInfo);
    sb.append(" (not built)");
    return sb.toString();
}
```

---

## How to Spot
Whenever a mutable object inherits a `toString()` (or any read method) from a base class that performs iteration or lazy materialization, check whether that materialization has irreversible side effects. In Cassandra's partition model, building the BTree is a one-way transition; any code path that reads the full row set before the update is finalized—including log statements—can corrupt write pipelines. Review all subclasses of `AbstractBTreePartition` that have a distinct mutable phase to ensure `toString()` is safe to call at any lifecycle stage.

---

## Test
No test added.
