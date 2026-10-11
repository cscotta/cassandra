# Bug 1323: Rare NPE in KeyCacheSerializer

**Commit**: bef1d0cb06 · **Ticket**: CASSANDRA-8067
**Subsystem**: service/CacheService
**Category**: Missing null / bounds check

---

## Root Cause

`KeyCacheSerializer.serialize()` looked up `CFMetaData` by keyspace and table name extracted from the SSTable descriptor after it had already confirmed the cache entry exists. Between a node restart (which replays the key cache) and the moment the serializer runs, a table can be dropped, causing `Schema.instance.getCFMetaData(ksname, cfname)` to return `null`. The code then passed that null directly to `cfm.comparator.rowIndexEntrySerializer()`, producing a NullPointerException. The fix moves the metadata lookup earlier and returns immediately when the table no longer exists, treating a missing table as a valid transient condition rather than an error.

### Before / After

```java
// BEFORE
ByteBufferUtil.writeWithLength(key.key, out);
Descriptor desc = key.desc;
out.writeInt(desc.generation);
out.writeBoolean(true);
CFMetaData cfm = Schema.instance.getCFMetaData(key.desc.ksname, key.desc.cfname);
cfm.comparator.rowIndexEntrySerializer().serialize(entry, out); // NPE if cfm == null
```

```java
// AFTER
CFMetaData cfm = Schema.instance.getCFMetaData(key.cfId);
if (cfm == null)
    return; // the table no longer exists.

ByteBufferUtil.writeWithLength(key.key, out);
out.writeInt(key.desc.generation);
out.writeBoolean(true);
cfm.comparator.rowIndexEntrySerializer().serialize(entry, out);
```

---

## How to Spot

Any code that retrieves schema metadata (table, keyspace, type definitions) and then immediately dereferences the result without a null guard is vulnerable to this pattern. Schema lookups by name are especially fragile because table drops are not coordinated with background tasks such as cache serialization or compaction. The fix pattern — lookup early, guard for null, and return cleanly — should be applied consistently whenever schema metadata is required for a non-critical background operation.

---

## Test

No test added.
