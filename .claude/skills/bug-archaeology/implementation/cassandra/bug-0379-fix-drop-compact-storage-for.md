# Bug 0379: DROP COMPACT STORAGE Removes COUNTER Flag for Counter Tables

**Commit**: fb76baa608 · **Ticket**: CASSANDRA-16653
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/schema
**Category**: State not cleaned up

---

## Root Cause

`AlterTableStatement.DropCompactStorage.apply()` replaced the table's full set of metadata flags with a hard-coded singleton `ImmutableSet.of(TableMetadata.Flag.COMPOUND)`, unconditionally discarding any other flags including `Flag.COUNTER`. After executing `ALTER TABLE … DROP COMPACT STORAGE` on a compact counter table, the table lost its counter identity, causing subsequent counter operations to fail or behave incorrectly.

### Before / After

```java
// BEFORE – hard-coded, drops COUNTER flag for counter tables
return keyspace.withSwapped(keyspace.tables.withSwapped(
    table.withSwapped(ImmutableSet.of(TableMetadata.Flag.COMPOUND))));
```

```java
// AFTER – preserve COUNTER flag when table is a counter table
Set<Flag> flags = table.isCounter()
                ? ImmutableSet.of(Flag.COMPOUND, Flag.COUNTER)
                : ImmutableSet.of(Flag.COMPOUND);
return keyspace.withSwapped(keyspace.tables.withSwapped(table.withSwapped(flags)));
```

---

## How to Spot

When a schema-altering operation rebuilds a flags/options set from scratch rather than updating an existing one, review every possible flag the table could carry and confirm none are silently dropped. Replacing a set wholesale instead of toggling individual flags is a common source of this class of bug.

---

## Test

`CountersTest` (distributed test) was added to verify that a compact counter table continues to function correctly after `DROP COMPACT STORAGE`.
