# Bug 2737: Secondary index scan uses column comparator instead of value validator for filter expressions

**Commit**: a0498517f2 · **Ticket**: CASSANDRA-2347

## Detection Rule
When evaluating a filter predicate on a column value, use the column's value validator (type-aware comparator) rather than the column-name comparator of the owning column family.

## Code Smell
`satisfies()` calls `data.getComparator().compare(column.value(), expression.value)` where `getComparator()` returns the column-name comparator (e.g., `UTF8Type` for names), not the value type, so numeric or typed value comparisons silently produce wrong orderings.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: wrong-comparator-for-value
- severity: silent-wrong-result
- subsystem: sai-secondary-index

## Root Cause
Non-indexed secondary filter expressions are evaluated after the primary index fetch. The comparison used `ColumnFamily.getComparator()`, which is the comparator for column *names* (ordering columns within a row). Column *values* must be compared using the column's `validator` from the schema, which knows the actual type (e.g., `LongType`, `IntegerType`). Using the name comparator for value bytes produces incorrect relational comparisons for any non-lexicographic type.

## Example
```java
// BEFORE — name comparator applied to value bytes
int v = data.getComparator().compare(column.value(), expression.value);
if (!satisfies(v, expression.op))
    return false;
```
```java
// AFTER — value validator used for type-correct comparison
int v = data.metadata().getValueValidator(expression.column_name)
            .compare(column.value(), expression.value);
if (!satisfies(v, expression.op))
    return false;
```
