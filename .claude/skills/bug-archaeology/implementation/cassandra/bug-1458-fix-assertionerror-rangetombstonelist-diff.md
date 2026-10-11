# Bug 1458: AssertionError in RangeTombstoneList diff

**Commit**: f0893f544f · **Ticket**: CASSANDRA-8013
**Subsystem**: src/java/org/apache/cassandra/db/RangeTombstoneList.java
**Category**: Logic error in condition

---

## Root Cause

The `RangeTombstoneList.diff` method is used during read repair to compute which tombstones from a superset are absent from the local replica's list. The original code assumed a strict size invariant (`assert size <= superset.size`) and relied on a simple one-to-one index walk between the two lists. This assumption breaks when the superset contains range tombstones that span multiple local tombstones, or when the superset has tombstones with earlier start keys than any local tombstone. In those cases the index walk fell out of sync, triggering the assertion or silently producing wrong diffs. The fix replaces the paired-index walk with a proper sorted-merge that advances the local pointer whenever the local start is less than the superset start, and adds a fast-path when the local list is exhausted mid-way through the superset.

### Before / After

```java
// BEFORE
assert size <= superset.size;
int j = 0;
for (int i = 0; i < superset.size; i++)
{
    boolean sameStart = j < size && starts[j].equals(superset.starts[i]);
    if (!sameStart || !ends[j].equals(superset.ends[i]) || markedAts[j] != superset.markedAts[i])
    {
        // add to diff
        if (sameStart) j++;
    }
    else { j++; }
}
```

```java
// AFTER
// no size assertion
int j = 0;
for (int i = 0; i < superset.size; i++)
{
    while (j < size && comparator.compare(starts[j], superset.starts[i]) < 0)
        j++;
    if (j >= size)
    {
        // append remainder of superset to diff and return
        break;
    }
    if (!starts[j].equals(superset.starts[i]) || !ends[j].equals(superset.ends[i])
            || markedAts[j] != superset.markedAts[i])
    {
        // add superset[i] to diff
    }
}
```

---

## How to Spot

Any algorithm that walks two sorted sequences in parallel and assumes a strict size or containment invariant should be scrutinised: if the invariant can be violated by compaction or merge outcomes it will cause assertion errors or silent miscounting. A safe pattern is to always advance the secondary pointer with a comparison loop rather than relying on positional alignment, and to add a fast-path for the case where one list is exhausted.

---

## Test

A comprehensive `testDiff()` test was added to `RangeTombstoneListTest.java` covering eleven distinct structural cases: no difference, subset contained by first superset element, multiple subset RTs covered by one superset RT, one superset RT covering the entire subset, timestamp-only differences, and extra superset RTs at the start, middle, and end of the list.
