# Bug 0731: CONTAINS filtering crashes on null collections

**Commit**: 0eebc6e6b7 · **Ticket**: CASSANDRA-13246
**Subsystem**: CQL / Row Filtering
**Category**: Missing null / bounds check

---

## Root Cause
`RowFilter.Expression` evaluated `CONTAINS` predicates against complex columns (sets, maps, lists) by calling `row.getComplexColumnData(column)` and immediately iterating over the result without checking for `null`. The Javadoc for `getComplexColumnData` explicitly states it may return `null` when the row has no data for that column (e.g., the collection was never set, or was set to `null`). A query with a `CONTAINS` restriction against such a row caused an unconditional `NullPointerException` at iteration time.

### Before / After
```java
// BEFORE
ComplexColumnData complexData = row.getComplexColumnData(column);
for (Cell cell : complexData)   // NPE when complexData == null
{
    ...
}
return false;

// AFTER
ComplexColumnData complexData = row.getComplexColumnData(column);
if (complexData != null)
{
    for (Cell cell : complexData)
    {
        ...
    }
}
return false;
```

---

## How to Spot
Whenever `getComplexColumnData()` is called in filter evaluation code, check that the caller guards against a `null` return before iterating or accessing its contents. The method contract permits `null` for rows where the column was never written, and filtering code must treat this as "no match" rather than an error condition.

---

## Test
`SecondaryIndexTest.testSelectOnMultiIndexOnCollectionsWithNull` and a companion test in `SelectTest` were added. Both insert a row with explicit `null` collection columns and assert that `CONTAINS` queries over those rows return the correct empty result rather than throwing.
