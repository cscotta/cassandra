# Bug 0567: Missing Rows When Reading 2.1 SSTables with Static Columns in 3.0

**Commit**: 9eee7aa787 · **Ticket**: CASSANDRA-14873
**Subsystem**: src/java/org/apache/cassandra/db
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`Serializers.indexEntryType` decoded the clustering key from a 2.1 (format `ka`) SSTable index entry but did not check for a composite-type static-row marker before attempting to decode a regular clustering. In the legacy format, a static row is encoded with a special composite type prefix (`CompositeType.isStaticName`). Without this check, when a reverse scan encountered the static-row index entry, the decoder interpreted it as a regular clustering, produced a malformed key, and the iteration logic concluded that no more regular rows existed in the index block, silently dropping all regular rows in that block.

### Before / After

```java
// BEFORE
if (clusteringSize == 0)
    return Clustering.EMPTY;

if (!metadata.isCompound())
    return new Clustering(bb);
```

```java
// AFTER
if (clusteringSize == 0)
    return Clustering.EMPTY;

if (metadata.isCompound() && CompositeType.isStaticName(bb))
    return Clustering.STATIC_CLUSTERING;

if (!metadata.isCompound())
    return new Clustering(bb);
```

---

## How to Spot

Legacy SSTable deserialization code that branches on `isCompound()` without also branching on `isStaticName()` is incomplete. In Cassandra 2.x compound types, the static-row sentinel is encoded inline in the byte buffer and must be detected before the buffer is interpreted as a regular clustering. Any path that reads a composite-type index entry should check `CompositeType.isStaticName(bb)` first; omitting it makes reverse scans silently skip rows in partitions that have both a static row and enough regular rows to span multiple index blocks.

---

## Test

`LegacySSTableTest.test14873` loads a 2.1 SSTable with a static column and five regular rows, then asserts that both a forward and a reverse range scan return exactly five rows.
