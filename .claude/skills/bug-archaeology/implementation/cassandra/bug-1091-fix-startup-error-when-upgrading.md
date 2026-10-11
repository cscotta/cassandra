# Bug 1091: Fix Startup Error When Upgrading Nodes to 3.0

**Commit**: f009272ba6 · **Ticket**: CASSANDRA-10136
**Subsystem**: db/Serializers.java, db/columniterator/AbstractSSTableIterator.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

When a node upgraded to 3.0 started reading pre-3.0 (non-`storeRows`) SSTable index entries, `Serializers.clusteringPrefixSerializer` threw `UnsupportedOperationException` for old-format versions instead of providing a deserializer that understood the legacy composite-name encoding. Additionally, the reversed SSTable iterator failed to account for the fact that a single row in the old format can span two consecutive index blocks: when iterating blocks in reverse, the iterator would read the tail of such a row from block N and then read the head again when processing block N-1, producing a duplicate. The fix adds a full legacy deserializer that parses composite cell names into `Clustering` or `Slice.Bound` objects, and adds an `includeFirst` flag that skips the first result of a block when it is identified as a continuation of the previous block's last row.

### Before / After

```java
// BEFORE
public ISerializer<ClusteringPrefix> clusteringPrefixSerializer(final Version version, final SerializationHeader header)
{
    if (!version.storeRows())
        throw new UnsupportedOperationException();
    // ... new-format serializer
}
```

```java
// AFTER
public ISerializer<ClusteringPrefix> indexEntryClusteringPrefixSerializer(final Version version, final SerializationHeader header)
{
    if (!version.storeRows())
    {
        return new ISerializer<ClusteringPrefix>() {
            public ClusteringPrefix deserialize(DataInputPlus in) throws IOException {
                ByteBuffer bb = ByteBufferUtil.readWithShortLength(in);
                // parse composite name into Clustering or Slice.Bound
            }
            // serialize/serializedSize throw UnsupportedOperationException (read-only path)
        };
    }
    // ... new-format serializer
}
```

---

## How to Spot

Any legacy-format code path that throws `UnsupportedOperationException` on the deserialization side (rather than just the serialization side) is a latent upgrade crasher: during an upgrade the node must be able to *read* old data even if it will never *write* it. Whenever a version guard gates a serializer, verify that both the read and write directions are handled appropriately for each version.

---

## Test

No test added.
