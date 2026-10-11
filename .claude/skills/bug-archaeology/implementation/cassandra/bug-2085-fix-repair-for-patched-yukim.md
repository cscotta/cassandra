# Bug 2085: Repair -pr Only Repaired Single Primary Range on Vnode Nodes

**Commit**: b5f172624a · **Ticket**: CASSANDRA-5329
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`forceRepairAsync` with `primaryRange=true` called `getLocalPrimaryRange()`, which returns only a single token range. In traditional (non-vnode) deployments each node owns exactly one primary range, so this was correct. With vnodes enabled, a node owns multiple discontiguous primary ranges. Using `getLocalPrimaryRange()` caused repair to silently skip all but one of the node's primary ranges, leaving data unrepaired.

The fix replaces the call with `getLocalPrimaryRanges()` (plural), which returns the full collection of primary ranges regardless of vnode configuration.

### Before / After

```java
// BEFORE
final Collection<Range<Token>> ranges = primaryRange
    ? Collections.singletonList(getLocalPrimaryRange())
    : getLocalRanges(keyspace);
```

```java
// AFTER
final Collection<Range<Token>> ranges = primaryRange
    ? getLocalPrimaryRanges()
    : getLocalRanges(keyspace);
```

---

## How to Spot

APIs that return a single item (`getX()`) vs. a collection (`getXs()`) are a common source of vnode-related bugs. Whenever repair, streaming, or range-scanning logic is written with vnodes in mind, verify that every call site uses the plural collection variant for primary range retrieval.

---

## Test

No test added.
