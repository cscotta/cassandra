# Bug 1414: Fix Non-Text Comparator Column Names in CQL3 Queries

**Commit**: 11e657189a · **Ticket**: CASSANDRA-8178
**Subsystem**: src/java/org/apache/cassandra/cql3/ColumnIdentifier.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

Thrift-created tables can use a non-UTF-8 column comparator (e.g., `IntegerType`, `LongType`). When CQL3 parsed a column name in such a table, `ColumnIdentifier` was always constructed by treating the name as plain text, producing a `ByteBuffer` via `ByteBufferUtil.bytes(text)` (i.e., a raw UTF-8 encoding). This did not match how the actual stored column keys were encoded by the native comparator, so queries would either fail to find the column or silently return wrong results. The fix introduces a two-phase `ColumnIdentifier.Raw` that defers `ByteBuffer` computation until `CFMetaData` is available, at which point the comparator's `fromString()` is used to produce the correct binary representation.

### Before / After

```java
// BEFORE — always UTF-8 bytes regardless of comparator
public ColumnIdentifier(String rawString, boolean keepCase) {
    this.text = keepCase ? rawString : rawString.toLowerCase(Locale.US);
    this.key = ByteBufferUtil.bytes(this.text);
}
```

```java
// AFTER — Raw defers encoding until CFMetaData is known
public ColumnIdentifier prepare(CFMetaData cfm) {
    if (cfm.getIsDense() || cfm.comparator instanceof CompositeType
            || cfm.comparator instanceof UTF8Type)
        return new ColumnIdentifier(text, true);
    // Non-text comparator: use comparator.fromString() for the ByteBuffer
    return new ColumnIdentifier(cfm.comparator.fromString(rawText), text);
}
```

---

## How to Spot

Any code that converts a user-supplied string into a storage-layer key before the table's comparator type is known is a candidate for this bug. Review `ColumnIdentifier` construction sites in the parser/grammar layer and check whether the `CFMetaData` (and therefore comparator) is available at the point of construction. If not, the encoding is almost certainly wrong for non-UTF-8 comparator tables.

---

## Test

`src/test/unit/org/apache/cassandra/cql3/ThriftCompatibilityTest.java` — covers CQL3 queries against Thrift-created tables with non-text comparators to verify correct column lookup.
