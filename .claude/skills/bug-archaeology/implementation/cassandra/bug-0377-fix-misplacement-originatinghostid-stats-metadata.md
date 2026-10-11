# Bug 0377: Misplacement of originatingHostId in Stats Metadata Serialization

**Commit**: d35f36cd05 · **Ticket**: CASSANDRA-16619
**Subsystem**: src/java/org/apache/cassandra/io/sstable/metadata
**Category**: Wrong serialization / deserialization

---

## Root Cause

In `StatsMetadata.Serializer`, the `originatingHostId` field was serialized and deserialized at the wrong position relative to `pendingRepair` and `isTransient`. Specifically, the `originatingHostId` block was placed before `pendingRepair` in both `serializedSize` and `serialize`, but `pendingRepair` was actually serialized in the `originatingHostId` block's code (copy-paste error). Deserialization read `originatingHostId` before `pendingRepair`, causing field values to be read from misaligned byte positions, silently corrupting both fields.

### Before / After

```java
// BEFORE (wrong order — originatingHostId written before pendingRepair)
if (version.hasOriginatingHostId()) { /* writes originatingHostId */ }
if (version.hasPendingRepair())     { /* writes pendingRepair */ }
if (version.hasIsTransient())       { out.writeBoolean(component.isTransient); }
```

```java
// AFTER (correct order matching on-disk layout)
if (version.hasPendingRepair())     { /* writes pendingRepair */ }
if (version.hasIsTransient())       { out.writeBoolean(component.isTransient); }
if (version.hasOriginatingHostId()) { /* writes originatingHostId */ }
```

---

## How to Spot

When reviewing serialization code that writes multiple optional fields guarded by version checks, verify that the write order in `serialize`, the read order in `deserialize`, and the summation order in `serializedSize` are all identical. Any permutation mismatch silently produces byte-misaligned reads that corrupt every field from the first mis-ordered one onward.

---

## Test

`MetadataSerializerTest` was extended with round-trip assertions for `originatingHostId` to ensure the field survives serialize/deserialize at the correct byte offset.
