# Bug 0728: Range tombstone bugs in SSTable iterators

**Commit**: f53e502c3c · **Ticket**: CASSANDRA-13340
**Subsystem**: SSTable I/O / Storage Engine
**Category**: Logic error in condition

---

## Root Cause
Two interrelated bugs in `SSTableIterator` and `SSTableReversedIterator` caused incorrect handling of range tombstones that span multiple index blocks. In `SSTableIterator.ForwardRead`, the end-of-slice comparison used a strict `> 0` inequality instead of `>= 0`, meaning a bound exactly equal to the slice end was incorrectly included rather than excluded. This led to returning extra or duplicated range tombstone markers when a bound coincides with a slice boundary. In `SSTableReversedIterator`, multi-block range tombstones were emitted in both the block that opened them and again in the adjacent block, because there was no mechanism to skip the duplicate first/last item when the marker was synthetic (reconstructed from an adjacent block's open state). A third issue in `ClusteringPrefix` used `nextKind.compareTo(bound.kind())` (ordinal comparison) instead of the semantically correct `Kind.compare()`, which could produce wrong ordering for boundary kinds.

### Before / After
```java
// BEFORE (SSTableIterator.ForwardRead.computeNext)
if (!deserializer.hasNext() || deserializer.compareNextTo(end) > 0)
    return null;

// AFTER
if (!deserializer.hasNext() || deserializer.compareNextTo(end) >= 0)
    return null;

// BEFORE (ClusteringPrefix.Serializer)
return nextKind.compareTo(bound.kind());

// AFTER
return Kind.compare(nextKind, bound.kind());

// AFTER (SSTableReversedIterator) — new skip flags
if (skipFirstIteratedItem)
    iterator.next();
if (skipLastIteratedItem)
    iterator = new SkipLastIterator(iterator);
```

---

## How to Spot
Whenever a `compareNextTo` call guards slice termination, check whether the inequality should be strict (`> 0`) or non-strict (`>= 0`). A bound exactly equal to the slice end should generally be excluded from results because an `EXCL_END(x)` bound equals `INCL_START(x)` of the next slice and returning it would duplicate the range tombstone marker. For reversed iterators, whenever a synthetic open/close marker is injected at a block boundary, ensure the real duplicate from the adjacent block is suppressed; the pattern `skipFirstIteratedItem` / `skipLastIteratedItem` is the guard needed.

---

## Test
`DeleteTest` in `cql3/validation/operations` gained 70 lines of new tests covering range tombstones that overlap multiple index blocks in both forward and reversed read order, including the case where a range tombstone bound exactly coincides with a slice boundary.
