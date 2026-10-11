# Bug 0411: Dynamic sink write config resolved at construction time, ignoring per-table properties

**Commit**: ccf4d91e1c0d3db1b723cb2cebb51253b0e05790 · **Ticket**: #15237

## Detection Rule

When a configuration object is resolved eagerly at sink/operator construction time and later used across multiple tables or dynamic targets, verify that the resolved values incorporate per-target properties that are only available at write time.

## Code Smell

A `FlinkWriteConf` (or similar config wrapper) is instantiated in a builder/constructor before any table is loaded, and its resolved scalar fields (`dataFileFormat`, `targetDataFileSize`, `overwriteMode`, etc.) are stored as final fields and threaded to the per-table writer factory without re-consulting the target table's own properties.

## Tags

- scope: project-specific
- category: stale-state-after-lifecycle
- code-pattern: stale-config-at-construction
- severity: silent-wrong-result
- subsystem: flink/dynamic-sink

## Root Cause

`DynamicIcebergSink` called `new FlinkWriteConf(writeOptions, readableConfig)` inside the builder's `build()` method and stored the resolved `dataFileFormat` and `targetDataFileSize` as primitive fields. When `DynamicWriter` later loaded each table at runtime, it merged `table.properties()` with `commonWriteProperties` but never re-resolved a `FlinkWriteConf` against the table, so per-table property overrides were silently ignored. The fix passes the raw `Configuration` object through to the writer and creates a `FlinkWriteConf(table, commonWriteProperties, flinkConfig)` at the point where each table's writer factory is created.

## Example

```java
// BEFORE — FlinkWriteConf resolved once at sink construction, table properties ignored
DynamicIcebergSink(... FlinkWriteConf flinkWriteConf ...) {
    this.dataFileFormat = flinkWriteConf.dataFileFormat();     // fixed at build time
    this.targetDataFileSize = flinkWriteConf.targetDataFileSize();
}

// In DynamicWriter, per-table:
Map<String, String> tableWriteProperties = Maps.newHashMap(table.properties());
tableWriteProperties.putAll(commonWriteProperties);
// dataFileFormat and targetDataFileSize come from the stale construction-time values
```

```java
// AFTER — FlinkWriteConf created fresh per table at write time
DynamicWriter(... Configuration flinkConfig ...) {
    this.flinkConfig = flinkConfig;
}

// In DynamicWriter, per-table:
FlinkWriteConf flinkWriteConf = new FlinkWriteConf(table, commonWriteProperties, flinkConfig);
Map<String, String> tableWriteProperties =
    SinkUtil.writeProperties(flinkWriteConf.dataFileFormat(), flinkWriteConf, table);
// format and size now incorporate table.properties() correctly
```
