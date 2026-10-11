# Bug 0049: Accord Journal Re-serializes Identical Topologies

**Commit**: 1350082a13 · **Ticket**: CASSANDRA-20721
**Subsystem**: src/java/org/apache/cassandra/service/accord/journal
**Category**: Wrong serialization / deserialization

---

## Root Cause

During journal compaction and replay, the `TopologyImage` accumulator always serialized the full topology update for every epoch even when consecutive epochs had equivalent topology (e.g., epochs triggered by schema-only changes). There was only one `Kind` variant for image records (`Topologies`), with no way to encode "same as previous." On deserialization the missing `NoOp` variant was not recognized, so replaying a compacted segment that skipped equivalent entries would silently omit them, corrupting the topology history. The fix introduced a `NoOp` kind that encodes only the lightweight `syncStatus`/`closed`/`retired` fields and reconstructs the full update from the preceding entry on read.

### Before / After

```java
// BEFORE – reserialize always emits the full TopologyImage
for (TopologyImage value : from.get().values())
    Serializer.instance.serialize(value, out);

// and only one variant existed:
enum Kind { NewTopology, Topologies }
```

```java
// AFTER – equivalent consecutive topologies are compacted to NoOp
Journal.TopologyUpdate prev = null;
for (TopologyImage value : from.get().values()) {
    Journal.TopologyUpdate tmp = value.update;
    if (prev != null && value.update.isEquivalent(prev))
        value = value.asNoOp();
    prev = tmp;
    Serializer.instance.serialize(value, out);
}

// and three variants exist:
enum Kind { NewTopology, TopologyImage, NoOp }
```

---

## How to Spot

When a compaction or replay path accumulates records and then re-emits them, check whether any optimization elides content that the deserializer still expects to be present. Here the deserializer consumed entries in sequence, so a `NoOp` entry must carry enough context (or a back-reference) to reconstruct itself; failing to do so silently drops data. Review compaction `reserialize` / deserialization symmetry whenever a new enum variant or record kind is added.

---

## Test

`IdenticalTopologyTest.identicalTopologyTest` runs a single-node cluster, applies 100 schema-only alterations (which produce equivalent topologies), forces segment rotation and compaction, then restarts the node and verifies it comes up cleanly.
