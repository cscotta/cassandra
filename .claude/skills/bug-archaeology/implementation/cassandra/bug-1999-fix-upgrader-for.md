# Bug 1999: Upgrader Uses Deprecated/Wrong API Methods for 2.0 Format

**Commit**: 24da2bcc5d · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/compaction/Upgrader.java
**Category**: Wrong constant / default value

---

## Root Cause

Three call sites in `Upgrader` used APIs that were either global defaults or removed in the 2.0 format: (1) `DatabaseDescriptor.getIndexInterval()` was used instead of the per-table `cfs.metadata.getIndexInterval()`, meaning all tables shared the global default rather than their own configured interval; (2) `SSTableReader.getApproximateKeyCount(toUpgrade)` was called without the `CFMetaData` argument required in 2.0; (3) `SSTableMetadata.createCollector()` was called without the comparator, producing metadata missing type information; and (4) `sstable.markCompacted()` was called instead of `markObsolete()`, which is the correct API for tables that have been fully replaced by an upgraded version.

### Before / After

```java
// BEFORE
long estimatedTotalKeys = Math.max(DatabaseDescriptor.getIndexInterval(),
    SSTableReader.getApproximateKeyCount(toUpgrade));
SSTableMetadata.Collector sstableMetadataCollector = SSTableMetadata.createCollector();
...
sstable.markCompacted();
```

```java
// AFTER
long estimatedTotalKeys = Math.max(cfs.metadata.getIndexInterval(),
    SSTableReader.getApproximateKeyCount(toUpgrade, cfs.metadata));
SSTableMetadata.Collector sstableMetadataCollector = SSTableMetadata.createCollector(cfs.getComparator());
...
sstable.markObsolete();
```

---

## How to Spot

When code is adapted for a new major format version, static or global accessor calls (`DatabaseDescriptor.getX()`) should be reviewed to see whether the equivalent per-instance accessor (`cfMetaData.getX()`) now exists and should be preferred. Similarly, methods that gain new required parameters across versions will compile with the old zero-arg overload if it still exists, silently using a default that may be wrong for the specific table.

---

## Test

No test added.
