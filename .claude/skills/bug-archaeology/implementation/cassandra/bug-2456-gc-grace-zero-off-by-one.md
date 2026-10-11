# Bug 2456: Off-by-one in tombstone boundary check allows premature column expiry when gc_grace=0

**Commit**: 89e54091fd · **Ticket**: CASSANDRA-3579

## Detection Rule

When comparing a local deletion time against `gcBefore` (which is `now - gc_grace`), verify whether the boundary should be strict (`<`) or non-strict (`<=`): at gc_grace=0, `gcBefore == now`, and `localDeletionTime == gcBefore` means the tombstone is exactly at the boundary and should not yet be collected.

## Code Smell

`localDeletionTime <= gcBefore` (or `getLocalDeletionTime() <= gcBefore`) used as the condition to discard a tombstone; when gc_grace=0 this prematurely removes tombstones that were just written in the current second.

## Tags

- scope: cassandra
- category: off-by-one
- code-pattern: boundary-comparison-wrong-direction
- severity: data-loss
- subsystem: compaction

## Root Cause

Multiple tombstone/expiry checks used `<=` when comparing `localDeletionTime` to `gcBefore`. When `gc_grace_seconds=0`, `gcBefore = now`, so a tombstone created this second has `localDeletionTime == gcBefore`. The `<=` check discarded it immediately during the same compaction, which could cause data to reappear from non-tombstoned replicas (assertion error or incorrect resurrection). Changing to strict `<` ensures that a tombstone is only collected once it is strictly older than the gc boundary.

## Example

```java
// BEFORE – discards tombstone at the exact boundary
if (c.isMarkedForDelete() && c.getLocalDeletionTime() <= gcBefore)
    iter.remove();
if (cf.getColumnCount() == 0 && cf.getLocalDeletionTime() <= gcBefore)
    return null;

// AFTER – keeps tombstone when localDeletionTime == gcBefore
if (c.isMarkedForDelete() && c.getLocalDeletionTime() < gcBefore)
    iter.remove();
if (cf.getColumnCount() == 0 && cf.getLocalDeletionTime() < gcBefore)
    return null;
```
