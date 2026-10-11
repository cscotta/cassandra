# Bug 0870: Static Thrift Column Names Decoded with Wrong Comparator

**Commit**: 4d7141f758 · **Ticket**: CASSANDRA-12147
**Subsystem**: Schema Migration / CompactTables
**Category**: Logic error in condition

---

## Root Cause
`CompactTables.columnDefinitionComparator` determined how a legacy column's name bytes should be interpreted during schema migration. The original logic used `"compact_value".equals(kind)` as the first branch and then had separate branches for super tables vs. static columns. This was wrong for non-text comparators: a `static` column in a non-super table should use `rawComparator` (the table's declared comparator), but the old code fell through to a final `"static".equals(kind) ? rawComparator : UTF8Type.instance` only after ruling out `compact_value` and then checking `isSuper`. The branch ordering caused static columns in super tables to be decoded with `UTF8Type.instance` instead of their actual comparator, corrupting column names when the comparator was not text. The fix collapses the logic: any kind that is not `"regular"` should use `UTF8Type.instance`, and regular columns use the appropriate raw comparator based on `isSuper`.

### Before / After
```java
// BEFORE
if ("compact_value".equals(kind))
    return UTF8Type.instance;

if (isSuper)
    return "regular".equals(kind) ? rawSubComparator : UTF8Type.instance;

return "static".equals(kind) ? rawComparator : UTF8Type.instance;

// AFTER
if (!"regular".equals(kind))
    return UTF8Type.instance;

return isSuper ? rawSubComparator : rawComparator;
```

---

## How to Spot
Multi-branch kind/type dispatch that handles only specific string values and falls through to a default is error-prone. Review all such switches or if-else chains to ensure every combination of `(kind, isSuper)` is covered correctly. In migration code that must handle legacy schema formats, test with non-UTF-8 comparators (e.g., `LongType`, `TimeUUIDType`) to catch assumptions that column names are always text.

---

## Test
No test added.
