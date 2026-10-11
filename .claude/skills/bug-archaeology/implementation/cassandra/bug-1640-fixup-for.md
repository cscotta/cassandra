# Bug 1640: Fixup for CASSANDRA-6831 on 2.1

**Commit**: 706afc300d · **Ticket**: CASSANDRA-6831
**Subsystem**: src/java/org/apache/cassandra/config (CFMetaData, ColumnDefinition)
**Category**: Logic error in condition

---

## Root Cause

The original CASSANDRA-6831 fix for preserving CQL metadata across thrift `ALTER TABLE` updates was ported to the 2.1 branch with a structural mistake: `internalFromThrift()` received a fully-built `CFMetaData` object to copy CQL column definitions from, but was called before the column definitions were added, so the copy logic operated on incomplete data. Additionally, the dense/sparse classification of the comparator was computed before the full column definitions list was assembled, leading to incorrect comparator construction. The fixup refactors `internalFromThrift()` to accept the raw list of previous `ColumnDefinition`s and assembles everything — thrift columns, the key alias, and CQL-only metadata — before computing the comparator type.

### Before / After

```java
// BEFORE
private static CFMetaData internalFromThrift(org.apache.cassandra.thrift.CfDef cf_def)
// Called without existing CQL metadata; fromThriftForUpdate() merged metadata after the fact
boolean isDense = (cf_def.column_metadata == null || cf_def.column_metadata.isEmpty())
                  && !isCQL3OnlyPKComparator(rawComparator);
CellNameType comparator = CellNames.fromAbstractType(makeRawAbstractType(...), isDense);
```

```java
// AFTER
private static CFMetaData internalFromThrift(CfDef cf_def, Collection<ColumnDefinition> previousCQLMetadata)
// All defs (thrift REGULAR + keyAlias + previousCQLMetadata) assembled first
CellNameType comparator = CellNames.fromAbstractType(fullRawComparator, isDense(fullRawComparator, defs));
```

---

## How to Spot

When a refactor splits object construction across two phases (build skeleton, then copy metadata), check whether any derived values computed in the first phase depend on data only available after the second phase completes. If so, defer those computations until all inputs are present.

---

## Test

No new test added; existing `ColumnDefinitionTest.testSerializeDeserialize` was updated to use the new `fromThrift` signature.
