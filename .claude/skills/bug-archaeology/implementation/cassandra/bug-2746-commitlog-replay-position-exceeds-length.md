# Bug 2746: CommitLog Replay Skips Segments When Flush Position Exceeds File Length

**Commit**: 21622125f3 · **Ticket**: CASSANDRA-2285

## Detection Rule
When seeking into a commitlog segment using a stored replay position, check that `replayPosition <= reader.length()` before seeking; a position beyond file end means data was flushed before the write completed.

## Code Smell
The guard `if (replayPosition < 0)` only catches the "header missing" case; it does not handle `replayPosition > reader.length()`, causing `reader.seek()` to throw or read garbage.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: incomplete-bounds-check
- severity: crash
- subsystem: commitlog-cdc

## Root Cause
A memtable can be flushed and its commit-log position recorded before the corresponding bytes are actually synced to the segment file. On replay, the stored position can therefore exceed the actual file length. The fix extends the existing negative-position guard to also skip fully-flushed segments when `replayPosition > reader.length()`.

## Example
```java
// BEFORE
if (replayPosition < 0) {
    logger.debug("skipping replay of fully-flushed {}", file);
    continue;
}
reader.seek(replayPosition);
```
```java
// AFTER
if (replayPosition < 0 || replayPosition > reader.length()) {
    // replayPosition > reader.length() can happen if some data gets
    // flushed before it is written to the commitlog (CASSANDRA-2285)
    logger.debug("skipping replay of fully-flushed {}", file);
    continue;
}
reader.seek(replayPosition);
```
