# Bug 2481: Race in AntiEntropyService — repair session termination races with tree receipt

**Commit**: 09b4f448e2 · **Ticket**: CASSANDRA-3548

## Detection Rule

When `terminated` is set after unregistering listeners but jobs are cleared separately in `forceShutdown()`, a concurrent thread receiving a Merkle tree after `terminated=true` but before jobs are cleared will assert that `job != null` on an empty queue.

## Code Smell

A termination sequence where `terminated = true` and `jobs.clear()` are in separate methods/paths, with an intervening assertion `assert job != null` that assumes termination state is atomic.

## Tags

- scope: cassandra
- category: race-condition
- code-pattern: non-atomic-termination-sequence
- severity: crash
- subsystem: repair

## Root Cause

`RepairSession` set `terminated = true` in the `finally` block but `jobs.clear()` only in `forceShutdown()`. If a Merkle tree arrived after `terminated` was set but before/without `forceShutdown()`, `session.jobs.peek()` returned non-null and then `job.addTree()` proceeded on a session that was terminating. Conversely, the assert `job != null` fired when `jobs` was already cleared via another path. The fix introduces a single `terminate()` method that atomically sets the flag and clears both job queues.

## Example

```java
// BEFORE
finally {
    FailureDetector.instance.unregisterFailureDetectionEventListener(this);
    Gossiper.instance.unregister(this);
    terminated = true;   // jobs not cleared here
    AntiEntropyService.instance.sessions.remove(getName());
}
```

```java
// AFTER
finally {
    terminate();  // sets terminated=true AND clears jobs+activeJobs atomically
    FailureDetector.instance.unregisterFailureDetectionEventListener(this);
    Gossiper.instance.unregister(this);
    AntiEntropyService.instance.sessions.remove(getName());
}

public void terminate() {
    terminated = true;
    jobs.clear();
    activeJobs.clear();
}
```
