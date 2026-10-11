# Bug 1454: SSTableReader resolves wrong CFMetaData for secondary-index SSTables (2nd branch)

**Commit**: dbb1fa5690 · **Ticket**: CASSANDRA-8027
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableReader.java
**Category**: Wrong constant / default value

---

## Root Cause

This commit applies the identical fix as bug-1453 to a second code branch (a different version lineage in the repository). `SSTableReader.getCompressionMetadata()` called `Schema.instance.getCFMetaData(descriptor)` for the schema lookup, which fails for secondary-index SSTables because their descriptor name contains the index suffix (e.g., `table.index`) rather than the plain parent table name. The result is that `crc_check_chance` alterations are not propagated to index SSTable readers. The same `isSecondaryIndex()` / `getParentColumnFamilyName()` helpers were introduced in `CFMetaData` and the same lookup correction was applied in `SSTableReader`.

### Before / After

```java
// BEFORE
cmd.parameters.setLiveMetadata(Schema.instance.getCFMetaData(descriptor));
```

```java
// AFTER
String cfName = metadata.isSecondaryIndex() ? metadata.getParentColumnFamilyName() : metadata.cfName;
cmd.parameters.setLiveMetadata(Schema.instance.getCFMetaData(metadata.ksName, cfName));
```

---

## How to Spot

See bug-1453. The same pattern — a schema lookup using an unmodified SSTable descriptor without special-casing index CF names — exists in all branches carrying compressed secondary indexes. Any time a fix is applied to one branch of a multi-branch codebase, check whether the same vulnerable pattern exists in sibling branches.

---

## Test

`CrcCheckChanceTest` extended (same additions as bug-1453): creates a secondary index, flushes and compacts multiple SSTables, then asserts that `crc_check_chance` propagates correctly to index SSTables after schema alteration.
