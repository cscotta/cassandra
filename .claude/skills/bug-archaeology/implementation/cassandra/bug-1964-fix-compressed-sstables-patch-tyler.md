# Bug 1964: Bulk-loading compressed SSTables fails due to wrong type cast

**Commit**: ab8a28e365 · **Ticket**: CASSANDRA-5820
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableReader.java, src/java/org/apache/cassandra/io/util/CompressedPoolingSegmentedFile.java, src/java/org/apache/cassandra/io/util/CompressedSegmentedFile.java, src/java/org/apache/cassandra/io/sstable/SSTableSimpleUnsortedWriter.java
**Category**: Wrong constant / default value

---

## Root Cause

`SSTableReader.getCompressionMetadata()` hard-cast `dfile` to `CompressedPoolingSegmentedFile` to access its `metadata` field. When an SSTable was opened through the bulk-loader path, `dfile` was a `CompressedSegmentedFile` instead, causing a `ClassCastException` at load time. The fix introduces a new `ICompressedFile` interface implemented by both `CompressedSegmentedFile` and `CompressedPoolingSegmentedFile`, and changes the cast to use that interface. Additionally, `SSTableSimpleUnsortedWriter` was not propagating `CFMetaData` (including `CompressionParameters`) to the writer thread, so compressed SSTables were written without compression even when explicitly requested; a new constructor chains correctly to ensure `bufferSize` and `diskWriter.start()` are always paired with the full metadata.

### Before / After

```java
// BEFORE – SSTableReader.getCompressionMetadata()
return ((CompressedPoolingSegmentedFile) dfile).metadata;
```

```java
// AFTER
return ((ICompressedFile) dfile).getMetadata();

// ICompressedFile is now implemented by both CompressedSegmentedFile
// and CompressedPoolingSegmentedFile
```

---

## How to Spot

Hard-casting an interface or abstract field to a specific concrete subtype is fragile; any alternative implementation of the same abstract type will produce a `ClassCastException`. Prefer interface abstractions when the field can hold multiple concrete types. Additionally, when a constructor delegates upward via `super(...)` but then adds its own initialisation, check that all sibling constructors also call `this(...)` so no initialisation step is silently skipped.

---

## Test

`SSTableLoaderTest` was added, verifying that an SSTable written with `SSTableSimpleUnsortedWriter` can be streamed via `SSTableLoader` and all rows are readable afterwards.
