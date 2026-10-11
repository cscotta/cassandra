# Bug 0165: VectorMemoryIndex Update Uses Wrong Vector Comparison; RangeTermTree Built for Vector Indexes

**Commit**: 53cd2d8286 · **Ticket**: CASSANDRA-19168
**Subsystem**: src/java/org/apache/cassandra/index/sai/memory/VectorMemoryIndex.java, src/java/org/apache/cassandra/index/sai/view
**Category**: Logic error in condition

---

## Root Cause

Two related bugs were fixed together. First, `VectorMemoryIndex.update` determined whether the old and new vector values differed by comparing raw `ByteBuffer` bytes element-by-element using `IntStream.range` with `ByteBuffer.get(i)` comparisons. This approach reads bytes, not floats, so byte-order differences or values with the same byte pattern in a different float representation could produce incorrect change detection. The fix uses `index.termType().compare(oldValue, newValue)` which applies the type-aware comparator. Second, `View.add` was calling `rangeTermTreeBuilder.add(index)` for *all* SSTable indexes including vector indexes, but the `RangeTermTree` builder's `add` method used a term comparator that is not defined for vectors (they have no natural linear order for range trees), which would produce wrong or error-prone tree structure. The fix skips vector indexes when building the `RangeTermTree`.

### Before / After

```java
// BEFORE – byte-level comparison of vector buffers
different = IntStream.range(0, oldRemaining).anyMatch(i -> oldValue.get(i) != newValue.get(i));
```

```java
// AFTER – type-aware comparison
different = index.termType().compare(oldValue, newValue) != 0;
```

```java
// BEFORE – View adds all indexes to range tree unconditionally
rangeTermTreeBuilder.add(sstableIndex);
```

```java
// AFTER – vector indexes skipped; assertion added in RangeTermTree.Builder.add
if (!indexTermType.isVector())
    rangeTermTreeBuilder.add(sstableIndex);
```

---

## How to Spot

Using raw byte iteration to compare semantic values (floats, longs, composite types) is error-prone because it ignores type semantics, endianness, and codec representation. During review, any comparison of `ByteBuffer` contents for typed index values should use the type's own `compare` method rather than manual byte walks. Additionally, data structures parameterized by a comparator should have assertions or guards ensuring the comparator is compatible with the value type before insertion.

---

## Test

No dedicated new test added in this commit; the existing `VectorMemoryIndex` and view tests cover the updated paths.
