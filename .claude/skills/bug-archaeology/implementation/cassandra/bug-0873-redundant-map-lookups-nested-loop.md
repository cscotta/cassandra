# Bug 0873: Redundant Map Lookups Inside Doubly-Nested Loop in CqlInputFormat

**Commit**: 3bf8bdceb8 · **Ticket**: n/a
**Subsystem**: Hadoop / CQL input format
**Category**: Logic error in condition

---

## Root Cause
`CqlInputFormat.getSplits()` iterated over `subSplits.keySet()` in the outer loop, then called `subSplits.get(subSplit)` in the inner loop body to retrieve the value associated with that key. Because the inner loop further iterates over `subSplit.unwrap()` ranges, this map lookup was executed once per inner-loop iteration for every outer key — a quadratic number of hash-map lookups for what is conceptually a single value lookup per outer entry.

### Before / After
```java
// BEFORE
for (TokenRange subSplit : subSplits.keySet())
{
    List<TokenRange> ranges = subSplit.unwrap();
    for (TokenRange subrange : ranges)
    {
        ColumnFamilySplit split = new ColumnFamilySplit(...,
                subSplits.get(subSplit),  // map lookup on every inner iteration
                endpoints);
    }
}

// AFTER
for (Map.Entry<TokenRange, Long> subSplitEntry : subSplits.entrySet())
{
    List<TokenRange> ranges = subSplitEntry.getKey().unwrap();
    for (TokenRange subrange : ranges)
    {
        ColumnFamilySplit split = new ColumnFamilySplit(...,
                subSplitEntry.getValue(),  // direct field access
                endpoints);
    }
}
```

---

## How to Spot
When iterating over `map.keySet()` and calling `map.get(key)` anywhere inside the loop body — especially inside a nested loop — replace the iteration with `map.entrySet()` to retrieve both key and value in a single operation. Static analysis tools such as FindBugs/SpotBugs flag this pattern as `WMI_WRONG_MAP_ITERATOR`.

---

## Test
No test added; purely a performance and correctness cleanup.
