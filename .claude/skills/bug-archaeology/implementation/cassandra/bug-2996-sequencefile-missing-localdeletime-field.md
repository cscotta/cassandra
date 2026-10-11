# Bug 2996: SequenceFile omits localDeletionTime field when reading/writing CF records

**Commit**: 6be087a80a · **Ticket**: CASSANDRA-33

## Detection Rule
When deserializing a record that contains a `localDeletionTime` int field followed by a `markedForDeleteAt` long field, read and re-emit the `localDeletionTime` field explicitly; omitting it causes all subsequent field reads to consume wrong bytes.

## Code Smell
After reading the CF name from a sequence file, `file_.readLong()` is called for `markedForDeleteAt` without first calling `file_.readInt()` for `localDeletionTime`, so the 4-byte int field is consumed as the first 4 bytes of the long, corrupting every subsequent read.

## Tags
- scope: cassandra
- category: wrong-serialization
- code-pattern: missing-field-in-deserialization
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
When CF serialization gained a `localDeletionTime` int field (4 bytes), the SequenceFile read-path was not updated to consume it. Reads would interpret the first 4 bytes of `localDeletionTime` together with the first 4 bytes of `markedForDeleteAt` as a single long, producing garbage tombstone timestamps and misaligning all following column reads. The fix inserts `int localDeletionTime = file_.readInt(); dataSize -= 4;` at both affected read sites, and mirrors it in the write-path with `bufOut.writeInt(localDeletionTime)`.

## Example
```java
// BEFORE: localDeletionTime not read — bytes consumed by markedForDeleteAt
String cfName = file_.readUTF();
dataSize -= (utfPrefix_ + cfName.length());
long markedForDeleteAt = file_.readLong(); // reads wrong bytes!

// AFTER: consume localDeletionTime first
String cfName = file_.readUTF();
dataSize -= (utfPrefix_ + cfName.length());
int localDeletionTime = file_.readInt();
dataSize -= 4;
long markedForDeleteAt = file_.readLong();
```
