# Bug 2674: NPE when bypassing commitlog during memtable flush

**Commit**: 4e96cc9d41 · **Ticket**: CASSANDRA-2781

## Detection Rule
When a code path conditionally produces a nullable context object and downstream code unconditionally dereferences it, replace the `null` sentinel with a designated no-op constant to avoid a NullPointerException.

## Code Smell
`final ReplayPosition ctx = writeCommitLog ? CommitLog.instance.getContext() : null;` where `ctx` is later passed to flush routines that dereference it without a null check.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: null-sentinel-dereference
- severity: crash
- subsystem: commitlog-cdc

## Root Cause
When `writeCommitLog` is `false` (commitlog bypassed for certain keyspaces), the replay-position context was set to `null`. Flush routines that accepted this context later tried to use it, causing an NPE. The fix substitutes `ReplayPosition.NONE` — a safe no-op sentinel — instead of `null`.

## Example
```java
// BEFORE
final ReplayPosition ctx = writeCommitLog ? CommitLog.instance.getContext() : null;
```
```java
// AFTER
final ReplayPosition ctx = writeCommitLog ? CommitLog.instance.getContext() : ReplayPosition.NONE;
```
