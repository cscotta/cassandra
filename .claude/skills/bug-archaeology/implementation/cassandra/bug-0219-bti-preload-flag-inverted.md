# Bug 0219: BTI Partition Index Opened with Inverted Preload Flag

**Commit**: e9198d6a66 · **Ticket**: CASSANDRA-18677
**Subsystem**: io/sstable/format/bti (BTI SSTable format, BtiTableReaderLoadingBuilder)
**Category**: Wrong constant / default value

---

## Root Cause

`BtiTableReaderLoadingBuilder` called `openPartitionIndex(builder.getFilter().isInformative())` to decide whether to preload the partition index into memory. The intent is to preload only when there is no bloom filter (i.e., the filter is not informative, so the index must be traversed to find partitions), but the condition was inverted: it preloaded when the filter *was* informative (bloom filter present) and skipped preloading when the filter was absent — the exact opposite of the desired behaviour. The fix is a one-character negation: `openPartitionIndex(!builder.getFilter().isInformative())`.

### Before / After

```java
// BEFORE – preloads when bloom filter IS present (wrong)
builder.setPartitionIndex(openPartitionIndex(builder.getFilter().isInformative()));
```

```java
// AFTER – preloads when bloom filter is NOT present (correct)
builder.setPartitionIndex(openPartitionIndex(!builder.getFilter().isInformative()));
```

---

## How to Spot

Boolean flags that express "do X when condition Y is absent" are easy to invert. When reviewing code where a flag controls a performance-critical decision (like preloading a large index file), look for a mismatch between the semantics of the condition method name (`isInformative` meaning "a reliable filter exists") and the desired action (preload only when *no* reliable filter exists). Unit tests that inject byteman rules to intercept the actual flag value passed to the lower-level API are the most reliable way to catch this class of inversion.

---

## Test

`LoadingBuilderTest.testPreloadFlag` uses a Byteman rule to intercept the `preload` argument passed to `PartitionIndex.load(...)` and asserts that it matches `disableBloomFilter` for both the flush path (always false) and the reopen-from-disk path (true iff bloom filter is disabled).
