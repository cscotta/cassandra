# Bug 0505: Commit Log Replay Fails When Static Column Has Collection Clustering Key

**Commit**: ae326eed2a · **Ticket**: CASSANDRA-14365
**Subsystem**: `src/java/org/apache/cassandra/serializers`
**Category**: Missing null / bounds check

---

## Root Cause

During commit log replay, a static column mutation carries an empty (zero-byte) clustering key component for each collection column in the primary key. The `validateForNativeProtocol` methods in `MapSerializer` and `SetSerializer` did not guard against a zero-remaining-bytes buffer before attempting to read the collection size header, causing a `BufferUnderflowException` that aborted replay for the affected mutation. The correct behavior is to treat an empty buffer as an empty collection and return immediately, consistent with how the data model represents "no value" for that component.

### Before / After

```java
// BEFORE (MapSerializer / SetSerializer)
public void validateForNativeProtocol(ByteBuffer bytes, ProtocolVersion version) {
    try {
        ByteBuffer input = bytes.duplicate();
        int n = readCollectionSize(input, version); // throws on empty buffer
        ...
    } catch (BufferUnderflowException e) {
        throw new MarshalException("Not enough bytes to read a set"); // wrong type too
    }
}
```

```java
// AFTER
public void validateForNativeProtocol(ByteBuffer bytes, ProtocolVersion version) {
    try {
        if (bytes.remaining() == 0) {
            return; // empty clustering component is valid for static rows
        }
        ByteBuffer input = bytes.duplicate();
        int n = readCollectionSize(input, version);
        ...
    } catch (BufferUnderflowException e) {
        throw new MarshalException("Not enough bytes to read a map");
    }
}
```

---

## How to Spot

Serializer `validate` methods that operate on `ByteBuffer` inputs should always start with a bounds check on `remaining()` before parsing a length prefix. An absent check is especially risky in code paths reachable from commit log replay, where partially constructed or empty column values are a normal occurrence for static columns whose clustering components are themselves collections.

---

## Test

`CommitLogTest#testRecoveryWithCollectionClusteringKeysStatic` creates a table with frozen `map<text,text>` and `set<text>` clustering columns and a static `int` column, writes a mutation with empty clustering key components (as replay would present), and asserts that replay completes without error (replayed segment count equals 1).
