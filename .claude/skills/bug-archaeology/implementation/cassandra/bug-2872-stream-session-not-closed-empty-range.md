# Bug 2872: Streaming session not closed when transfer range has no data

**Commit**: c7810a8e17 · **Ticket**: CASSANDRA-1573

## Detection Rule
When a streaming session is created unconditionally but files are only added conditionally, verify there is an else branch that closes the session when no files are added, to avoid a session leak that blocks the caller.

## Code Smell
`StreamOut.transferRanges` created a `StreamSession`, called `session.begin()` only inside `if (pending.size() > 0)`, but had no `else` branch to close the session when there was nothing to stream, leaving the session dangling and the caller waiting forever.

## Tags
- scope: cassandra
- category: resource-leak
- code-pattern: session-not-closed-on-empty-path
- severity: hang
- subsystem: streaming

## Root Cause
`unbootstrap` depends on streaming sessions completing before it can finish. When a transfer range contained no SSTables, the session was never opened or closed, so the completion callback was never fired and `unbootstrap` hung. Adding an `else { session.close(); }` branch completes the session immediately when there is nothing to stream.

## Example
```java
// BEFORE — session just disappears if nothing to stream
if (pending.size() > 0) {
    session.addFilesToStream(pending);
    session.begin();
}
// else: session leaked, no completion signal
```
```java
// AFTER
if (pending.size() > 0) {
    session.addFilesToStream(pending);
    session.begin();
} else {
    session.close();
}
```
