# Bug 0942: Non-primary-key filtering silently rejected for COMPACT tables

**Commit**: 9c3855b5fd · **Ticket**: CASSANDRA-6377
**Subsystem**: CQL / Filtering
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`SingleColumnRelation.toRestriction()` contained an unconditional `checkFalse` that rejected any predicate on a non-primary-key column in a dense (COMPACT) table with the error "not yet supported". This blocked legitimate `ALLOW FILTERING` queries on COMPACT tables that have no secondary index. Separately, `RowFilter.applyToPartition()` did not check static-column filter expressions before iterating rows, so a filter on a static column that should exclude an entire partition would still emit the partition (with no rows). The fix removes the blanket rejection for COMPACT tables, adds early static-column evaluation at the partition level, and refactors `validateForIndexing` into a general `validate` method called at expression-add time.

### Before / After
```java
// BEFORE — blanket rejection for non-PK columns on dense tables
checkFalse(!columnDef.isPrimaryKeyColumn() && isDense,
           "Predicates on the non-primary-key column (%s) of a COMPACT table are not yet supported",
           columnDef.name);

// AFTER — check removed; StatementRestrictions handles filtering policy instead
```

```java
// BEFORE — partition returned without checking static-column filter
public UnfilteredRowIterator applyToPartition(UnfilteredRowIterator partition)
{
    pk = partition.partitionKey();
    return Transformation.apply(partition, this);
}

// AFTER — static filter evaluated first; partition excluded if static row fails
public UnfilteredRowIterator applyToPartition(UnfilteredRowIterator partition)
{
    if (filterStaticColumns && applyToRow(partition.staticRow()) == null)
        return null;
    pk = partition.partitionKey();
    UnfilteredRowIterator iterator = Transformation.apply(partition, this);
    return (filterNonStaticColumns && !iterator.hasNext()) ? null : iterator;
}
```

---

## How to Spot
Any guard that throws "not yet supported" based solely on table type (dense/compact) should be scrutinised: if `ALLOW FILTERING` is a valid fallback, the rejection must be conditional on `!useFiltering` rather than unconditional. Similarly, `RowFilter` transformations applied to partition iterators must evaluate static-row predicates before entering the per-row loop, otherwise partitions that should be excluded pass through wholesale.

---

## Test
`SelectTest`, `SelectSingleColumnRelationTest`, `SelectMultiColumnRelationTest`, and `CassandraIndexTest` all received extensive new test cases covering the newly supported filtering paths.
