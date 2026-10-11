# Bug 0777: CDC flag missing on Mutation construction and partial copy (backport)

**Commit**: 83ba3b7f37 · **Ticket**: CASSANDRA-13035
**Subsystem**: Commit Log / CDC
**Category**: State not cleaned up

---

## Root Cause
This is the same fix as bug-0776 applied to a different release branch. `Mutation`'s map-accepting constructor never iterated the supplied `PartitionUpdate` values to compute the `cdcEnabled` flag, leaving it `false` for any multi-table mutation assembled outside the single-update fast path. Likewise, `without()` could leave the flag stale after removing column families from the copy because it did not recompute the flag from the remaining updates.

### Before / After
```java
// BEFORE — constructor
this.keyspaceName = keyspaceName;
this.key = key;
this.modifications = modifications;
// cdcEnabled remains false

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
Any code path that creates or transforms a `Mutation` without going through the single-`PartitionUpdate` constructor must explicitly derive `cdcEnabled` by scanning all contained updates. In review, flag every helper that builds or modifies a `Mutation` to verify it recomputes any boolean flag derived from the table metadata of its updates.

---

## Test
No test added.
