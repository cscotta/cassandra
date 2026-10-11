# Bug 0943: Non-primary-key filtering broken for thrift static column families

**Commit**: 90fd8deed0 · **Ticket**: CASSANDRA-6377
**Subsystem**: CQL / Thrift / Filtering
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
For pre-CQL (thrift) "static" column families — those whose comparator is neither dense nor compound — `StatementRestrictions` never passed the `useFiltering` flag into the constructor, so even valid `ALLOW FILTERING` queries were rejected. The secondary index manager's `validateIndexSearchersForQuery()` also unconditionally threw "No secondary indexes…" for any clause that lacked an index, even when the table type permitted in-memory filtering. The fix wires `parameters.allowFiltering` into the `StatementRestrictions` constructor, adds a compound/dense guard that only throws for table types that genuinely cannot filter, and refactors the error-message constant to `StatementRestrictions` so it is shared between the two code paths.

### Before / After
```java
// BEFORE — useFiltering not passed; always threw for any non-index restriction
new StatementRestrictions(cfm, whereClause, boundNames,
                          selection.containsOnlyStaticColumns(),
                          selection.containsACollection());

// AFTER — allowFiltering propagated into restrictions
new StatementRestrictions(cfm, whereClause, boundNames,
                          selection.containsOnlyStaticColumns(),
                          selection.containsACollection(),
                          parameters.allowFiltering);
```

```java
// BEFORE — SecondaryIndexManager threw for all non-index clauses
if (!haveSupportedIndexLookup)
{
    // build error about missing secondary index
    throw new InvalidRequestException(...);
}

// AFTER — only dense/compound tables (which cannot filter) throw; others pass through
if (!haveSupportedIndexLookup && (comparator.isDense() || comparator.isCompound()))
{
    if (expressionsByIndexType.isEmpty())
        throw new InvalidRequestException("Predicates on non-primary-key columns (...) not yet supported");
    // otherwise build the "no index supports operator" message
}
```

---

## How to Spot
When a method that validates restrictions is called from a statement builder, check that every configuration flag (especially `allowFiltering`) is forwarded. A missing parameter means the validator cannot distinguish "no index and no filtering allowed" from "no index but filtering was requested", causing legitimate queries to be rejected. Also verify that "no suitable index" error paths are gated on table type: some table types can fall back to in-memory filtering and should not throw.

---

## Test
`SelectTest`, `SelectSingleColumnRelationTest`, `SelectMultiColumnRelationTest`, and `PerRowSecondaryIndexTest` received extensive new cases for thrift static CFs with non-PK column predicates.
