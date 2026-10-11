# Bug 0776: CDC flag missing on Mutation construction and partial copy

**Commit**: 249e4b195d · **Ticket**: CASSANDRA-13035
**Subsystem**: Commit Log / CDC
**Category**: State not cleaned up

---

## Root Cause
`Mutation` carries a `cdcEnabled` boolean that governs whether the mutation must be written to the CDC commit-log segment. The constructor that accepts a pre-built `modifications` map never scanned those `PartitionUpdate` entries to derive the flag, so `cdcEnabled` remained `false` even when one or more of the contained tables had CDC enabled. A second gap existed in `without()`, which copies the mutation and then removes a subset of column families: it also never recomputed `cdcEnabled` after the removal, so the flag could remain `true` even if the only CDC-enabled table had just been stripped out.

### Before / After
```java
// BEFORE — constructor
this.keyspaceName = keyspaceName;
this.key = key;
this.modifications = modifications;
// cdcEnabled stays false

// AFTER — constructor
this.keyspaceName = keyspaceName;
this.key = key;
this.modifications = modifications;
for (PartitionUpdate pu : modifications.values())
    cdcEnabled |= pu.metadata().params.cdc;

// BEFORE — without()
Mutation copy = copy();
copy.modifications.keySet().removeAll(cfIds);
return copy;

// AFTER — without()
Mutation copy = copy();
copy.modifications.keySet().removeAll(cfIds);
copy.cdcEnabled = false;
for (PartitionUpdate pu : modifications.values())
    copy.cdcEnabled |= pu.metadata().params.cdc;
return copy;
```

---

## How to Spot
Any `Mutation` constructor or factory method that does not go through the single-argument `PartitionUpdate` path needs an explicit scan of `modifications.values()` to set `cdcEnabled`. In review, look for multi-table mutation builders or mutation trimming helpers that return a new or modified `Mutation` without recalculating boolean flags that are derived from the `PartitionUpdate` collection.

---

## Test
No test added.
