# Bug 0125: Missing Override Causes Heap Allocation in NativeCell.valueSize() and NativeClustering.dataSize()

**Commit**: 4ae1aee39b · **Ticket**: CASSANDRA-20162
**Subsystem**: src/java/org/apache/cassandra/db / src/java/org/apache/cassandra/db/rows
**Category**: Missing override of default method

---

## Root Cause

`NativeCell` and `NativeClustering` are off-heap (`offheap_objects`) implementations of `Cell` and `Clustering` that store data directly in native memory via `MemoryUtil`. Their parent classes provide default implementations of `valueSize()` and `dataSize()` that materialize the value as a heap `ByteBuffer` and call `remaining()` on it. Because `NativeCell` and `NativeClustering` did not override these methods, every call to `valueSize()`/`dataSize()` triggered an unnecessary heap `ByteBuffer` allocation just to read a size field that is already stored inline in the native memory layout. The fix adds direct overrides that read the size directly from the known native memory offset.

### Before / After

```java
// BEFORE – NativeCell inherits the default: allocates a ByteBuffer to get the length
// (no override in NativeCell; parent default calls value().remaining())
```

```java
// AFTER – NativeCell reads the length field directly from native memory
public int valueSize()
{
    return MemoryUtil.getInt(peer + LENGTH);
}

// AFTER – NativeClustering reads the dataSize field from the metadata area
public int dataSize()
{
    int dataSizeOffset = (size() * 2) + 2; // metadataSize - 2
    return MemoryUtil.getShort(peer + dataSizeOffset);
}
```

---

## How to Spot

When a class stores data off-heap and overrides accessors like `get(i)` to read from native memory, it should also override all size/length query methods that the parent implements in terms of those accessors; failing to do so means the "fast path" size query silently falls back to the "slow path" that allocates heap objects. During review, for every off-heap class, check that every method whose default implementation materialises heap data is either overridden with a direct native read or explicitly documented as intentionally using the slow path.

---

## Test

`NativeCellTest` adds assertions comparing `dataSize()` on the native and heap-backed clustering objects, and `valueSize()` on each cell across rows, verifying that the native implementations return the same values as the heap implementations without any allocation discrepancy.
