# Bug 0244: Spark streaming checkpoint uses table FileIO instead of HadoopFileIO

**Commit**: 42d01511c52032e4c08da3d08d5e45b980dacffb · **Ticket**: #15574

## Detection Rule

When a component writes files to a path that is independent of the table's storage location (e.g., a local or Hadoop-native checkpoint path), verify it uses a generic filesystem implementation (e.g., `HadoopFileIO`) rather than the table-specific `FileIO`, which may be scoped to the table's storage backend and lack credentials or support for the checkpoint path.

## Code Smell

An `InitialOffsetStore` constructor that assigns `this.io = table.io()` where `io` is subsequently used to write checkpoint files to a path provided by the streaming framework (not the table location). The table's `FileIO` is a custom implementation configured for the table's storage (S3, GCS, ADLS, etc.) and may not be able to access local or HDFS checkpoint paths.

## Tags

- scope: project-specific
- category: wrong-constant
- code-pattern: wrong-io-for-path
- severity: crash
- subsystem: spark/streaming/micro-batch

## Root Cause

`InitialOffsetStore` used `table.io()` to read and write the initial offset checkpoint file. When a table has a custom `FileIO` (e.g., an S3-specific implementation), that `FileIO` cannot handle local or HDFS checkpoint paths, causing failures at stream startup. Spark streaming checkpoints are always written to a Hadoop-compatible path, so the fix is to instantiate a `HadoopFileIO` from the Spark context's Hadoop configuration instead of reusing the table's `FileIO`.

## Example

```java
// BEFORE
InitialOffsetStore(Table table, String checkpointLocation, Long fromTimestamp) {
  this.table = table;
  this.io = table.io();  // table IO may not support checkpoint path
  this.initialOffsetLocation = SLASH.join(checkpointLocation, "offsets/0");
  this.fromTimestamp = fromTimestamp;
}
```

```java
// AFTER
InitialOffsetStore(Table table, String checkpointLocation,
                  long fromTimestamp, Configuration conf) {
  this.table = table;
  this.io = new HadoopFileIO(conf);  // always works for checkpoint paths
  this.initialOffsetLocation = SLASH.join(checkpointLocation, "offsets/0");
  this.fromTimestamp = fromTimestamp;
}
```
