# Bug 0407: Flink Dynamic Sink resolves write config at construction time, ignoring per-table settings

**Commit**: b0c236512ae38b7668fa2a332b100db0f5b64d30 · **Ticket**: #15247

## Detection Rule

When a sink or writer component is constructed before the target table is known, verify that write configuration (file format, target file size, compression) is resolved at write time against the actual table, not extracted and cached from a global config object at construction time.

## Code Smell

Fields like `dataFileFormat`, `targetDataFileSize`, and `overwriteMode` are extracted from a `FlinkWriteConf` built without a `Table` reference in the sink constructor and stored as `final` primitives; `DynamicWriter` then uses these frozen values for all tables it later encounters, ignoring each table's own properties.

## Tags

- scope: project-specific
- category: stale-state-after-lifecycle
- code-pattern: config-frozen-before-table-known
- severity: silent-wrong-result
- subsystem: flink/dynamic-sink

## Root Cause

`DynamicIcebergSink` evaluated `FlinkWriteConf` (which merges Flink options with table properties) against a `null` table at builder time, then passed the resulting scalar values (`dataFileFormat`, `targetDataFileSize`) to `DynamicWriter`. Because no table was available yet, table-level overrides were never applied. The fix passes the raw `Configuration` object to `DynamicWriter` and defers `FlinkWriteConf` construction until each table is actually loaded, so per-table properties are honoured.

## Example

```java
// BEFORE — config frozen at sink construction, no table available
DynamicIcebergSink(... FlinkWriteConf flinkWriteConf ...) {
    this.dataFileFormat = flinkWriteConf.dataFileFormat();   // null-table context
    this.targetDataFileSize = flinkWriteConf.targetDataFileSize();
}
// In DynamicWriter, per-table loading uses the frozen values:
return new RowDataTaskWriterFactory(..., targetDataFileSize, dataFileFormat, ...);
```

```java
// AFTER — raw config stored; resolved per-table at write time
DynamicIcebergSink(... Configuration flinkConfig ...) {
    this.flinkConfig = flinkConfig;
}
// In DynamicWriter, after table is loaded:
FlinkWriteConf flinkWriteConf = new FlinkWriteConf(table, commonWriteProperties, flinkConfig);
return new RowDataTaskWriterFactory(...,
    flinkWriteConf.targetDataFileSize(), flinkWriteConf.dataFileFormat(), ...);
```
