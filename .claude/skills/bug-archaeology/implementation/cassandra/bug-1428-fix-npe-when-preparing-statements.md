# Bug 1428: Fix NPE When Preparing Statements

**Commit**: 7e53db0b92 · **Ticket**: CASSANDRA-8090
**Subsystem**: src/java/org/apache/cassandra/cql3/selection/
**Category**: Missing null / bounds check

---

## Root Cause

The CQL3 `Selection` layer was refactored to move selector classes from `cql3/statements/` into a new `cql3/selection/` package and introduce a proper `Selectable` abstraction. In the old code, `ColumnIdentifier` implemented the `Selectable` interface from the statements package but the lookup path did not validate that `cfm.getColumnDefinition(this)` returned a non-null result before dereferencing it, causing a `NullPointerException` when a SELECT referred to a column name that did not exist in the table. The new `newSelectorFactory` method on `ColumnIdentifier` explicitly checks for null and throws a descriptive `InvalidRequestException` instead.

### Before / After

```java
// BEFORE – no null guard; NPE if column not found
ColumnDefinition def = cfm.getColumnDefinition(id);
// def used unconditionally below
```

```java
// AFTER – explicit null check with user-facing error
public Selector.Factory newSelectorFactory(CFMetaData cfm, List<ColumnDefinition> defs)
        throws InvalidRequestException {
    ColumnDefinition def = cfm.getColumnDefinition(this);
    if (def == null)
        throw new InvalidRequestException(
            String.format("Undefined name %s in selection clause", this));
    return SimpleSelector.newFactory(def.name.toString(), addAndGetIndex(def, defs), def.type);
}
```

---

## How to Spot

Any lookup that resolves a user-supplied name against a schema map (column definitions, keyspace names, table names) must guard against a null return before further use. The pattern `cfm.getColumnDefinition(id)` followed by immediate method calls on the result without a null check is the canonical trigger for this class of NPE. Reviewers should treat every schema-lookup result as potentially null and confirm that either an exception is thrown or a default is applied.

---

## Test

No test added.
