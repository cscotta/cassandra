# Bug 0048: CRC Errors in Journal due to Missing fsyncLimit Field

**Commit**: b506665478 · **Ticket**: CASSANDRA-20661
**Subsystem**: src/java/org/apache/cassandra/journal
**Category**: Wrong serialization / deserialization

---

## Root Cause

The `Metadata` class persisted to the Journal omitted the `fsyncLimit` field from its CRC checksum calculation and serialized form. As a result, the stored CRC covered only `recordsCount`, while the on-disk layout already contained (or was expected to contain) the `fsyncLimit` integer, causing a mismatch when the metadata was read back and the CRC was re-validated. Additionally, `EntrySerializer` computed `totalSize` as `getInt(start) - start` instead of simply reading the end-offset value, which would yield a negative total size when `start > 0`, leading to corrupted reads or spurious CRC errors.

### Before / After

```java
// BEFORE
out.writeInt(recordsCount);
updateChecksumInt(crc, recordsCount);
out.writeInt((int) crc.getValue());

// and in deserialization:
int totalSize = from.getInt(start) - start;  // wrong: subtracts current position
```

```java
// AFTER
out.writeInt(recordsCount);
out.writeInt(fsyncLimit);
updateChecksumInt(crc, recordsCount);
updateChecksumInt(crc, fsyncLimit);
out.writeInt((int) crc.getValue());

// and in deserialization:
int endOffset = from.getInt(start);
int totalSize = endOffset - start;  // correct: end offset minus start
```

---

## How to Spot

When a CRC covers only a subset of the fields that are actually serialized, any new field added to the layout will silently invalidate old checksums. During code review, verify that every field written between the CRC seed and the CRC write call appears in exactly one `updateChecksumInt`/`updateChecksumLong` call, and cross-check the serializer's field list against the deserializer's field list to ensure they match.

---

## Test

`MetadataTest.testWriteRead` verifies round-trip serialization of `Metadata`, ensuring the CRC matches after adding the `fsyncLimit` field.
