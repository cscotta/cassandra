# Bug 2863: CommitLog recovery deletes newly-created segment due to initialization order

**Commit**: cf1a65498b · **Ticket**: CASSANDRA-1644

## Detection Rule
When initializing a singleton via a holder class (double-checked locking pattern), accessing the singleton field during the holder class's static initialization can cause the new instance to be constructed before recovery completes, resulting in the fresh segment being incorrectly included in the recovery-and-delete pass.

## Code Smell
`CommitLog.instance()` used a `CLHandle` inner class for lazy initialization. Code that called `CommitLog.instance()` during commitlog recovery triggered `CLHandle`'s static initializer, which called `new CommitLog()`, which added a fresh segment — *before* old segments were fully recovered and discarded. The recovery code then deleted the new segment along with the old ones.

## Tags
- scope: cassandra
- category: state-not-cleaned
- code-pattern: singleton-initialization-order
- severity: data-loss
- subsystem: commitlog-cdc

## Root Cause
The holder-class lazy-init pattern (`CLHandle.instance`) caused `new CommitLog()` (which adds a fresh segment to `segments`) to run at an unexpected time relative to recovery. The fix changes to eager initialization (`public static final CommitLog instance = new CommitLog()`) and moves directory creation into the constructor, ensuring the fresh segment is only added after all recovery is done and the static field is set once at class load time.

## Example
```java
// BEFORE
public static CommitLog instance() { return CLHandle.instance; }
private static class CLHandle {
    public static final CommitLog instance = new CommitLog();
}

// AFTER
public static final CommitLog instance = new CommitLog();
// constructor now creates directories and adds its first segment
// only after all recovery code has run
```
