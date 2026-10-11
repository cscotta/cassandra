# Bug 2073: Wrong Default for maxTimestamp in SSTableMetadata Constructor Causes Incorrect SSTable Skipping

**Commit**: b4d26bb4e4 · **Ticket**: CASSANDRA-5372
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableMetadata.java
**Category**: Wrong constant / default value

---

## Root Cause

The `SSTableMetadata` default constructor initialised `maxTimestamp` to `Long.MIN_VALUE`. This value is used to determine whether an SSTable might contain data relevant to a query: when the SSTable format does not track a max timestamp, the safe default is `Long.MAX_VALUE` (meaning "could contain anything"), not `Long.MIN_VALUE` (meaning "contains nothing recent"). The same wrong default appeared when deserialising older-format SSTables that lack a stored timestamp. Both paths caused older SSTables to be incorrectly excluded from reads.

### Before / After

```java
// BEFORE (constructor default)
this(defaultRowSizeHistogram(),
     defaultColumnCountHistogram(),
     ReplayPosition.NONE,
     Long.MIN_VALUE,   // maxTimestamp — wrong sentinel
     ...);

// BEFORE (deserialisation)
long maxTimestamp = desc.containsTimestamp() ? dis.readLong() : Long.MIN_VALUE;
```

```java
// AFTER
     Long.MAX_VALUE,   // maxTimestamp — correct sentinel

long maxTimestamp = desc.containsTimestamp() ? dis.readLong() : Long.MAX_VALUE;
```

---

## How to Spot

When a sentinel value governs whether data is included or excluded, ask: does this sentinel lean toward inclusion (safe/conservative) or exclusion (dangerous/lossy)? For "max timestamp not known", `Long.MAX_VALUE` is the only safe choice; `Long.MIN_VALUE` silently discards data.

---

## Test

No test added.
