# Bug 2389: assert on empty SSTable list crashes streaming's skeleton compaction controller

**Commit**: b4af12b43b · **Ticket**: CASSANDRA-4080

## Detection Rule

When a method has a hard `assert !collection.isEmpty()` that is valid for one caller but violated by a second caller with legitimately different semantics, replace the assertion with an early-return guard.

## Code Smell

A `public` method opens with `assert !sstables.isEmpty()` — assertions can be disabled at runtime and this call site is reached with an empty list by the streaming compaction controller path.

## Tags

- scope: cassandra
- category: missing-null-check
- code-pattern: assert-not-empty-public-method
- severity: crash
- subsystem: streaming

## Root Cause

`ColumnFamilyStore.getOverlappingSSTables` assumed it would only be called with a non-empty set of SSTables and enforced this with an assertion. However, the streaming path creates a "skeleton" `CompactionController` that passes an empty list. The assertion either fires (if assertions are enabled) or the code proceeds to build an interval tree over an empty set and returns a meaningless result. The fix replaces the assertion with a conditional that returns an empty set for an empty input.

## Example

```java
// BEFORE
public Set<SSTableReader> getOverlappingSSTables(Collection<SSTableReader> sstables)
{
    assert !sstables.isEmpty();  // crashes when streaming passes empty list
    IntervalTree<SSTableReader> tree = data.getView().intervalTree;
    ...
}
```

```java
// AFTER
public Set<SSTableReader> getOverlappingSSTables(Collection<SSTableReader> sstables)
{
    if (sstables.isEmpty())
        return ImmutableSet.of();  // streaming skeleton controller passes empty list legitimately
    IntervalTree<SSTableReader> tree = data.getView().intervalTree;
    ...
}
```
