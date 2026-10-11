# Bug 1943: SSTable in batch mode loads real Bloom filter then overwrites with AlwaysPresentFilter

**Commit**: f5c9b4a8b9 · **Ticket**: CASSANDRA-5938
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableReader.java, src/java/org/apache/cassandra/utils/FilterFactory.java, src/java/org/apache/cassandra/utils/IFilter.java, src/java/org/apache/cassandra/utils/AlwaysPresentFilter.java, src/java/org/apache/cassandra/utils/Murmur3BloomFilter.java
**Category**: Wrong constant / default value

---

## Root Cause

`SSTableReader.openForBatch` intended to skip loading the Bloom filter (batch mode does not need it) and use `AlwaysPresentFilter` instead. However, the original code set `bf = new AlwaysPresentFilter()` before calling `sstable.load(true, false)`, where the first `true` argument meant "load the Bloom filter from disk". The `load` call overwrote the sentinel with the real on-disk filter, defeating the optimization and causing unnecessary I/O and memory usage. The fix reorders the calls — passing `false` to suppress BF loading — then assigns the shared `FilterFactory.AlwaysPresent` singleton after `load` returns. Additionally, `getBloomFilterSerializedSize` contained an `instanceof` check to return 0 for `AlwaysPresentFilter`; this was replaced by a `serializedSize()` method on the `IFilter` interface, implemented to return 0 in `AlwaysPresentFilter` and the real size in `Murmur3BloomFilter`.

### Before / After

```java
// BEFORE — BF loaded from disk, then overwritten
sstable.bf = new AlwaysPresentFilter();
sstable.load(true, false);   // first arg: loadBloomFilter=true
```

```java
// AFTER — BF load suppressed, singleton assigned after
sstable.load(false, false);
sstable.bf = FilterFactory.AlwaysPresent;
```

```java
// BEFORE — instanceof check scattered at call site
public long getBloomFilterSerializedSize() {
    if (bf instanceof AlwaysPresentFilter)
        return 0;
    return FilterFactory.serializedSize(bf);
}

// AFTER — polymorphic via interface method
public long getBloomFilterSerializedSize() {
    return bf.serializedSize();
}
```

---

## How to Spot

When a "skip expensive operation" path assigns a placeholder value before calling an initializer, verify that the initializer cannot overwrite the placeholder. Prefer assigning sentinels after initialization. Also watch for scattered `instanceof` type checks that should instead be polymorphic interface methods.

---

## Test

No test added.
