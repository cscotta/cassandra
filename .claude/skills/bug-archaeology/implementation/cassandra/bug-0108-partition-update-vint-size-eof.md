# Bug 0108: PartitionUpdate.isEmpty Uses Wrong VInt Size Computation, Causing EOFException

**Commit**: 369daf56bc · **Ticket**: CASSANDRA-20345
**Subsystem**: db/partitions, messaging
**Category**: Wrong serialization / deserialization

---

## Root Cause

`PartitionUpdate.SimplePartitionData.isEmpty` deserializes a `PartitionUpdate` from a `ByteBuffer` in order to check whether it is empty, without consuming the buffer. For messaging version 5.1+, the code reads an epoch field encoded as an unsigned VInt and advances a position counter by `VIntCoding.computeVIntSize(epoch)`. However, the epoch is encoded with `VIntCoding.writeUnsignedVInt`, so its size must be computed with `computeUnsignedVIntSize`, not `computeVIntSize`. The two functions use different bit-width encodings: for values ≥ 128 the signed variant returns a size that is too small, causing subsequent field reads to begin at the wrong offset and eventually hit an `EOFException` when deserializing a Paxos commit stored in the system table on restart.

### Before / After

```java
// BEFORE — wrong size function for unsigned VInt
long epoch = VIntCoding.getUnsignedVInt(in, position);
position += VIntCoding.computeVIntSize(epoch);   // signed size, under-counts for large epochs
```

```java
// AFTER
long epoch = VIntCoding.getUnsignedVInt(in, position);
position += VIntCoding.computeUnsignedVIntSize(epoch);
```

---

## How to Spot

When a codec uses a paired read/write API with both signed and unsigned variants (`writeVInt`/`writeUnsignedVInt`, `readVInt`/`readUnsignedVInt`, `computeVIntSize`/`computeUnsignedVIntSize`), every call site must use the matching variant throughout. Review each position-advance expression and confirm that the size function matches the write function used during serialization. Mismatches are silent until an epoch value crosses a threshold (e.g., ≥ 128 requires an extra byte in unsigned encoding), at which point any deserialization that relies on the computed offset will start reading from the wrong position.

---

## Test

`PaxosRepairTest.epochBadDeserializationTest` creates a 3-node cluster, bumps the epoch past the boundary where the size mismatch matters, drops a Paxos commit, and restarts node 1 to verify it can deserialize the stored commit without an `EOFException`.
