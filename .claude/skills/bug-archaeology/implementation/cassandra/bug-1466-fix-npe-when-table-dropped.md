# Bug 1466: Fix NPE When Table Dropped During Streaming

**Commit**: d143487cb1 · **Ticket**: CASSANDRA-7946
**Subsystem**: src/java/org/apache/cassandra/streaming/
**Category**: Missing null / bounds check

---

## Root Cause

`StreamReader.read()` and `CompressedStreamReader.read()` called `Schema.instance.getCF(cfId)` to look up the keyspace/table names, then immediately dereferenced the result without a null check. If the table was dropped while a streaming session was already in progress, `getCF` returned `null`, and the subsequent call to `Keyspace.open(kscf.left)` threw a NullPointerException. Similarly, `StreamReceiveTask`'s finalizer thread tried to open the column family store from a null pair when flushing received SSTables.

### Before / After

```java
// BEFORE
Pair<String, String> kscf = Schema.instance.getCF(cfId);
ColumnFamilyStore cfs = Keyspace.open(kscf.left).getColumnFamilyStore(kscf.right); // NPE if kscf == null
```

```java
// AFTER
Pair<String, String> kscf = Schema.instance.getCF(cfId);
if (kscf == null)
{
    // schema was dropped during streaming
    throw new IOException("CF " + cfId + " was dropped during streaming");
}
ColumnFamilyStore cfs = Keyspace.open(kscf.left).getColumnFamilyStore(kscf.right);
```

---

## How to Spot

Any lookup that can return null based on concurrent schema changes must be null-checked before dereferencing. Streaming, repair, and migration code all race against schema operations and must handle the case where the looked-up entity has been concurrently removed.

---

## Test

No test added.
