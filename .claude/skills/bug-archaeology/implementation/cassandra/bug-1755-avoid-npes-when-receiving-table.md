# Bug 1755: Avoid NPEs When Receiving Table Changes for an Unknown Keyspace

**Commit**: 5e40a3b7c1 · **Ticket**: CASSANDRA-5631
**Subsystem**: src/java/org/apache/cassandra/service/MigrationManager.java
**Category**: Missing null / bounds check

---

## Root Cause

When a node missed the `CREATE KEYSPACE` migration message (e.g., due to being down at that moment), subsequent `CREATE TABLE`, `ALTER TABLE`, and `DROP TABLE` announcements would arrive with only the column-family schema mutation. When the receiving node tried to look up the parent keyspace to apply the table change it found nothing and threw a `NullPointerException`. The fix ensures that every table-level schema announcement also includes the serialized keyspace row by calling `addSerializedKeyspace()`, so the recipient can reconstruct the keyspace if it is missing before applying the table change.

### Before / After

```java
// BEFORE (announceColumnFamilyUpdate)
announce(oldCfm.toSchemaUpdate(cfm, FBUtilities.timestampMicros()));
```

```java
// AFTER
announce(addSerializedKeyspace(oldCfm.toSchemaUpdate(cfm, FBUtilities.timestampMicros()), cfm.ksName));

// new helper:
private static RowMutation addSerializedKeyspace(RowMutation migration, String ksName) {
    migration.add(SystemTable.readSchemaRow(ksName).cf);
    return migration;
}
```

---

## How to Spot

Whenever a schema change message references a parent object (keyspace), the handler at the receiver must tolerate receiving the message in isolation. If the code does a direct lookup of the parent without a null check, a missed earlier message will cause a NPE. Always bundle parent-object state with child-object mutations, or add defensive null handling at the receiver.

---

## Test

No test added.
