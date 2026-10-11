# Bug 0825: Paging sends exclusive start to 2.x nodes that cannot honour it

**Commit**: c11c7d73d0 · **Ticket**: CASSANDRA-11195
**Subsystem**: CQL query execution / mixed-version paging
**Category**: Wrong serialization / deserialization

---

## Root Cause
When a 3.x coordinator serialises a paging `RangeCommand` for a 2.x replica it must encode the "resume from" position in the legacy on-wire format. The code created an *exclusive* start bound (`ClusteringBound.exclusiveStartOf`) even though old nodes can only handle inclusive start positions; the exclusive start caused the legacy node to skip a row it should have returned. Separately, the limit calculation used a conditional `+1` only for non-compound comparators, when in fact the pre-3.0 protocol always requires `limit + 1` because the start cell itself is re-sent. On the response side, the code path for non-compound comparators was applying a client-side filter to drop the already-returned row instead of simply reading all results through the normal slice filter.

### Before / After
```java
// BEFORE — exclusive start, wrong limit
Slice.Bound newStart = Slice.Bound.exclusiveStartOf(lastReturned);
int maxResults = rangeCommand.limits().count() + (metadata.isCompound() ? 0 : 1);

// AFTER — inclusive start, always +1
Slice.Bound newStart = Slice.Bound.inclusiveStartOf(lastReturned);
int maxResults = rangeCommand.limits().count() + 1;
```

---

## How to Spot
Mixed-version compatibility code that introduces new semantics (exclusive bounds) must probe whether the target node version actually supports that semantic. Any place that conditionally adjusts limits or slice bounds based on `isCompound()` is suspect — the 2.x wire format is always inclusive/always-over-requests, regardless of the comparator type.

---

## Test
No new test added; the fix was validated by the existing upgrade integration tests.
