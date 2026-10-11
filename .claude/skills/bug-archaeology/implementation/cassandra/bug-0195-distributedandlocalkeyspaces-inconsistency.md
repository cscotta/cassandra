# Bug 0195: distributedAndLocalKeyspaces Cache Inconsistent with distributedKeyspaces Plus localKeyspaces

**Commit**: ac71d0f56e · **Ticket**: CASSANDRA-18747
**Subsystem**: schema/Schema
**Category**: State not cleaned up

---

## Root Cause

`Schema` maintained a separate `distributedAndLocalKeyspaces` field that was supposed to be the union of distributed and local keyspaces. It was initialised from `localKeyspaces` and then kept in sync by mirroring every `distributedKeyspaces` mutation through `withAddedOrUpdated` / `without`. However the two fields diverged in practice: `distributedAndLocalKeyspaces` was not updated atomically with `distributedKeyspaces`, several callers that needed "distributed + local" were accidentally using only one or the other (e.g. `getNonLocalStrategyKeyspaces()` included `LocalStrategy` keyspaces in some contexts but not others), and the separate field added mutation paths that could be missed. The fix eliminated `distributedAndLocalKeyspaces` entirely and computes the union on demand from the immutable `localKeyspaces` and the up-to-date `distributedKeyspaces`.

### Before / After

```java
// BEFORE – separate mutable field, updated in multiple places
private volatile Keyspaces distributedAndLocalKeyspaces;

// in load():
distributedKeyspaces = distributedKeyspaces.withAddedOrUpdated(ksm);
distributedAndLocalKeyspaces = distributedAndLocalKeyspaces.withAddedOrUpdated(ksm);

public Keyspaces distributedAndLocalKeyspaces() {
    return distributedAndLocalKeyspaces;
}
```

```java
// AFTER – computed on demand, no separate field
public Sets.SetView<String> getKeyspaces() {
    return Sets.union(distributedKeyspaces.names(), localKeyspaces.names());
}
// callers use Streams.concat(distributedKeyspaces.stream(), localKeyspaces.stream()) where needed
```

---

## How to Spot

Maintaining a cached "union" of two independently mutable collections is a classic consistency hazard: the cache must be updated in every code path that modifies either source collection, and transient windows exist where the cache is stale. The correct approach is to either compute the union lazily from its component collections or to make one collection the canonical source and derive the other. When a redundant field is removed and callers are forced to explicitly compose the two sources, the inconsistency can no longer arise.

---

## Test

Existing `SchemaTest`, `BootStrapperTest`, `LeaveAndBootstrapTest`, `SimpleStrategyTest`, and `MoveTest` were updated to use `distributedKeyspaces()` (the new canonical API), which exercises the corrected code paths.
