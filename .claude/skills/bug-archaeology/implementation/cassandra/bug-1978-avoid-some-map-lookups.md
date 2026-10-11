# Bug 1978: Redundant schema map lookups in RowMutation causing avoidable overhead and assertion gap

**Commit**: 86a077a190 · **Ticket**: n/a
**Subsystem**: db/RowMutation.java, config/CFMetaData.java, config/KSMetaData.java, config/ColumnDefinition.java
**Category**: Logic error in condition

---

## Root Cause

`RowMutation.addOrGet(String cfName)` performed a `Schema.instance.getCFMetaData(table, cfName)` lookup every time a column family was accessed by name, even when the caller already had the `CFMetaData` object in hand (e.g. `UpdateStatement`, `DeleteStatement`, `CFMetaData.dropFromSchema`). This caused unnecessary repeated map lookups against the schema registry. Additionally, `RowMutation.apply()` called `getTable()` instead of the already-available `table` field, and lacked an assertion that the keyspace actually exists, making misconfiguration harder to diagnose.

### Before / After

```java
// BEFORE: string lookup even when CFMetaData is already available
public ColumnFamily addOrGet(String cfName) {
    CFMetaData cfm = Schema.instance.getCFMetaData(table, cfName);
    ColumnFamily cf = modifications.get(cfm.cfId);
    ...
}
```

```java
// AFTER: overload that accepts CFMetaData directly; string variant delegates
public ColumnFamily addOrGet(String cfName) {
    return addOrGet(Schema.instance.getCFMetaData(table, cfName));
}

public ColumnFamily addOrGet(CFMetaData cfm) {
    ColumnFamily cf = modifications.get(cfm.cfId);
    ...
}

// Also: assertion added in apply()
KSMetaData ksm = Schema.instance.getTableDefinition(table);
assert ksm != null : "Attempting to mutate non-existant keyspace " + table;
```

---

## How to Spot

When a method accepts a `String` name only to immediately look it up in a registry, add an overload that accepts the already-resolved object. This both avoids redundant lookups and surfaces missing-entry bugs earlier with an assertion.

---

## Test

No test added.
