# Bug 0223: CLUSTERING ORDER with Non-Clustering Column Uses Wrong Validation and Gives Misleading Error

**Commit**: 1eccb2bc1f · **Ticket**: CASSANDRA-17818
**Subsystem**: cql3/statements (CreateTableStatement)
**Category**: Logic error in condition

---

## Root Cause

`CreateTableStatement` validated `CLUSTERING ORDER BY` columns with the check `properties.definedOrdering.size() > columnAliases.size()`, which only detected the case where *more* columns were listed in `CLUSTERING ORDER BY` than there are clustering columns in total. It could not detect the case where the count is within bounds but one or more listed columns are not clustering columns at all (e.g., a regular column or partition key column). Furthermore, even when the size check fired it produced a generic error message with no indication of which column(s) were offending. The fix replaces the size check with a set-difference: collect all columns in `definedOrdering` that are not in `columnAliases`, and if that set is non-empty, throw with the list of offending column names included in the message.

### Before / After

```java
// BEFORE – only catches size overflow, misses wrong-name cases
if (properties.definedOrdering.size() > columnAliases.size())
    throw new InvalidRequestException("Only clustering key columns can be defined in CLUSTERING ORDER directive");
```

```java
// AFTER – catches any non-clustering column by name and reports them
List<ColumnIdentifier> nonClusterColumn = properties.definedOrdering.keySet().stream()
                                                                    .filter((id) -> !columnAliases.contains(id))
                                                                    .collect(Collectors.toList());
if (!nonClusterColumn.isEmpty())
    throw new InvalidRequestException(
        "Only clustering key columns can be defined in CLUSTERING ORDER directive: "
        + nonClusterColumn + " are not clustering columns");
```

---

## How to Spot

A size-only guard on a set or map is a weak substitute for a membership check: it catches too-many elements but not wrong elements. When validating that a user-supplied list of column names is a subset of an expected set, always perform an explicit set-difference (or `containsAll`) rather than comparing cardinalities; this both catches the right condition and enables a useful error message identifying the offending names.

---

## Test

`CreateTableValidationTest.testCreateTableErrorOnNonClusteringKey` asserts that attempting to put a regular column, a partition key column, or a mix of valid and invalid columns in `CLUSTERING ORDER BY` raises `InvalidRequestException` with a message that names the offending columns.
