# Bug 1817: Infinite Loop When Paging Queries with IN

**Commit**: 7c32ffbbfa · **Ticket**: CASSANDRA-6464
**Subsystem**: src/java/org/apache/cassandra/service/pager
**Category**: Logic error in condition

---

## Root Cause

`MultiPartitionPager` tracked how many results remained to fetch using each sub-pager's live `maxRemaining()` counter, which was recomputed by summing all remaining sub-pagers on every call. When the pager resumed from a saved `PagingState`, earlier (exhausted) partitions were still included in the array, so `maxRemaining()` always returned a non-zero value even after every partition had been drained, causing `fetchPage` to loop forever. The fix restructures initialization so already-seen partitions are skipped, a single `remaining` counter is maintained and decremented as rows are consumed, and `isExhausted()` short-circuits on `remaining <= 0`.

### Before / After

```java
// BEFORE – remaining recomputed by summing all pagers; never reached 0
public int maxRemaining()
{
    int max = 0;
    for (int i = current; i < pagers.length; i++)
        max += pagers[i].maxRemaining();
    return max;
}
```

```java
// AFTER – single counter decremented as rows are fetched
private int remaining;  // set once at construction, decremented in fetchPage

public int maxRemaining()
{
    return remaining;
}
```

---

## How to Spot

Whenever a pager or cursor maintains "items remaining" by re-querying subordinate objects each iteration, verify that the subordinate objects are eventually exhausted rather than repeatedly re-evaluated. A tight loop where `!isExhausted()` never becomes true is the runtime signature; the code smell is a `maxRemaining()` that sums mutable sub-pager state without an upper-bound counter.

---

## Test

No dedicated test added in this commit. The fix was covered by the existing paging integration tests exercising `IN` queries once the correct behaviour was restored.
