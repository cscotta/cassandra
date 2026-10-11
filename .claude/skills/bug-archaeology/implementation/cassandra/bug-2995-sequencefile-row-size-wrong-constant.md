# Bug 2995: SequenceFile row-size header written with wrong byte count for CF serialization

**Commit**: 234d0895ad · **Ticket**: CASSANDRA-33

## Detection Rule
When writing a serialized size prefix for a column family record, account for every field actually serialized: CF name (with UTF prefix), localDeletionTime (4 bytes), markedForDeleteAt (8 bytes), and column count (4 bytes).

## Code Smell
A `bufOut.writeInt(dataSize + utfPrefix_ + cfName.length() + 4 + 1)` constant `1` is used where the actual payload includes a `long` (8 bytes) and an `int` (4 bytes), under-reporting the size by 11 bytes.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: serialized-size-constant-wrong
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
The "returned" size written into the output buffer for a column family record used the constant `4 + 1` (apparently an old schema where deletion was a single boolean byte) rather than the current `4 + 8 + 4` (localDeletionTime int + markedForDeleteAt long + column count int). The under-reported size meant callers read fewer bytes than were actually present, corrupting subsequent reads. The fix changes both call sites to `4 + 8 + 4 = 16`.

## Example
```java
// BEFORE: wrong constant — old 1-byte boolean schema
bufOut.writeInt(dataSize + utfPrefix_ + cfName.length() + 4 + 1);

// AFTER: correct size — int + long + int
bufOut.writeInt(dataSize + utfPrefix_ + cfName.length() + 4 + 8 + 4);
```
