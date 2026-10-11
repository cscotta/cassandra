# Bug 2971: SuperColumn.serializedSize() reports wrong size — includes extra int, uses wrong field sizes

**Commit**: 939709f977 · **Ticket**: CASSANDRA-341

## Detection Rule
When a `serializedSize()` method is kept in sync manually with the actual serialization code, check whether all fields written by the serializer match the sizes used in the calculation — a mismatch causes stream corruption or buffer overflows during streaming and compaction.

## Code Smell
`SuperColumn.serializedSize()` included `DBConstants.boolSize_` (delete flag) and two `intSize_` fields, but the actual `SuperColumnSerializer.serialize()` writes a `long` (deletion time) and only one `int` (column count); the old size formula was left over from a prior serialization format.

## Tags
- scope: cassandra
- category: wrong-serialization
- code-pattern: serialized-size-out-of-sync
- severity: data-loss
- subsystem: storage-engine

## Root Cause
A previous refactoring changed `SuperColumn` serialization from writing a `boolean` delete flag plus two `int` fields to writing a `long` (marked-for-delete timestamp) plus one `int` (column count). The `serializedSize()` method was not updated to match, so it reported a size that included `boolSize_ + intSize_ + intSize_` instead of `intSize_ + longSize_`. Any consumer that used `serializedSize()` for buffer allocation or stream framing (such as the SSTable writer or streaming code) would produce incorrectly sized output, potentially corrupting data.

## Example
```java
// BEFORE: wrong fields — bool + 2 ints (old format)
return IColumn.UtfPrefix_ + name_.length
    + DBConstants.boolSize_    // wrong: no bool is written
    + DBConstants.intSize_     // extra int not written
    + DBConstants.intSize_
    + getSizeOfAllColumns();
```
```java
// AFTER: matches actual serialization — int + long
return IColumn.UtfPrefix_ + name_.length
    + DBConstants.intSize_     // column count
    + DBConstants.longSize_    // markedForDeleteAt
    + getSizeOfAllColumns();
```
