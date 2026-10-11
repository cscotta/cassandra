# Bug 1023: Fix the sstable-needs-cleanup check

**Commit**: 3d99418cfd · **Ticket**: CASSANDRA-10740
**Subsystem**: Compaction / Cleanup
**Category**: Logic error in condition

---

## Root Cause
`CompactionManager.needsCleanup()` iterated the sorted owned token ranges and, after finding the first key beyond the current range, checked whether the *next* owned range contained that key (`nextRange.contains(firstBeyondRange.getToken())`). The `Range.contains()` method uses an exclusive left endpoint, so a key that lands exactly on `nextRange.left` would not be considered "contained" and the method would incorrectly return `true` (cleanup needed) even though the key falls precisely on the boundary of the next owned range. The correct test is whether the key's token is less than or equal to the next range's left bound — if so, it lies in the gap between ranges and genuine cleanup is required.

### Before / After
```java
// BEFORE — Range.contains() misses keys landing exactly on the left boundary
Range<Token> nextRange = sortedRanges.get(i + 1);
if (!nextRange.contains(firstBeyondRange.getToken()))
{
    // we found a key in between the owned ranges
    return true;
}

// AFTER — explicit token comparison includes the left-boundary case
Range<Token> nextRange = sortedRanges.get(i + 1);
if (firstBeyondRange.getToken().compareTo(nextRange.left) <= 0)
{
    // we found a key in between the owned ranges
    return true;
}
```

---

## How to Spot
Token range boundary semantics in Cassandra use an exclusive left endpoint and inclusive right endpoint. Using high-level `Range.contains()` near boundaries silently excludes the left endpoint. In review, whenever a token is compared against a range left-bound to decide "is this in the gap?", prefer a direct `token.compareTo(range.left) <= 0` rather than `!range.contains(token)`. Also note that `needsCleanup` was package-private before this fix, making it untestable from the test directory — the visibility was widened to `public` to allow a direct unit test.

---

## Test
`CleanupTest` was extended with a targeted unit test for `needsCleanup()` that verifies keys on range boundaries are handled correctly.
