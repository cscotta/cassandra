# Bug 1077: Fix Rare Race Where Older Gossip States Can Be Shadowed

**Commit**: 6f8e07ab06 · **Ticket**: CASSANDRA-10366
**Subsystem**: src/java/org/apache/cassandra/gms/Gossiper.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`addLocalApplicationState` was a public method that updated gossip state without holding `taskLock`. `addLocalApplicationStates` (the batch variant) correctly acquired `taskLock` before iterating, but then called the public `addLocalApplicationState` for each entry, which did not assert or require the lock. A caller that used the single-state variant directly could therefore race with the batch variant: the lock was held by the batch call for intermediate states, but a direct single-state call from another thread could interleave, publishing a newer version number before older states in the same logical batch were applied, causing those older states to be silently dropped by receiving nodes that had already seen the higher version.

The fix renames the unlocked implementation to `addLocalApplicationStateInternal` (which asserts the lock is held), rewrites the public `addLocalApplicationState` to delegate to `addLocalApplicationStates`, and keeps the lock scope exclusively in the batch method.

### Before / After

```java
// BEFORE — public method, no lock assertion
public void addLocalApplicationState(ApplicationState state, VersionedValue value)
{
    // ... mutates endpointStateMap without holding taskLock
}

public void addLocalApplicationStates(List<Pair<ApplicationState, VersionedValue>> states)
{
    taskLock.lock();
    try {
        for (Pair<ApplicationState, VersionedValue> pair : states)
            addLocalApplicationState(pair.left, pair.right); // re-entrant but lock not checked
    } ...
}
```

```java
// AFTER — internal method asserts lock; public single-state delegates to batch
private void addLocalApplicationStateInternal(ApplicationState state, VersionedValue value)
{
    assert taskLock.isHeldByCurrentThread();
    // ...
}

public void addLocalApplicationState(ApplicationState applicationState, VersionedValue value)
{
    addLocalApplicationStates(Arrays.asList(Pair.create(applicationState, value)));
}

public void addLocalApplicationStates(List<Pair<ApplicationState, VersionedValue>> states)
{
    taskLock.lock();
    try {
        for (Pair<ApplicationState, VersionedValue> pair : states)
            addLocalApplicationStateInternal(pair.left, pair.right);
    } ...
}
```

---

## How to Spot

When a class has both a single-item and a batch variant of a mutating operation and the batch variant holds a lock, verify that the single-item variant either also holds the lock independently or delegates to the batch variant. A public method that performs the same mutation without the lock is a hidden race: any external caller bypasses the synchronization that the batch path carefully sets up.

---

## Test

No test added.
