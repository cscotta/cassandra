# Bug 1453: SSTableReader resolves wrong CFMetaData for secondary-index SSTables

**Commit**: e68f52b8d1 · **Ticket**: CASSANDRA-8027
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableReader.java
**Category**: Wrong constant / default value

---

## Root Cause

`SSTableReader.getCompressionMetadata()` set live schema metadata on compression parameters by calling `Schema.instance.getCFMetaData(descriptor)` — a lookup keyed on the SSTable's own descriptor. For secondary-index SSTables the descriptor name encodes the index name (e.g., `table.index`), not the parent table name, and `getCFMetaData()` returned `null` or the wrong entry. This meant that when a table's `crc_check_chance` was altered via `ALTER TABLE`, the change was not propagated to the index SSTable's compression parameters, leaving index reads using the stale value. The fix looks up the parent table name by stripping the `.indexName` suffix before performing the schema lookup.

### Before / After

```java
// BEFORE — uses the index SSTable's own descriptor, returns null/wrong metadata
cmd.parameters.setLiveMetadata(Schema.instance.getCFMetaData(descriptor));
```

```java
// AFTER — resolves the parent CF name for index SSTables
String cfName = metadata.isSecondaryIndex() ? metadata.getParentColumnFamilyName() : metadata.cfName;
cmd.parameters.setLiveMetadata(Schema.instance.getCFMetaData(metadata.ksName, cfName));
```

---

## How to Spot

Any schema lookup that uses a raw SSTable descriptor or CF name without first checking whether the table is a secondary index is suspect, because secondary-index CF names follow the convention `parent.indexName`. Whenever `Schema.instance.getCFMetaData()` is called inside SSTable-level code, verify that it handles the index-name format explicitly; a null return or unchecked cast downstream is the telltale symptom.

---

## Test

`CrcCheckChanceTest` extended to create a secondary index, flush, compact, then verify that `crc_check_chance` is correctly reflected on both the base table and index SSTables after an `ALTER TABLE`.
