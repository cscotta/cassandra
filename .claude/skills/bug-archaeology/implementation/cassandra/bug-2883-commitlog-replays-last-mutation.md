# Bug 2883: Commitlog replays the last already-flushed mutation unnecessarily

**Commit**: ad29c258ea · **Ticket**: CASSANDRA-1512

## Detection Rule
When filtering commitlog entries to replay, check whether the boundary condition uses `>=` or `>` for the flush position comparison — using `>=` will replay the mutation at the exact flush position that was already applied.

## Code Smell
`if (finalHeader.isDirty(id) && entryLocation >= finalHeader.getPosition(id))` — the `>=` comparison causes the entry whose position equals the recorded flush position to be replayed, even though the commitlog header records the position of the last successfully flushed entry (meaning that entry was already persisted).

## Tags
- scope: cassandra
- category: off-by-one
- code-pattern: boundary-condition-ge-vs-gt
- severity: silent-wrong-result
- subsystem: commitlog-cdc

## Root Cause
The commitlog header stores the position of the last flushed mutation. An entry at exactly that position was already durably written to an SSTable. Using `>=` caused that entry to be re-applied during recovery, leading to a duplicate write (harmless for idempotent mutations, but incorrect in general). Changing to `>` replays only mutations strictly after the flush watermark.

## Example
```java
// BEFORE — replays the mutation AT the flush position
if (finalHeader == null ||
    (finalHeader.isDirty(columnFamily.id()) &&
     entryLocation >= finalHeader.getPosition(columnFamily.id())))
    newRm.add(columnFamily);
```
```java
// AFTER — only replay mutations strictly after flush position
if (finalHeader == null ||
    (finalHeader.isDirty(columnFamily.id()) &&
     entryLocation > finalHeader.getPosition(columnFamily.id())))
    newRm.add(columnFamily);
```
