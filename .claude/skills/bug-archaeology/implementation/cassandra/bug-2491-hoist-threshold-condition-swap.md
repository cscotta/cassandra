# Bug 2491: Single-column read hoist triggers compaction incorrectly — condition operands swapped

**Commit**: 70eb357568 · **Ticket**: n/a (post-review editing goof)

## Detection Rule

When a guard condition checks `!disabledCondition && thresholdCondition` but the threshold check is evaluated first, the threshold comparison is always evaluated even when it would be irrelevant — and a logic swap can trigger expensive operations when the threshold is not met.

## Code Smell

`if (!cfs.isCompactionDisabled() && cfs.getMinimumCompactionThreshold() > 0 && ...)` where the "how many sstables were iterated" check (`sstablesIterated > threshold`) is missing, so the hoist happens on every single-column read regardless of how many SSTables were accessed.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: missing-threshold-operand-in-condition
- severity: performance
- subsystem: compaction

## Root Cause

After a post-review edit, the condition for "hoist up" single-column data was left with `cfs.getMinimumCompactionThreshold() > 0` instead of the correct `sstablesIterated > cfs.getMinimumCompactionThreshold()`. This meant the hoist-to-newer-sstable re-write happened on every single-partition read that touched any sstable, instead of only when many SSTables were accessed — causing unnecessary write amplification.

## Example

```java
// BEFORE (post-review editing goof)
if (!cfs.isCompactionDisabled()
    && cfs.getMinimumCompactionThreshold() > 0  // wrong: always true when threshold configured
    && cfs.getCompactionStrategy() instanceof SizeTieredCompactionStrategy)
```

```java
// AFTER
if (sstablesIterated > cfs.getMinimumCompactionThreshold()  // correct threshold check
    && !cfs.isCompactionDisabled()
    && cfs.getCompactionStrategy() instanceof SizeTieredCompactionStrategy)
```
