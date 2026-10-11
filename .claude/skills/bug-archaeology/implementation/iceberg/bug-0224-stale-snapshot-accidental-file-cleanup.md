# Bug 0224: Committed snapshot files accidentally deleted due to stale pre-commit reference

**Commit**: aa5894a139bca2967bf5cc01feef3a920fcc6ca5 · **Ticket**: #15511

## Detection Rule

When a post-commit cleanup step uses an object reference captured before or during the commit (rather than reloading from the authoritative store after commit), verify that eventual consistency or retry logic cannot cause the captured reference to differ from what was actually committed.

## Code Smell

A field or local variable of the staged/new object is captured inside the retry loop (e.g., `AtomicReference<Snapshot> stagedSnapshot`) and then used after the loop to drive cleanup; the cleanup compares that in-memory object against persisted state without first refreshing from storage.

## Tags

- scope: distributed-systems
- category: state-not-cleaned
- code-pattern: stale-state-after-lifecycle
- severity: data-loss
- subsystem: core/snapshot-producer

## Root Cause

`SnapshotProducer.commit()` stored the `Snapshot` object produced by the most recent `apply()` call into an `AtomicReference<Snapshot>`. After the retry loop succeeded, cleanup used `stagedSnapshot.get()` to identify which manifests to keep. Because multiple retry attempts each call `apply()` and overwrite the reference, the final reference holds the last attempted snapshot object — but with eventual consistency, the metadata visible on a subsequent `refresh()` may not yet contain that snapshot, causing `cleanUncommitted` to incorrectly delete manifest files that belong to the successfully committed snapshot.

## Example

```java
// BEFORE — snapshot object captured inside retry loop, used directly for cleanup
AtomicReference<Snapshot> stagedSnapshot = new AtomicReference<>();
Tasks.foreach(ops)
    .run(taskOps -> {
        Snapshot newSnapshot = apply();
        stagedSnapshot.set(newSnapshot);          // in-memory object, may mismatch persisted state
        // ... commit ...
    });

Snapshot committedSnapshot = stagedSnapshot.get();
if (cleanupAfterCommit()) {
    cleanUncommitted(Sets.newHashSet(committedSnapshot.allManifests(ops.io())));
}
```

```java
// AFTER — only the snapshot ID is captured; snapshot is reloaded from storage after commit
AtomicLong newSnapshotId = new AtomicLong(-1L);
Tasks.foreach(ops)
    .run(taskOps -> {
        Snapshot newSnapshot = apply();
        newSnapshotId.set(newSnapshot.snapshotId());  // store ID only
        // ... commit ...
    });

Snapshot saved = ops.refresh().snapshot(newSnapshotId.get());
if (saved != null) {
    if (cleanupAfterCommit()) {
        cleanUncommitted(Sets.newHashSet(saved.allManifests(ops.io())));
    }
} else {
    LOG.warn("Failed to load committed snapshot, skipping manifest clean-up");
}
```
