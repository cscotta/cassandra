# Bug 1725: Trigger Definitions Omitted When Serializing CFMetaData to Schema

**Commit**: 553401d2fe · **Ticket**: CASSANDRA-6789
**Subsystem**: src/java/org/apache/cassandra/config/CFMetaData.java
**Category**: State not cleaned up

---

## Root Cause

`CFMetaData.toSchema()` iterates over column definitions and writes them to the schema mutation, but it never iterated over `triggers`. As a result, any triggers attached to a column family were held in memory but never persisted to the system schema tables. After a node restart (or schema gossip round-trip), the trigger definitions were silently lost.

### Before / After

```java
// BEFORE
toSchemaNoColumnsNoTriggers(rm, timestamp);
// triggers not written
for (ColumnDefinition cd : column_metadata.values())
    cd.toSchema(rm, cfName, getColumnDefinitionComparator(cd), timestamp);
```

```java
// AFTER
toSchemaNoColumnsNoTriggers(rm, timestamp);

for (TriggerDefinition td : triggers.values())
    td.toSchema(rm, cfName, timestamp);

for (ColumnDefinition cd : column_metadata.values())
    cd.toSchema(rm, cfName, getColumnDefinitionComparator(cd), timestamp);
```

---

## How to Spot

When reviewing serialization methods, check that every logical component of the object (columns, triggers, indexes, etc.) has a corresponding write call. A method named `toSchemaNoColumnsNoTriggers` is a clear signal that triggers must be written separately — look for the corresponding loop.

---

## Test

`TriggersSchemaTest` added — verifies that triggers survive round-trips through `MigrationManager` for new keyspace creation, adding a CF with a trigger, adding a trigger to an existing CF, and removing a trigger.
