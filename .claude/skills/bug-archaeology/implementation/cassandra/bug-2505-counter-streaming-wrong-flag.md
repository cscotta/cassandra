# Bug 2505: Counter columns deserialized with wrong remote/local flag during streaming

**Commit**: e029b7d0c1 · **Ticket**: CASSANDRA-3481

## Detection Rule
When a serializer accepts a boolean `fromRemote` parameter, check that every call site passes the right value; boolean flags are easily confused and a wrong value silently corrupts data.

## Code Smell
`deserialize(dis, false, factory)` called on the streaming path — `false` means LOCAL but streaming data is FROM_REMOTE, causing counter delta clearing to be skipped and producing an incorrect size exception downstream.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: boolean-flag-wrong-value
- severity: crash
- subsystem: streaming

## Root Cause
The deserializer used a raw `boolean fromRemote` parameter. The single-argument `deserialize(DataInput)` overload hardcoded `false` (LOCAL), and callers on the streaming path used the wrong overload. This meant `CounterColumn.create()` was never told the data came from a remote node, so it skipped delta clearing only for negative shard counts (`count < 0`), missing cases that produce an incorrect serialized size exception during streaming.

## Example
```java
// BEFORE
public ColumnFamily deserialize(DataInput dis) throws IOException {
    return deserialize(dis, false, ThreadSafeSortedColumns.factory());
}
public ColumnFamily deserialize(DataInput dis, boolean fromRemote, ...) { ... }
```
```java
// AFTER
public ColumnFamily deserialize(DataInput dis) throws IOException {
    return deserialize(dis, IColumnSerializer.Flag.LOCAL,
                       ThreadSafeSortedColumns.factory());
}
public ColumnFamily deserialize(DataInput dis, IColumnSerializer.Flag flag, ...) { ... }
```
