# Bug 0644: ISE in UPI.Serializer.hasNext() With Per-Partition Limit Queries

**Commit**: 1efdf330e2 · **Ticket**: CASSANDRA-13911
**Subsystem**: db/partitions, service/DataResolver
**Category**: Logic error in condition

---

## Root Cause

`UnfilteredPartitionIterators.Serializer` must fully consume the row iterator for a partition before it can deserialize the next partition from the byte stream. The original code assumed upstream callers would always drain each partition iterator completely before calling `hasNext()` again, throwing an `IllegalStateException` if they had not. However, when a per-partition row limit is set, the merge iterator legally stops consuming rows early once the limit is satisfied, leaving unconsumed rows in the stream. The serializer's guard was therefore both too strict (it threw an error on a valid scenario) and in the wrong place (the drain responsibility belongs to the serializer itself, not to callers). Additionally, `DataResolver`'s short-read protection optimization assumed it could skip fetching more pages whenever `isDoneForPartition()` was true, but with per-partition limits some rows can be under-counted, making that shortcut unsafe.

### Before / After

```java
// BEFORE (UnfilteredPartitionIterators.Serializer.hasNext)
if (next != null && next.hasNext())
    throw new IllegalStateException(
        "Cannot call hasNext() until the previous iterator has been fully consumed");
```

```java
// AFTER
if (null != next)
    while (next.hasNext())
        next.next();   // drain leftover rows before reading next partition
```

```java
// BEFORE (DataResolver short-read protection)
if (!singleResultCounter.isDoneForPartition())
    return null;
```

```java
// AFTER
if (!singleResultCounter.isDoneForPartition()
        && command.limits().perPartitionCount() == DataLimits.NO_LIMIT)
    return null;
```

---

## How to Spot

Whenever a streaming deserializer relies on ordering guarantees (e.g., "the previous item must be fully consumed before the next is started"), it should enforce that invariant itself by draining, not by throwing. Throwing an `IllegalStateException` here is fragile because it breaks valid callers that short-circuit iteration. Review sites where an iterator is advanced inside a stream-position-sensitive deserializer and ask whether every exit path (early termination, limits, exceptions) leaves the stream in the expected position.

---

## Test

No test added (the fix changes runtime behavior inside the serializer path that is exercised by integration-level read tests).
