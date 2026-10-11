# Bug 0058: Token Range Queries With MIN_TOKEN Return Wrong Rows

**Commit**: 5e003af516 · **Ticket**: CASSANDRA-20557
**Subsystem**: cql3/restrictions
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`ClusteringElements` range helpers (`lessThan`, `greaterThan`, `atLeast`, `atMost`) and the `BETWEEN` operator built ranges without knowledge of the partitioner, so they could not distinguish the special sentinel value `MIN_TOKEN`. When a query used `token(id) > MIN_TOKEN` or `token(id) BETWEEN MIN_TOKEN AND x`, the comparison treated `MIN_TOKEN` as an ordinary byte value rather than as the logical lower bound of the ring, producing an incorrect range and returning wrong rows. Additionally, the `BETWEEN` operator sorted its arguments to find the lower bound, which destroyed the user-supplied order and made `BETWEEN MIN_TOKEN AND 0` equivalent to `BETWEEN 0 AND MIN_TOKEN`, silently changing query semantics.

### Before / After

```java
// BEFORE – no partitioner; MIN_TOKEN indistinguishable from regular value
public static RangeSet<ClusteringElements> greaterThan(ClusteringElements endpoint)
{
    return buildRangeSet(endpoint, false, BoundType.OPEN);
}
```

```java
// AFTER – partitioner passed through; MIN_TOKEN detected and range built from bottom sentinel
public static RangeSet<ClusteringElements> greaterThan(ClusteringElements endpoint, IPartitioner partitioner)
{
    return buildRangeSet(endpoint, false, BoundType.OPEN, partitioner);
}
// …inside buildRangeSet: if endpoint is MIN_TOKEN the lower bound is replaced by oppositeEndpoint (bottom sentinel)
boolean minToken = false;
if (endpoint.token && !upperBound)
    minToken = partitioner.getTokenFactory().fromByteArray(endpoint.get(0)).isMinimum();
```

---

## How to Spot

Whenever range-building helpers receive a token value as a plain `ByteBuffer` without a reference to the partitioner, they cannot detect ring-boundary sentinels. Review any code that converts token restrictions into `RangeSet<ClusteringElements>` and verify that the partitioner is threaded through so that `isMinimum()` / `isMaximum()` can be tested before applying comparisons.

---

## Test

`SimpleQueryTest` and `SingleNodeTokenConflictTest` cover `token()` queries including `BETWEEN MIN_TOKEN AND <x>` and `token(id) > MIN_TOKEN` patterns and assert the correct row sets are returned.
