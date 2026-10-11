# Bug 0676: Hints Reader Returns Early on UnknownColumnFamily, Skipping CRC Validation

**Commit**: f919cf4a47 · **Ticket**: CASSANDRA-13696
**Subsystem**: `src/java/org/apache/cassandra/hints/HintsReader.java`, `src/java/org/apache/cassandra/hints/HintsDescriptor.java`
**Category**: Logic error in condition

---

## Root Cause

When `HintsReader` encountered a hint for a dropped table (`UnknownColumnFamilyException`), it skipped the hint's bytes and then immediately `return null`-ed without executing the trailing CRC check. On the next call, the reader was positioned after the skipped hint's bytes but before the CRC bytes, so it would try to deserialize the CRC bytes as the next hint — producing a digest mismatch exception on valid hints data. The fix changes `return null` to `hint = null`, allowing execution to fall through to the existing `input.checkCrc()` call.

A second related fix corrected `HintsDescriptor.messagingVersion()`: for `VERSION_30` hints files it always returned `VERSION_30`, but should return `VERSION_3014` unless the legacy protocol is forced, so that deserialization uses the correct serializer version.

### Before / After

```java
// BEFORE – skips bytes then returns, never checking CRC
input.skipBytes(Ints.checkedCast(size - input.bytesPastLimit()));
return null;
```

```java
// AFTER – sets return value, falls through to CRC check
hint = null; // set the return value to null and let following code update/check the CRC
// ... falls through to: if (input.checkCrc()) ...
```

---

## How to Spot

Whenever an error-handling path skips content from a checksummed stream, it must still advance past and verify the checksum. Any early `return` inserted inside a read loop that is guarded by a CRC suffix will silently desynchronise the reader. Reviewers should trace every exception/skip path in checksummed readers to confirm that the checksum bytes are always consumed.

---

## Test

`HintsReaderTest.testDroppedTableRead()` writes hints for two tables, drops one of them, and asserts that reading the hints file still returns the correct number of valid hints for the remaining table.
