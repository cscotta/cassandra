# Bug 1300: Fix Merkle Tree Mismatch Between Empty and GC-able Data

**Commit**: e2ad8969cc · **Ticket**: CASSANDRA-8979
**Subsystem**: db/compaction/LazilyCompactedRow.java, db/compaction/PrecompactedRow.java, repair/Validator.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

During repair, the Merkle tree digest for a row was always updated with the serialized top-level deletion time, even when that deletion was `DeletionTime.LIVE` (i.e. no real tombstone exists). A row that had previously held a tombstone which was then GC-eligible would serialize a `LIVE` deletion time and still feed it into the digest, whereas a node that never saw the row would contribute nothing to the digest. This produced a hash mismatch between nodes for data that was semantically identical after GC, causing unnecessary repair work and potentially incorrect streaming. The fix skips the digest update when the top-level deletion is `LIVE`, and the `Validator` ignores `null` row hashes returned by `rowHash()`.

### Before / After

```java
// BEFORE — always updates digest, even for LIVE (no-op) tombstones
DeletionTime.serializer.serialize(emptyColumnFamily.deletionInfo().getTopLevelDeletion(), out);
digest.update(out.getData(), 0, out.getLength());
```

```java
// AFTER — skips digest update for LIVE deletion
DeletionTime.serializer.serialize(emptyColumnFamily.deletionInfo().getTopLevelDeletion(), out);
if (emptyColumnFamily.deletionInfo().getTopLevelDeletion() != DeletionTime.LIVE)
{
    digest.update(out.getData(), 0, out.getLength());
}
```

---

## How to Spot

Whenever a Merkle-tree or checksum computation serializes data that has a "no-op" sentinel value (zero, `LIVE`, `null`), ask whether including that sentinel in the digest is equivalent to excluding it. If two nodes can legally hold different representations of the same logical state (e.g. a purged tombstone vs. a row that was never written), their digests must agree — meaning no-op values must either be excluded from the hash or normalized before hashing.

---

## Test

`ValidatorTest.testPurgedVsNonExisting` is added: it constructs one validator with zero rows and another with a single row backed by a null column family (simulating a fully-purged tombstone), then asserts that both Merkle trees produce zero differences.
