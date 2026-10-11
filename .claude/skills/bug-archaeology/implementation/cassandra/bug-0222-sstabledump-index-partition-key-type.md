# Bug 0222: SSTableDump Fails on Secondary Index SSTables Due to Missing Partition Key Type

**Commit**: cc3e742c73 · **Ticket**: CASSANDRA-17698
**Subsystem**: tools (SSTableExport/JsonTransformer), db/marshal (PartitionerDefinedOrder, TypeParser), index/internal (CassandraIndex)
**Category**: Wrong serialization / deserialization

---

## Root Cause

Secondary index SSTables store their partition key in a partitioner-defined order, and the clustering column for the index table is typed as `PartitionerDefinedOrder`. When `sstabledump` serialized the clustering column value to JSON it called `PartitionerDefinedOrder.toJSONString()`, which unconditionally threw `UnsupportedOperationException` because `PartitionerDefinedOrder` had no knowledge of the underlying base-table partition key type needed to render the bytes as JSON.

The fix threads the partition key type through the index table construction: `CassandraIndex` now passes `baseCfsMetadata.partitionKeyType` to a new `partitionOrdering(AbstractType<?>)` overload on `IPartitioner`, and `PartitionerDefinedOrder` stores the type and delegates `toJSONString()` to it. `TypeParser` was also extended to handle the new serialized form `PartitionerDefinedOrder(<partitioner>:<baseType>)` while remaining backward compatible with the old form.

### Before / After

```java
// BEFORE – PartitionerDefinedOrder always threw on toJSONString
@Override
public String toJSONString(ByteBuffer buffer, ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
}

// BEFORE – index table clustering column had no partition key type context
.addClusteringColumn("partition_key", baseCfsMetadata.partitioner.partitionOrdering());
```

```java
// AFTER – PartitionerDefinedOrder delegates to the stored partition key type
@Override
public String toJSONString(ByteBuffer buffer, ProtocolVersion protocolVersion) {
    assert partitionKeyType != null : "PartitionerDefinedOrder's toJSONString method needs a partition key type";
    return partitionKeyType.toJSONString(buffer, protocolVersion);
}

// AFTER – index table clustering column carries the partition key type
AbstractType<?> indexedTablePartitionKeyType = baseCfsMetadata.partitioner.partitionOrdering(baseCfsMetadata.partitionKeyType);
.addClusteringColumn("partition_key", isCompatible ? baseCfsMetadata.partitioner.partitionOrdering()
                                                   : indexedTablePartitionKeyType);
```

---

## How to Spot

Any `AbstractType` subclass that represents a composite or indirect ordering and throws `UnsupportedOperationException` from `toJSONString()` will cause `sstabledump` to fail on any SSTable that uses that type as a column type. When reviewing new type wrappers, verify that all serialization entry points (toJSONString, getString, compose/decompose) are either properly implemented or forward to a delegate; stubs that throw are only safe if the type is guaranteed never to appear in a serialized position (partition key, clustering column, value) that the tools layer can visit.

---

## Test

`SecondaryIndexSSTableExportTest` creates tables with secondary indexes, flushes them, and asserts that `sstabledump` can successfully export the index SSTable to JSON for various index types (regular column, partition key, clustering column, collection).
