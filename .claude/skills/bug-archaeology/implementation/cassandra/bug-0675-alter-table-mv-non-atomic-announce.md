# Bug 0675: ALTER TABLE Schema Changes for Table and MVs Not Atomic

**Commit**: f85b024fb3 · **Ticket**: CASSANDRA-12952
**Subsystem**: `src/java/org/apache/cassandra/service/MigrationManager.java`
**Category**: Non-atomic file write

---

## Root Cause

When `ALTER TABLE` modified a table that had Materialized Views, the schema change was announced in two separate steps: first `announceColumnFamilyUpdate()` (base table), then one `announceViewUpdate()` call per MV. Each call produced and broadcast an independent schema mutation with its own timestamp. Between the two announcements, a node could observe a schema state where the base table had been updated but its MVs had not — an inconsistent window that could trigger false schema-version mismatches, failed view maintenance, or assertion errors. The fix merges both the base-table mutation and all MV mutations into a single `Mutation` object before broadcasting.

### Before / After

```java
// BEFORE – two separate announce calls, two separate broadcast timestamps
MigrationManager.announceColumnFamilyUpdate(cfm, isLocalOnly);
for (ViewDefinition viewUpdate : viewUpdates)
    MigrationManager.announceViewUpdate(viewUpdate, isLocalOnly);
```

```java
// AFTER – single mutation, single broadcast
MigrationManager.announceColumnFamilyUpdate(cfm, viewUpdates, isLocalOnly);
// internally: builds one Mutation, adds view diffs to it, then calls announce()
```

---

## How to Spot

Any schema-change path that calls `announce*` multiple times in sequence for logically related objects (table + its views, type + its dependents) is vulnerable to the same partial-visibility window. Reviewers should verify that correlated schema objects are bundled into a single `Mutation` before being broadcast, or that a compensating rollback path exists.

---

## Test

No test added.
