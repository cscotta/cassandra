# Bug 2177: Range Slice Scan Uses Thrift SlicePredicate Instead of Internal Filter After Merge

**Commit**: 660016658f · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/service/StorageProxy.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

After merging from the 1.1 branch, the `getRangeSlice` path in `StorageProxy` continued to use the Thrift-layer `SlicePredicate` type for constructing the empty predicate used in range scan paging. Post-merge the internal `IDiskAtomFilter` / `SliceQueryFilter` abstraction replaced `SlicePredicate` throughout the storage layer, so constructing and passing a Thrift `SlicePredicate` would either fail to compile or be interpreted incorrectly. The fix replaces both the helper method and the call site with the correct internal `SliceQueryFilter` type.

### Before / After

```java
// BEFORE
private static SlicePredicate getEmptySlicePredicate() {
    final SliceRange emptySliceRange =
        new SliceRange(ByteBufferUtil.EMPTY_BYTE_BUFFER, ByteBufferUtil.EMPTY_BYTE_BUFFER, false, -1);
    return new SlicePredicate().setSlice_range(emptySliceRange);
}
```

```java
// AFTER
private static IDiskAtomFilter getEmptySlicePredicate() {
    return new SliceQueryFilter(ByteBufferUtil.EMPTY_BYTE_BUFFER,
                                ByteBufferUtil.EMPTY_BYTE_BUFFER,
                                false,
                                -1);
}
```

---

## How to Spot

After a branch merge that changes a core abstraction (e.g. Thrift type to internal type), search for all usages of the old type in the merged files to ensure none were left behind. Compiler errors should catch most cases, but when both types happen to exist, silent semantic bugs can slip through.

---

## Test

No test added.
