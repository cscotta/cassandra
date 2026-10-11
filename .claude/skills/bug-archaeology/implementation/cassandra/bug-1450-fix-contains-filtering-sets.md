# Bug 1450: CONTAINS filtering on sets compares wrong cell part

**Commit**: 8d20b48fc3 · **Ticket**: CASSANDRA-8033
**Subsystem**: src/java/org/apache/cassandra/db/filter/ExtendedFilter.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

In Cassandra's internal storage model, a `set<T>` column stores each element as a cell *name* (the column qualifier), not as a cell *value* (the column body). The `collectionSatisfies()` method in `ExtendedFilter` handled `CONTAINS` for all collection types with a single branch that compared `cell.value()` against the expression value using `valueComparator()`. For sets this is wrong: the value bytes are empty; the searched-for element lives in `cell.name().collectionElement()` and must be compared with `nameComparator()`. The bug caused every `CONTAINS` query on a set to return no rows because every cell value comparison failed.

### Before / After

```java
// BEFORE — always compares cell values, silently wrong for sets
if (((CollectionType) def.type).valueComparator().compare(iter.next().value(), expr.value) == 0)
    return true;
```

```java
// AFTER — dispatches on collection kind
Cell cell = iter.next();
if (type.kind == CollectionType.Kind.SET)
{
    if (type.nameComparator().compare(cell.name().collectionElement(), expr.value) == 0)
        return true;
}
else
{
    if (type.valueComparator().compare(cell.value(), expr.value) == 0)
        return true;
}
```

---

## How to Spot

When writing or reviewing filter logic over Cassandra collection columns, always ask which storage tier (name vs. value) each collection kind uses. A single `valueComparator()` path that does not branch on `CollectionType.Kind` is a red flag — sets and lists store their data differently from maps. A reviewer can confirm the storage layout by checking the collection serializer or inspecting a raw dump: set elements will have non-empty names and empty values.

---

## Test

`ContainsRelationTest.testFilterForContains()` added — creates a table with a `set<int>` column and a secondary index, inserts four rows, and asserts that `CONTAINS` with `ALLOW FILTERING` returns only the matching row.
