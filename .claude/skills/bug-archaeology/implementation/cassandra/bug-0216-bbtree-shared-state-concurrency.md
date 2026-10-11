# Bug 0216: Shared TraversalState Causes Concurrency Corruption in BBTree Reader

**Commit**: 2531cb0458 · **Ticket**: CASSANDRA-18669
**Subsystem**: index/sai/disk/v1/bbtree (Storage-Attached Index, Block Balanced Tree)
**Category**: Race condition / missing synchronization

---

## Root Cause

`BlockBalancedTreeWalker` held a single shared `TraversalState` instance (`state`) as a field. Each call to `intersect()` called `state.reset()` and then reused that same mutable state for the traversal, meaning concurrent queries against the same index segment would overwrite each other's traversal cursor. The fix lifted all immutable metadata (bytesPerValue, numLeaves, minPackedValue, maxPackedValue, etc.) out of `TraversalState` into final fields on the walker, and made `TraversalState` a fresh object allocated per query (via `newTraversalState()`), annotated `@NotThreadSafe` for clarity.

### Before / After

```java
// BEFORE – single shared mutable state, reset per query
state.reset();
Intersection intersection = new Intersection(treeInput, ...);
// Intersection used state.bytesPerValue, state.numLeaves, etc.
```

```java
// AFTER – per-query snapshot allocated inside Intersection constructor
this.state = newTraversalState();
// Immutable data (bytesPerValue, maxValuesInLeafNode, ...) accessed as final fields on the walker
```

---

## How to Spot

When a class manages a single mutable traversal cursor as an instance field and exposes a public read method (like `intersect()`) that resets and replaces that cursor, any concurrent callers will corrupt each other's state. Review classes that (a) hold mutable position/cursor objects at the instance level, (b) expose multiple public entry points that mutate those objects, and (c) are reused across threads — if the cursor is not per-call, it is a latent race.

---

## Test

`BlockBalancedTreeReaderTest` was extended with a concurrent intersect scenario that spawns multiple threads querying the same reader simultaneously and asserts that each returns the correct result set without corruption.
