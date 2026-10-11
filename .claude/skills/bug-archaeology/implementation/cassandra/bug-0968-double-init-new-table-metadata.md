# Bug 0968: Double initialization of metadata for new tables

**Commit**: cb21f28eca · **Ticket**: CASSANDRA-11027
**Subsystem**: Schema management
**Category**: Logic error in condition

---

## Root Cause
When a new table was added via `Schema.load(CFMetaData)`, the code called `Keyspace.open(cfm.ksName).initCf(cfm, true)` to both open the keyspace and initialize the column family. However, `initCf` was also invoked implicitly as a side effect of the subsequent `update(...)` call that swapped the tables map with the new metadata. This caused the column family to be initialized twice — once explicitly and once through the schema update pathway — leading to duplicate state setup.

The fix removes the explicit `initCf` call, leaving only the `Keyspace.open(cfm.ksName)` call to ensure the keyspace is initialized, and relies on the schema `update()` path to initialize the table correctly.

### Before / After
```java
// BEFORE
// Make sure the keyspace is initialized and initialize the table.
Keyspace.open(cfm.ksName).initCf(cfm, true);
// Update the keyspaces map with the updated metadata
update(cfm.ksName, ks -> ks.withSwapped(ks.tables.with(cfm)));

// AFTER
// Make sure the keyspace is initialized
Keyspace.open(cfm.ksName);
// Update the keyspaces map with the updated metadata
update(cfm.ksName, ks -> ks.withSwapped(ks.tables.with(cfm)));
```

---

## How to Spot
When reviewing schema mutation paths, check whether any operation that adds a new table both calls an explicit init method and then also calls an update method that performs initialization internally. The pattern `open(ks).initCf(...)` followed immediately by `update(ks, ks -> ks.withSwapped(ks.tables.with(cfm)))` is the tell: if the update path already triggers CF setup, the preceding `initCf` is redundant and potentially harmful.

---

## Test
No test added.
