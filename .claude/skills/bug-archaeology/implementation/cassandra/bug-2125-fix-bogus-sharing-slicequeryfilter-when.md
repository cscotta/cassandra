# Bug 2125: SliceQueryFilter Shared Across ReadCommands Causes Incorrect Column Counts With SuperColumns

**Commit**: 4495736227 · **Ticket**: CASSANDRA-5123
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java, src/java/org/apache/cassandra/db/filter/SliceQueryFilter.java, src/java/org/apache/cassandra/db/filter/NamesQueryFilter.java, src/java/org/apache/cassandra/db/filter/IDiskAtomFilter.java, src/java/org/apache/cassandra/thrift/CassandraServer.java
**Category**: State not cleaned up

---

## Root Cause

`SliceQueryFilter` maintains a mutable `columnCounter` field used by `lastCounted()` to track how many columns were scanned during a read. When processing a multi-key IN query, the original code created a single filter instance and shared it across all `ReadCommand` objects — one per key. As each command executed and updated the counter, subsequent commands started with polluted counter state, producing incorrect column counts especially when super-columns were involved. `NamesQueryFilter` was also shared but is effectively immutable with respect to this state, so it was safe; however the code was inconsistent. The fix introduces a `cloneShallow()` method on `IDiskAtomFilter` and its implementations so that each `ReadCommand` receives its own filter copy with a fresh counter.

### Before / After

```java
// BEFORE — SliceQueryFilter instance reused across all keys in IN clause
for (ByteBuffer key : keys) {
    QueryProcessor.validateKey(key);
    commands.add(new SliceFromReadCommand(keyspace(), key, columnFamily(), (SliceQueryFilter)makeFilter(variables)));
}
```

```java
// AFTER — each command gets a shallow clone with a fresh counter
IDiskAtomFilter filter = makeFilter(variables);
for (ByteBuffer key : keys) {
    QueryProcessor.validateKey(key);
    commands.add(ReadCommand.create(keyspace(), key, columnFamily(), filter.cloneShallow()));
}
```

---

## How to Spot

Any time a stateful object (one with mutable counters or cursors) is constructed once and then passed to multiple independent consumers in a loop, it is a sharing hazard. Look for filter/iterator objects created outside a loop body but consumed inside it — they nearly always need per-consumer copies.

---

## Test

No test added.
