# Bug 2309: Leveled compaction overlapping SSTable computation misses gaps between candidate ranges

**Commit**: 88b48df21d · **Ticket**: CASSANDRA-4321

## Detection Rule

When a set of candidate SSTables is being expanded by finding all SSTables that overlap them, verify that the overlap is computed against the full bounding range of the entire candidate set, not individual candidates one at a time.

## Code Smell

A loop that iterates over individual candidate SSTables and calls `overlapping(singleCandidate, others)` independently — this misses SSTables that fall between two non-adjacent candidates but inside their combined span.

## Tags

- scope: cassandra
- category: wrong-filter-result
- code-pattern: per-item-instead-of-aggregate-range
- severity: silent-wrong-result
- subsystem: compaction

## Root Cause

The `overlapping()` helper used a `Range<Token>` for intersection testing. `Range` in Cassandra uses wrapping semantics for the ring, so a range `(a, b)` would not correctly represent an inclusive closed interval between two SSTable boundaries. The fix replaced `Range` with `Bounds` (which is inclusive on both ends) and changed the overlap computation to first compute the combined bounding span (min first key, max last key) across all candidates, then find everything that overlaps that span.  Without this, when compacting multiple L0 SSTables, SSTable gaps between individual candidates could be missed, leaving overlapping L1 SSTables out of the compaction set and violating LCS invariants.

## Example

```java
// BEFORE: computed overlap per individual sstable, missing gaps
private static Set<SSTableReader> overlapping(SSTableReader sstable, Iterable<SSTableReader> candidates)
{
    Set<SSTableReader> overlapped = new HashSet<>();
    overlapped.add(sstable);
    Range<Token> promotedRange = new Range<Token>(sstable.first.token, sstable.last.token);
    for (SSTableReader candidate : candidates)
    {
        Range<Token> candidateRange = new Range<Token>(candidate.first.token, candidate.last.token);
        if (candidateRange.intersects(promotedRange))
            overlapped.add(candidate);
    }
    return overlapped;
}
```

```java
// AFTER: compute bounding span of all candidates first
private static Set<SSTableReader> overlapping(Collection<SSTableReader> candidates, Iterable<SSTableReader> others)
{
    // find min first / max last across all candidates
    Token first = ..., last = ...;
    return overlapping(first, last, others);
}

private static Set<SSTableReader> overlapping(Token start, Token end, Iterable<SSTableReader> sstables)
{
    Bounds<Token> promotedBounds = new Bounds<Token>(start, end);
    Set<SSTableReader> overlapped = new HashSet<>();
    for (SSTableReader candidate : sstables)
    {
        Bounds<Token> candidateBounds = new Bounds<Token>(candidate.first.token, candidate.last.token);
        if (candidateBounds.intersects(promotedBounds))
            overlapped.add(candidate);
    }
    return overlapped;
}
```
