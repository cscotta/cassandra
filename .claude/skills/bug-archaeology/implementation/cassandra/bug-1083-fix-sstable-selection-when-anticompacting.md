# Bug 1083: Anticompaction Skips SSTable When It Intersects Multiple Repair Ranges

**Commit**: 9dd847135f · **Ticket**: CASSANDRA-10299
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionManager.java
**Category**: Logic error in condition

---

## Root Cause

The anticompaction loop iterated over repair ranges inside the sstable loop and made an irrevocable removal decision (`sstableIterator.remove()`) the first time an SSTable did not intersect a particular range, even though a later range in the list might intersect it. Because `break` was reached immediately on the first non-intersecting range, an SSTable that overlapped only the second or later repair range was incorrectly placed into `nonAnticompacting` and skipped entirely. Additionally, `Range.normalize(ranges)` was recomputed on every iteration of the outer loop, causing unnecessary repeated work.

### Before / After

```java
// BEFORE — break on first non-intersect, decision made too early
for (Range<Token> r : Range.normalize(ranges))
{
    Range<Token> sstableRange = new Range<>(...);
    if (r.contains(sstableRange)) { ...; sstableIterator.remove(); break; }
    else if (!sstableRange.intersects(r))
    {
        nonAnticompacting.add(sstable);
        sstableIterator.remove();
        break;  // wrong: skips remaining ranges
    }
}
```

```java
// AFTER — defer removal until all ranges examined
boolean shouldAnticompact = false;
for (Range<Token> r : normalizedRanges)
{
    if (r.contains(sstableRange)) { ...; sstableIterator.remove(); shouldAnticompact = true; break; }
    else if (sstableRange.intersects(r)) { shouldAnticompact = true; }
}
if (!shouldAnticompact)
{
    nonAnticompacting.add(sstable);
    sstableIterator.remove();
}
```

---

## How to Spot

Whenever a loop makes an early-exit decision (`break` + collection removal) inside a nested loop over a second collection, ask whether the outer decision is valid with respect to only the first matching (or non-matching) inner element, or must consider all inner elements. Here, the correct invariant is "skip the SSTable only if it intersects *no* repair range," which requires exhausting the inner loop before deciding.

---

## Test

`AntiCompactionTest` test range corrected from `("-10", "-1")` to `("-1", "-10")` to produce a valid range that exercises the repaired-range intersection logic.
