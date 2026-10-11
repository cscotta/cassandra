# Bug 2238: CQL3 SELECT Allows IN Operator on Non-Primary-Key Indexed Columns, Producing Wrong Results

**Commit**: 9c88ad16a6 · **Ticket**: CASSANDRA-4709
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The query validation path for secondary-indexed CQL3 SELECT statements checked whether a metadata restriction was an equality restriction (`isEquality()`) and, if so, counted the column as satisfying the "has indexed equality" requirement. However, `isEquality()` returns true for both single-value equalities and multi-value `IN` lists. An `IN` restriction on a non-PK column would semantically require OR semantics across the index, which Cassandra does not support; instead of rejecting it, the validator silently accepted it and attempted a single-key index lookup, returning incomplete results.

The fix adds an explicit check: after confirming equality, if `restriction.eqValues.size() > 1` the statement immediately throws `InvalidRequestException("Cannot use IN operator on column not part of the PRIMARY KEY")`.

### Before / After

```java
// BEFORE — IN silently accepted as an equality for indexed non-PK cols
if (entry.getValue().isEquality() && indexedNames.contains(entry.getKey().name.key))
{
    hasEq = true;
    break;
}
```

```java
// AFTER — IN on non-PK cols rejected before the index check
if (!restriction.isEquality())
    continue;
if (restriction.eqValues.size() > 1)
    throw new InvalidRequestException(
        "Cannot use IN operator on column not part of the PRIMARY KEY");
if (indexedNames.contains(entry.getKey().name.key))
{
    hasEq = true;
    break;
}
```

---

## How to Spot

Whenever `isEquality()` (or a similar predicate) is used as a gate for an index lookup, also verify that the equality is single-valued. An IN list is technically a form of equality but semantically requires OR/union behavior that most single-index implementations cannot provide; the distinction must be enforced explicitly.

---

## Test

No test added.
