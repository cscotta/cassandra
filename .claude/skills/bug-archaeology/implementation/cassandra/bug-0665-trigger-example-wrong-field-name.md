# Bug 0665: Wrong Field Name in Trigger Example for 4.0 API

**Commit**: 2e5847d29b · **Ticket**: CASSANDRA-13796
**Subsystem**: examples/triggers/src/org/apache/cassandra/triggers/AuditTrigger.java
**Category**: Wrong constant / default value

---

## Root Cause

The `AuditTrigger` example was written against an older `CFMetaData` API where the table name field was called `table`. In the 4.0 `TableMetadata` refactor the field was renamed to `name`, but the example was not updated. Any attempt to compile or run the trigger against a 4.0 node would fail with a compilation error or `NoSuchFieldError` because `update.metadata().table` no longer exists.

### Before / After

```java
// BEFORE — wrong field name from old API
.add("table_name", update.metadata().table)
```

```java
// AFTER — correct field name in 4.0 TableMetadata
.add("table_name", update.metadata().name)
```

---

## How to Spot

When renaming fields in a core API class (`CFMetaData` → `TableMetadata`), grep for every usage site including example code and tool directories, not just production source trees. Example code that is not compiled as part of the main build is especially easy to miss during large refactors.

---

## Test

No test added.
