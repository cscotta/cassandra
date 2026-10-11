# Bug 1248: ColumnFamilySplit length Field Not Serialized in Hadoop InputSplit

**Commit**: 81b0acb2a9 · **Ticket**: CASSANDRA-9380
**Subsystem**: src/java/org/apache/cassandra/hadoop/ColumnFamilySplit.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`ColumnFamilySplit` implements the Hadoop `Writable` interface, which requires that `write()` and `readFields()` be symmetric. The `length` field was read in `readFields()` but was never written in `write()`, so the serialized form contained only endpoints and the `length` value would be garbage (or an `EOFException`) on deserialization. The fix writes `length` at the end of `write()` and reads it back with a backward-compatible `EOFException` catch to tolerate older serialized splits in a mixed-version cluster.

### Before / After

```java
// BEFORE
for (String endpoint : dataNodes)
    out.writeUTF(endpoint);
// length never written
```

```java
// AFTER
for (String endpoint : dataNodes)
    out.writeUTF(endpoint);
out.writeLong(length);   // field now serialized
```

---

## How to Spot

When a class implements a symmetric serialization contract (`Writable`, `Externalizable`, custom `readObject`/`writeObject`), verify that every field touched in the read method is also touched in the write method and vice versa. A quick structural check is to count the read calls and match them one-for-one against write calls in the same class; any asymmetry flags a missing field.

---

## Test

No test added.
