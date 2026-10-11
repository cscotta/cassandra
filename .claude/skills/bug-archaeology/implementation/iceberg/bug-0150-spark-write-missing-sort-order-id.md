# Bug 0150: Spark writes do not set sort_order_id on data files in manifest

**Commit**: 420fb1a688926440582fe078d7348f60f27997a8 · **Ticket**: #15150

## Detection Rule

When a writer factory serializes table metadata and produces `DataFile` objects, verify that the sort order ID from the write configuration is passed through to the file builder, not left at the default unsorted value.

## Code Smell

A `WriterFactory` or `PositionDeltaWriteFactory` that passes a `Table` broadcast to executors but constructs `DataFileWriter` without calling `.dataSortOrder(...)`, causing all written files to record `sort_order_id = 0` regardless of the table's actual sort order.

## Tags

- scope: project-specific
- category: silent-skip
- code-pattern: missing-field-propagation
- severity: silent-wrong-result
- subsystem: spark/write

## Root Cause

`SparkWrite.WriterFactory` and `SparkPositionDeltaWrite.PositionDeltaWriteFactory` created file writers without providing the sort order ID, so every written `DataFile` recorded `sort_order_id = 0` (unsorted) in the manifest even when the data was sorted per the table's sort order. `SerializableTable.sortOrders()` also always delegated to `lazyTable.sortOrders()` (requiring a full table load on executors) because the serialized sort-order map was never populated, making `outputSortOrderId` unusable on executor side. Both problems were fixed together: the sort-order map is now serialized into `SerializableTable`, and the resolved `sortOrderId` is propagated from the driver into each factory and passed to the file writer via `.dataSortOrder(...)`.

## Example

```java
// BEFORE
return new WriterFactory(
    tableBroadcast, queryId, format, outputSpecId,
    writeSchema, dsSchema, useFanoutWriter, writeProperties);

// in WriterFactory.createWriter():
SparkAppenderFactory.builderFor(table, writeSchema, dsSchema)
    .writeProperties(writeProperties)
    .build();
```

```java
// AFTER
int sortOrderId = writeConf.outputSortOrderId(writeRequirements);
return new WriterFactory(
    tableBroadcast, queryId, format, outputSpecId,
    writeSchema, dsSchema, useFanoutWriter, writeProperties, sortOrderId);

// in WriterFactory.createWriter():
SparkAppenderFactory.builderFor(table, writeSchema, dsSchema)
    .writeProperties(writeProperties)
    .dataSortOrder(table.sortOrders().get(sortOrderId))
    .build();
```
