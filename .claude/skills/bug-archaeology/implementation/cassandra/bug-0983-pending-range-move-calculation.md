# Bug 0983: Wrong Pending Range Calculation During Token Moves

**Commit**: 812df9e8bc · **Ticket**: CASSANDRA-10887
**Subsystem**: Token ring / Cluster topology
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
When a node moves to a new token, `PendingRangeCalculatorService` must identify which ranges are in transition so that coordinators know to send writes to both the old and new owner. The original implementation simply added all ranges the moving node would own after the move as pending. This is incorrect: ranges the node already owns before the move do not need a second write, and other nodes that pick up ranges due to the move were not considered at all.

The fix computes the set of endpoints whose natural-endpoint list changes due to the move, then for each such endpoint finds only the sub-ranges it does not already own (using `Range.subtractAll(oldRanges)`), and marks only those as pending. A new `Range.subtractAll()` helper method supports this set subtraction across a collection of ranges.

### Before / After
```java
// BEFORE – all post-move ranges of the moving node become pending (too broad)
allLeftMetadata.updateNormalToken(moving.left, endpoint);
for (Range<Token> range : strategy.getAddressRanges(allLeftMetadata).get(endpoint))
{
    pendingRanges.put(range, endpoint);
}

// AFTER – only ranges newly acquired by any affected endpoint become pending
for (Range<Token> range : moveAffectedRanges)
{
    Set<InetAddress> difference = Sets.difference(newEndpoints, currentEndpoints);
    for (InetAddress address : difference)
    {
        for (Range<Token> newRange : newRanges)
            for (Range<Token> pendingRange : newRange.subtractAll(oldRanges))
                pendingRanges.put(pendingRange, address);
    }
}
```

---

## How to Spot
Pending-range calculations must account for symmetry: when node A moves away from a range, node B gains it, so B needs pending writes for exactly the new sub-range, not its entire new ownership. Whenever topology-change code calls `getAddressRanges()` after mutating the metadata, check whether the resulting set of ranges needs to be intersected or subtracted against the pre-change set to isolate only the delta.

---

## Test
`MoveTest` is added as a comprehensive new test class covering single and multi-node move scenarios, asserting that the calculated pending ranges are exactly the expected token sub-ranges for both the moving node and the nodes that inherit its old ranges.
