# Bug 2424: Streaming sends wrong data ranges during token move operations

**Commit**: a277fbedb9 · **Ticket**: CASSANDRA-3639

## Detection Rule

When a method iterates two ordered collections in lockstep (paired iteration) to compute set differences, check whether any range pair can be missed if the collections are not perfectly aligned one-to-one.

## Code Smell

A method iterates `current` and `updated` range collections with a shared `while (current.hasNext() && updated.hasNext())` loop, processing them as correlated pairs; any asymmetry in count or ordering silently skips entries.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: paired-iteration-mismatch
- severity: silent-wrong-result
- subsystem: streaming

## Root Cause

`calculateStreamAndFetchRanges` paired up current and updated ranges one-to-one via a shared iterator loop. When a node moves tokens the two collections are not necessarily the same size or aligned, so many ranges were never compared, causing the node to stream too much (whole old ranges instead of only the difference) or miss fetching new ranges entirely. The fix replaces the paired loop with nested loops that compare every current range against every updated range.

## Example

```java
// BEFORE – paired iteration skips non-aligned ranges
while (current.hasNext() && updated.hasNext()) {
    Range r1 = current.next();
    Range r2 = updated.next();
    if (r1.intersects(r2)) {
        toFetch.addAll(r1.differenceToFetch(r2));
        ...
    } else {
        toStream.add(r1);
        toFetch.add(r2);
    }
}

// AFTER – nested loops compare all pairs
for (Range r1 : current) {
    boolean intersect = false;
    for (Range r2 : updated) {
        if (r1.intersects(r2)) { toStream.addAll(r1.subtract(r2)); intersect = true; break; }
    }
    if (!intersect) toStream.add(r1);
}
for (Range r2 : updated) {
    boolean intersect = false;
    for (Range r1 : current) {
        if (r2.intersects(r1)) { toFetch.addAll(r2.subtract(r1)); intersect = true; break; }
    }
    if (!intersect) toFetch.add(r2);
}
```
