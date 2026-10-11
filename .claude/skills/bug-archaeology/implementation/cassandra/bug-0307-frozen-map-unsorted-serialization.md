# Bug 0307: Frozen Maps Serialized in Wrong Key Order via Parameterized Queries

**Commit**: ffc4c89c3d · **Ticket**: CASSANDRA-17623
**Subsystem**: src/java/org/apache/cassandra/cql3/Maps.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`Maps.Value` stored its key-value pairs in a `LinkedHashMap`, which preserves insertion order rather than the type-defined sort order. When a client supplied map entries via a parameterized query, the driver passed them in whatever order the application inserted them. Cassandra then serialized the map bytes in that insertion order. Because frozen maps used as clustering keys are compared byte-for-byte, two insertions of the same logical map with keys in different orders produced distinct byte representations, so equality comparisons and lookups failed.

The fix changed `Maps.Value` to hold a `SortedMap<ByteBuffer, ByteBuffer>` backed by a `TreeMap` constructed with the key type's comparator, so entries are always ordered by Cassandra's type ordering regardless of the order in which the client supplied them.

### Before / After

```java
// BEFORE – insertion order preserved, not type order
public final Map<ByteBuffer, ByteBuffer> map;
public Value(Map<ByteBuffer, ByteBuffer> map) { this.map = map; }

// fromSerialized reconstructed via LinkedHashMap
Map<ByteBuffer, ByteBuffer> map = new LinkedHashMap<>(m.size());
```

```java
// AFTER – entries always stored in comparator order
public final SortedMap<ByteBuffer, ByteBuffer> map;
public Value(SortedMap<ByteBuffer, ByteBuffer> map) { this.map = map; }

// fromSerialized reconstructed via TreeMap keyed by the type comparator
SortedMap<ByteBuffer, ByteBuffer> map = new TreeMap<>(type.getKeysType());
```

---

## How to Spot

Any code path that builds a collection of serialized `ByteBuffer` pairs from user-supplied data and relies on the resulting map to be byte-comparable must use a sorted collection keyed by the Cassandra type comparator, not `LinkedHashMap` or `HashMap`. During review, confirm that any `Map<ByteBuffer, ByteBuffer>` used for clustering-key or frozen-collection serialization is a `SortedMap` whose comparator matches the declared CQL type, not Java's default hash or insertion ordering.

---

## Test

`CollectionsTest.testInsertingMapDataWithParameterizedQueriesIsKeyOrderIndependent` inserts two rows using `linkedHashMap` with keys supplied in opposite orders, flushes, then asserts that both rows are returned by filter queries regardless of key insertion order.
