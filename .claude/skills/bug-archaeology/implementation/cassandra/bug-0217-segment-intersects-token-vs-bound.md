# Bug 0217: Segment#intersects Compares Token Instead of KeyBound, Dropping Rows

**Commit**: 661a1d5bb6 · **Ticket**: n/a
**Subsystem**: index/sai/disk/v1/segment (Storage-Attached Index, Segment)
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`Segment#intersects(AbstractBounds<PartitionPosition>)` was comparing the query range bounds against raw `Token` values stored in `minKey` / `maxKey` fields rather than against the `Token.KeyBound` objects (`minKeyBound` / `maxKeyBound`). Because a token-level comparison ignores whether a bound is inclusive or exclusive, exclusive range endpoints at segment boundaries could incorrectly pass (or fail) the intersection check, causing rows at the boundary token to be silently skipped or incorrectly included. The fix removes the raw `Token` fields entirely and performs all comparisons using `keyRange.right.compareTo(minKeyBound)` and `keyRange.left.compareTo(maxKeyBound)`.

### Before / After

```java
// BEFORE – comparing against raw token
int cmp = keyRange.right.getToken().compareTo(minKey);
if (!keyRange.right.isMinimum() && (!keyRange.inclusiveRight() && cmp == 0 || cmp < 0))
    return false;
return keyRange.isStartInclusive() || keyRange.left.getToken().compareTo(maxKey) < 0;
```

```java
// AFTER – comparing against KeyBound (respects inclusive/exclusive semantics)
int cmp = keyRange.right.compareTo(minKeyBound);
if (!keyRange.right.isMinimum() && (!keyRange.inclusiveRight() && cmp == 0 || cmp < 0))
    return false;
cmp = keyRange.left.compareTo(maxKeyBound);
return (keyRange.isStartInclusive() || cmp != 0) && cmp <= 0;
```

---

## How to Spot

Any range-intersection predicate that extracts `.getToken()` from a `PartitionPosition` bound and compares it directly discards the inclusive/exclusive flag embedded in the bound. During review, look for `compareTo(minKey)` or `compareTo(maxKey)` where the left-hand side is a `PartitionPosition` or `KeyBound` — those comparisons should use the bound directly, not `.getToken()`.

---

## Test

`TokenRangeReadTest.testTokenRangeRead` verifies that a token-range query `token(k1) >= token(1) AND token(k1) <= token(1)` returns the correct row both before and after a flush, exercising the boundary-inclusive path that was broken.
