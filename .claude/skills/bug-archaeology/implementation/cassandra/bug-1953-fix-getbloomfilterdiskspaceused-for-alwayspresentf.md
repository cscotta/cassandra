# Bug 1953: getBloomFilterSerializedSize crashes on AlwaysPresentFilter

**Commit**: bc98886d4b · **Ticket**: CASSANDRA-5900
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableReader.java
**Category**: Missing null / bounds check

---

## Root Cause

`SSTableReader.getBloomFilterSerializedSize()` called `FilterFactory.serializedSize(bf, ...)` unconditionally. However, when a bloom filter is disabled for an SSTable the field `bf` holds an `AlwaysPresentFilter` sentinel object, which is not a real bloom filter and cannot be serialized. Passing it to `FilterFactory.serializedSize` causes an error. The fix short-circuits the method to return 0 when the sentinel is detected, matching the physical reality that no bloom filter data is stored on disk for that SSTable.

### Before / After

```java
// BEFORE
public long getBloomFilterSerializedSize()
{
    return FilterFactory.serializedSize(bf, descriptor.version.filterType);
}
```

```java
// AFTER
public long getBloomFilterSerializedSize()
{
    if (bf instanceof AlwaysPresentFilter)
        return 0;
    return FilterFactory.serializedSize(bf, descriptor.version.filterType);
}
```

---

## How to Spot

Whenever a field can hold either a real object or a sentinel/null-object pattern, every method that operates on that field must guard against the sentinel before delegating to code that assumes the real type.

---

## Test

No test added.
