# Bug 1974: Schema Propagation and Persistence Broken for Triggers

**Commit**: a9659774b3 · **Ticket**: CASSANDRA-5774
**Subsystem**: src/java/org/apache/cassandra/config/TriggerDefinition.java, src/java/org/apache/cassandra/db/SystemKeyspace.java, src/java/org/apache/cassandra/triggers/TriggerExecutor.java, src/java/org/apache/cassandra/cql3/statements/CreateTriggerStatement.java, src/java/org/apache/cassandra/cql3/statements/DropTriggerStatement.java
**Category**: State not cleaned up

---

## Root Cause

The trigger subsystem had multiple interconnected schema correctness issues. The `SystemKeyspace.serializedSchema()` and `serializeSchema()` methods did not include the `schema_triggers` column family, so trigger definitions were never propagated during schema exchange or gossip. The `readSchemaRow()` method was hardcoded to read only from `SCHEMA_COLUMNFAMILIES_CF`, making it impossible to read trigger rows. Additionally, trigger metadata was stored in a flat `TriggerOptions` map rather than a proper `TriggerDefinition` object with schema serialization support, so CREATE/DROP TRIGGER mutations were not written to or read from the schema tables correctly.

### Before / After

```java
// BEFORE - schema serialization omits trigger table entirely
public static List<Row> serializedSchema()
{
    schema.addAll(serializedSchema(SCHEMA_KEYSPACES_CF));
    schema.addAll(serializedSchema(SCHEMA_COLUMNFAMILIES_CF));
    schema.addAll(serializedSchema(SCHEMA_COLUMNS_CF));
    // triggers never included
    return schema;
}

// BEFORE - readSchemaRow hardcoded to COLUMNFAMILIES table
public static Row readSchemaRow(String ksName, String cfName)
{
    ColumnFamilyStore schemaCFS = SystemKeyspace.schemaCFS(SCHEMA_COLUMNFAMILIES_CF);
    ...
}
```

```java
// AFTER - schema serialization includes trigger table
schema.addAll(serializedSchema(SCHEMA_TRIGGERS_CF));

// AFTER - readSchemaRow parameterized by schema table name
public static Row readSchemaRow(String schemaCfName, String ksName, String cfName)
{
    ColumnFamilyStore schemaCFS = SystemKeyspace.schemaCFS(schemaCfName);
    ...
}
```

---

## How to Spot

When adding a new schema entity type, audit every method that iterates over schema column families (serialization, gossip exchange, schema read) to ensure the new table is included. Missing one serialization path causes the entity to silently disappear from remote nodes or after restarts.

---

## Test

No test added.
