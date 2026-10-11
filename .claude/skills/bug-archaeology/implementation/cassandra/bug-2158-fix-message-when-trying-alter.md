# Bug 2158: ALTER TABLE on Clustering Key Type Produces No Useful Error Message

**Commit**: bac979410d · **Ticket**: CASSANDRA-5012
**Subsystem**: src/java/org/apache/cassandra/config/CFMetaData.java, src/java/org/apache/cassandra/service/MigrationManager.java
**Category**: Logic error in condition

---

## Root Cause

Schema compatibility validation in `CFMetaData.apply()` was only executed when changes were applied locally; `MigrationManager.announceColumnFamilyUpdate()` skipped the validation entirely before broadcasting the update. This meant that an attempt to alter a clustering key type would either silently propagate an incompatible schema or fail with a generic/misleading error rather than a descriptive `ConfigurationException`. The fix extracts validation into a public `validateCompatility()` method and calls it from both the local apply path and the migration announcement path.

### Before / After

```java
// BEFORE — MigrationManager: no validation before announcing
if (oldCfm == null)
    throw new ConfigurationException(...);
logger.info("Update ColumnFamily ...");
announce(oldCfm.toSchemaUpdate(cfm, ...));
```

```java
// AFTER
if (oldCfm == null)
    throw new ConfigurationException(...);
oldCfm.validateCompatility(cfm);   // validate before announcing
logger.info("Update ColumnFamily ...");
announce(oldCfm.toSchemaUpdate(cfm, ...));
```

---

## How to Spot

When validation logic is inlined in one code path (local apply) but not in another (remote announce), schema mutations can be distributed without validation. Extract validation into a shared method and call it from every entry point that modifies schema.

---

## Test

No test added.
