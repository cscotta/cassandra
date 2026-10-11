# Bug 0010: Snapshot procedure fails for tables containing Variant columns

**Commit**: d71583f58859131668cbf85b0df293cd32ef165a · **Ticket**: #15964

## Detection Rule

When resolving a file format from a Hive/Spark serde class name, verify that the code handles all known Iceberg types (including newer ones like Variant) and does not silently treat the serde value as the file format string when it is an unrecognised class name.

## Code Smell

Code that unconditionally uses `storage().serde().get()` as the file format string — without checking whether the serde value is actually a known file format keyword (parquet/avro/orc) — will pass an unrecognised serde class name to downstream format resolution, causing a crash or wrong behavior for tables with non-standard column types.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: sentinel-not-handled
- severity: crash
- subsystem: spark/SparkTableUtil, hive-metastore/HiveSchemaUtil

## Root Cause

When Spark tables contain Variant columns, Spark's Hive catalog stores a Variant-specific serde class name in `storage().serde()`. The snapshot procedure's `toSparkPartition` and `snapshotUnpartitionedStagingTable` methods used the raw serde string as the file format (e.g. passing a Hive serde class name to `TableMigrationUtil.listPartition` as the format), which caused the procedure to crash. Two fixes were needed: (1) `SparkTableUtil.resolveFileFormat` now checks whether the serde string contains a known format keyword before using it, falling back to `table.provider()` otherwise; (2) `HiveSchemaUtil.convertToHiveType` now maps `VARIANT` to `"unknown"` instead of falling through to an unhandled case.

## Example

```java
// BEFORE: raw serde string used directly as file format
Option<String> serde = partition.storage().serde();
String format = serde.nonEmpty() ? serde.get() : table.provider().get();
// If serde is a Variant serde class name, format is wrong/crash

// HiveSchemaUtil: VARIANT type has no case -> falls to default -> throws
switch (type.typeId()) {
    case FIXED:
    case BINARY:
        return "binary";
    // VARIANT missing — UnsupportedOperationException
```

```java
// AFTER: validate serde is a known file format before using it
String format = resolveFileFormat(partitionSerde.getOrElse(() -> null), table);

private static String resolveFileFormat(String partitionSerde, CatalogTable table) {
    if (partitionSerde != null && isKnownFileFormat(partitionSerde)) return partitionSerde;
    Option<String> serde = table.storage().serde();
    if (serde.nonEmpty() && isKnownFileFormat(serde.get())) return serde.get();
    return table.provider().get(); // fall back to provider (e.g. "parquet")
}

// HiveSchemaUtil: VARIANT mapped to "unknown"
case VARIANT:
    return "unknown";
```
