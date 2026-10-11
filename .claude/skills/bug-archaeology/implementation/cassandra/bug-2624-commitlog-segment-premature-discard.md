# Bug 2624: Dirty commit log segments discarded prematurely when writes occur after flush position

**Commit**: e37a12a7c1 · **Ticket**: CASSANDRA-2829

## Detection Rule
When a flush completion marks a commit log segment clean, check whether additional writes arrived between the flush start (when the ReplayPosition was recorded) and flush completion, or else the segment may be falsely cleaned and deleted while still dirty.

## Code Smell
Code calls `segment.turnOn(id)` on a flush completion without comparing the flush's recorded position against the segment's current write position, meaning concurrent writes between flush-start and flush-end are silently lost.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: flush-position-not-checked
- severity: data-loss
- subsystem: commitlog-cdc

## Root Cause
When a column family flush completed, `CommitLog.discardCompletedSegments()` called `segment.turnOn(id)` unconditionally for the segment matching the flush's ReplayPosition. This marked the CF clean even if writes arrived after the ReplayPosition was captured, because those writes had already incremented the segment's position past the stored context. A concurrent write between flush initiation and flush completion left the segment appearing fully flushed, so `isSafeToDelete()` could return `true` and the segment would be deleted before all mutations were durably flushed.

## Example
```java
// BEFORE
segment.turnOn(id);  // always marks CF as flushed regardless of post-ReplayPosition writes
maybeDiscardSegment(segment, iter);

// AFTER
// Only unmark this segment if there were no writes since the ReplayPosition was grabbed.
segment.turnOffIfNotWritten(id, context.position);
maybeDiscardSegment(segment, iter);
```
