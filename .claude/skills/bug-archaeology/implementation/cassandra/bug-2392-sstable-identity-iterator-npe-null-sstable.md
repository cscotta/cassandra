# Bug 2392: NPE in SSTableIdentityIterator when sstable is null but promoted-index check is unconditional

**Commit**: 65c33fac9c · **Ticket**: n/a

## Detection Rule

When a method documents that a parameter "may be null" under a certain condition, every dereference of that parameter must be guarded by a null check.

## Code Smell

A private constructor has a comment "sstable may be null if checkData is false" but the body contains `if (!sstable.descriptor.hasPromotedIndexes)` — an unconditional field access that NPEs when `sstable` is null.

## Tags

- scope: cassandra
- category: missing-null-check
- code-pattern: documented-nullable-unconditional-deref
- severity: crash
- subsystem: storage-engine

## Root Cause

`SSTableIdentityIterator`'s private constructor accepted a nullable `sstable` parameter and even documented this, but subsequently dereferenced `sstable.descriptor.hasPromotedIndexes` without a null guard. The public constructor that omits the SSTable (used for streaming current-format data) would hit this NPE. The fix wraps the dereference in `if (sstable != null && !sstable.descriptor.hasPromotedIndexes)`.

## Example

```java
// BEFORE — sstable may be null, but unconditional deref
if (!sstable.descriptor.hasPromotedIndexes)   // NPE when sstable == null
{
    IndexHelper.skipBloomFilter(inputWithTracker);
    IndexHelper.skipIndex(inputWithTracker);
}
```

```java
// AFTER
if (sstable != null && !sstable.descriptor.hasPromotedIndexes)
{
    IndexHelper.skipBloomFilter(inputWithTracker);
    IndexHelper.skipIndex(inputWithTracker);
}
```
