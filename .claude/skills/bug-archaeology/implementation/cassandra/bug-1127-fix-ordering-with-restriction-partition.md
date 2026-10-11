# Bug 1127: ORDER BY Ignored When IN Restriction Is on Non-Last Partition Key Column

**Commit**: ee186b6b05 · **Ticket**: CASSANDRA-9970
**Subsystem**: cql3/restrictions/PrimaryKeyRestrictionSet
**Category**: Logic error in condition

---

## Root Cause

`PrimaryKeyRestrictionSet` tracks whether the combined restriction set is an IN restriction by checking only the newly added restriction (`restriction.isIN()`). When a composite partition key had an IN restriction on one column and the restriction set was then extended with a restriction on a second partition key column, the `in` flag would not be set because the new restriction itself was not an IN — only the accumulated `primaryKeyRestrictions` already was. The missing check `|| primaryKeyRestrictions.isIN()` meant the combined set was wrongly classified as an equality restriction, causing the query execution layer to skip the merge-sort needed to deliver results in the requested ORDER BY order.

### Before / After

```java
// BEFORE
else if (restriction.isIN())
    this.in = true;
```

```java
// AFTER
else if (restriction.isIN() || primaryKeyRestrictions.isIN())
    this.in = true;
```

---

## How to Spot

When a flag summarises a property of an accumulated collection, the condition that sets the flag must be checked against both the existing accumulated state and the newly added element. Any time you see `flag |= newItem.property()` without a corresponding `flag |= existingCollection.property()`, ask whether the existing collection could already have satisfied the property before the new item arrived.

---

## Test

`SelectOrderByTest.testInOrderByWithTwoPartitionKeyColumns` added — covers `ORDER BY col_3 ASC` and `ORDER BY col_3 DESC` with an IN on the second partition key column, and with the column order in the WHERE clause reversed.
