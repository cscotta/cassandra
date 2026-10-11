# Bug 0117: sort_order_id not recorded in manifest for Spark-written data files

**Commit**: 1a2a8a5ecc701629183405ca842f2d1eb23505ee · **Ticket**: #15832

## Detection Rule

When a write path constructs a file writer (DataFileWriter / RowDeltaWriter) and the table has a non-trivial sort order, verify that `dataSortOrder(...)` is called on the writer builder so the resulting manifest entry carries the correct `sort_order_id`.

## Code Smell

A `WriterFactory.createWriter()` or equivalent that calls `.writeProperties(...)` and `.build()` on a file-appender builder but never calls `.dataSortOrder(...)`, while the surrounding write path does resolve distribution-and-ordering requirements.

## Tags

- scope: project-specific
- category: silent-skip
- code-pattern: missing-metadata-field-on-write
- severity: silent-wrong-result
- subsystem: spark/source write path

## Root Cause

`SparkWrite.WriterFactory` and `SparkPositionDeltaWrite.PositionDeltaWriteFactory` built file appenders without passing a `dataSortOrder`, so the manifest entries for every file written through Spark stored the default `sort_order_id = 0` (unsorted) regardless of the table's configured sort order. The sort order was correctly applied to the data layout via Spark distribution-and-ordering requirements, but that information was never threaded back into the file metadata.

## Example

```java
// BEFORE — WriterFactory.createWriter(), sort order never passed
return SparkAppenderFactory.builderFor(table, writeSchema, dsSchema)
    .spec(spec)
    .writeProperties(writeProperties)
    .build();

// BEFORE — PositionDeltaWriteFactory, same omission
return SparkAppenderFactory.builderFor(table, writeSchema, deleteSchema)
    .deleteFileFormat(context.deleteFileFormat())
    .writeProperties(writeProperties)
    .build();
```

```java
// AFTER — sort order id resolved once and threaded in
int sortOrderId = writeConf.outputSortOrderId(writeRequirements);

// WriterFactory.createWriter()
return SparkAppenderFactory.builderFor(table, writeSchema, dsSchema)
    .spec(spec)
    .writeProperties(writeProperties)
    .dataSortOrder(table.sortOrders().get(sortOrderId))
    .build();

// PositionDeltaWriteFactory
return SparkAppenderFactory.builderFor(table, writeSchema, deleteSchema)
    .deleteFileFormat(context.deleteFileFormat())
    .writeProperties(writeProperties)
    .dataSortOrder(table.sortOrders().get(sortOrderId))
    .build();
```
