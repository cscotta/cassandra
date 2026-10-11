# Bug 0193: NullPointerException when engineSchema is null in format model writers

**Commit**: 2874fc46e8b307b93e2ebb1a849fd7365fb3cb31 · **Ticket**: #15688

## Detection Rule

When a factory or builder accepts an optional engine-specific schema that callers may legally omit (pass as null), verify that every code path that uses that schema parameter guards against null before dereferencing it, rather than relying on all callers to supply it.

## Code Smell

A writer constructor call like `new FlinkAvroWriter(engineSchema)` or `buildWriter(engineSchema, fileSchema)` inside a lambda that receives `engineSchema` from an external registry, with no null check on `engineSchema` before forwarding it to a class that will unconditionally use it.

## Tags

- scope: universal
- category: missing-null-check
- code-pattern: null-forwarded-to-constructor
- severity: crash
- subsystem: flink/data, spark/data

## Root Cause

The format model registry passes `engineSchema` (the engine-specific row type) to writer factories; the parameter is optional and may be null when callers invoke the builder without supplying an engine schema. The Flink and Spark Avro, ORC, and Parquet writer factories forwarded the raw (possibly null) value directly to constructors and `buildWriter` methods that do not tolerate null. The fix adds a null-coalescing layer: when `engineSchema` is null, derive it from the Iceberg schema using `FlinkSchemaUtil.convert()` / `SparkSchemaUtil.convert()`.

## Example

```java
// BEFORE — engineSchema forwarded unconditionally; crashes if null
(icebergSchema, fileSchema, engineSchema) -> new FlinkAvroWriter(engineSchema)

// OrcWriter factory:
public static OrcRowWriter<RowData> buildWriter(RowType rowType, Schema iSchema) {
    return new FlinkOrcWriter(rowType, iSchema); // NPE if rowType is null
}
```

```java
// AFTER — null-safe fallback to converted Iceberg schema
(icebergSchema, fileSchema, engineSchema) ->
    new FlinkAvroWriter(icebergSchema, engineSchema)

// new constructor:
public FlinkAvroWriter(org.apache.iceberg.Schema icebergSchema, RowType engineSchema) {
    this(engineSchema != null ? engineSchema : FlinkSchemaUtil.convert(icebergSchema));
}

// OrcWriter factory:
public static OrcRowWriter<RowData> buildWriter(RowType rowType, Schema iSchema) {
    return new FlinkOrcWriter(
        rowType != null ? rowType : FlinkSchemaUtil.convert(iSchema), iSchema);
}
```
