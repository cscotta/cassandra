# Bug 2906: CommitLog replays fully-flushed segments unnecessarily

**Commit**: 0448530e31 · **Ticket**: CASSANDRA-1298

## Detection Rule
When a sentinel value (0) is returned for both the "no dirty CFs" case and a legitimate offset of 0, callers cannot distinguish between the two states.

## Code Smell
A method returns 0 both when the map is empty (meaning "fully flushed, nothing to replay") and as a valid position (offset 0), forcing callers to always replay from the start of clean segments.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: sentinel-value-collision
- severity: performance
- subsystem: commitlog-cdc

## Root Cause
`CommitLogHeader.getReplayPosition()` returned `0` when `cfDirtiedAt` was empty, which is the same value as a valid byte offset of 0. The CommitLog replayer couldn't distinguish "nothing dirty, skip this segment" from "replay from the beginning". Changing the empty-map sentinel to `-1` lets the replayer skip already-flushed segments entirely.

## Example
```java
// BEFORE
int getReplayPosition()
{
    return cfDirtiedAt.isEmpty() ? 0 : Collections.min(cfDirtiedAt.values());
}
// caller: reader.seek(replayPosition);  // always seeks to 0, replays full segment
```
```java
// AFTER
int getReplayPosition()
{
    return cfDirtiedAt.isEmpty() ? -1 : Collections.min(cfDirtiedAt.values());
}
// caller:
if (replayPosition < 0) { continue; }  // skip fully-flushed segment
reader.seek(replayPosition);
```
