# Bug 0316: ObjectSizes Methods Return Wrong Heap Sizes for ByteBuffers and Arrays

**Commit**: a690f339ab · **Ticket**: CASSANDRA-17402
**Subsystem**: src/java/org/apache/cassandra/utils/ObjectSizes.java
**Category**: Wrong constant / default value

---

## Root Cause

Several `ObjectSizes` helper methods used fixed constants (e.g., `sizeOfEmptyHeapByteBuffer()`, `sizeOfEmptyByteArray()`) rather than computing sizes relative to the actual object's characteristics.  Three specific problems existed: (1) `sizeOnHeapOf(ByteBuffer)` failed to account for the backing array overhead when the buffer was not a slice, returning just the data bytes for a dedicated buffer; (2) `sizeOnHeapExcludingData(ByteBuffer)` did not distinguish sliced buffers (whose backing array is shared) from dedicated ones, always adding the same constant regardless of array length; (3) callers in `ArrayCell`, `BufferCell`, `CellPath`, and `ColumnIdentifier` called the empty-object constant methods unconditionally, so the header overhead was always the same whether the underlying array was 0 or 1 MB.  Additionally, `FullQueryLogger` weight calculations omitted several reference-slot fields and used `8` / `4` magic numbers instead of `Long.BYTES` / `Integer.BYTES`.  The fix replaces all fixed-constant methods with proper size computations that inspect the actual array length and distinguish slice vs. dedicated buffers.

### Before / After

```java
// BEFORE – sizeOnHeapOf always treats non-slice buffer the same as slice
if (buffer.capacity() > buffer.remaining())
    return buffer.remaining();
return BUFFER_EMPTY_SIZE + sizeOfArray(buffer.capacity(), 1);

// callers
return EMPTY_SIZE + ObjectSizes.sizeOfEmptyHeapByteBuffer(); // ColumnIdentifier
return EMPTY_SIZE + ObjectSizes.sizeOfEmptyHeapByteBuffer(); // BufferCell
return EMPTY_SIZE + ObjectSizes.sizeOfEmptyByteArray() + ...; // ArrayCell
```

```java
// AFTER – distinguishes dedicated vs. slice, heap vs. direct
int arrayLen = buffer.array().length;
int bufLen = buffer.remaining();
if (arrayLen > bufLen)
    return EMPTY_HEAP_BUFFER_SIZE + bufLen; // slice: count data bytes only
return EMPTY_HEAP_BUFFER_SIZE + (arrayLen == 0 ? EMPTY_BYTE_ARRAY_SIZE : sizeOfArray(arrayLen, 1));

// callers
return EMPTY_SIZE + ObjectSizes.sizeOnHeapExcludingData(bytes); // ColumnIdentifier
return EMPTY_SIZE + ObjectSizes.sizeOnHeapExcludingData(value); // BufferCell
return EMPTY_SIZE + ObjectSizes.sizeOfArray(value) - value.length + ...; // ArrayCell
```

---

## How to Spot

When reviewing memory-metering utilities, look for methods that return a constant computed from an empty prototype object and then check whether every caller is passing an equally empty object.  Constants like `sizeOfEmptyXxx()` are unsafe once the real objects have variable-length backing data; callers must measure the actual instance or compute the delta between the header (fixed) and the data (variable).  Always cross-check the returned size against a live `MemoryMeter.measureDeep()` result in a unit test.

---

## Test

New `ObjectSizesTest` class exercises `sizeOnHeapOf` and `sizeOnHeapExcludingData` for empty buffers, full buffers, direct buffers, prefix slices, suffix slices, and infix slices, asserting exact byte counts derived from `MemoryLayoutSpecification`.
