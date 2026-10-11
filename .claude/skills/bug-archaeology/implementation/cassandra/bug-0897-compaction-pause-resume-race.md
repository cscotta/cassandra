# Bug 0897: Race in CompactionStrategyManager pause/resume

**Commit**: f40c632e42 · **Ticket**: CASSANDRA-11922
**Subsystem**: Compaction
**Category**: Race condition / missing synchronization

---

## Root Cause
`CompactionStrategyManager.getNextBackgroundTask()` read the `isEnabled()` check and the strategy lists **before** acquiring the read lock, so a concurrent `pause()` call could change `isActive` between the check and the strategy iteration. Similarly, `pause()` and `resume()` mutated `isActive` as a bare volatile write, providing no mutual exclusion with the read-locked consumers that also inspect `isActive` through `isEnabled()`. The result was that a compaction task could be issued even after pause was requested, or the `isActive` flag could be read in an inconsistent state.

### Before / After
```java
// BEFORE — check and list-copy outside the lock
public AbstractCompactionTask getNextBackgroundTask(int gcBefore)
{
    if (!isEnabled())          // no lock held here
        return null;
    maybeReload(cfs.metadata); // ditto
    List<AbstractCompactionStrategy> strategies = new ArrayList<>();
    readLock.lock();
    try { strategies.addAll(repaired); ... }

// AFTER — entire check inside the lock
public AbstractCompactionTask getNextBackgroundTask(int gcBefore)
{
    readLock.lock();
    try {
        if (!isEnabled()) return null;
        maybeReload(cfs.metadata);
        List<AbstractCompactionStrategy> strategies = new ArrayList<>();
        strategies.addAll(repaired); ...

// BEFORE — bare volatile write in pause/resume
public void pause()  { isActive = false; }
public void resume() { isActive = true;  }

// AFTER — write-locked mutation
public void pause()
{
    writeLock.lock();
    try { isActive = false; }
    finally { writeLock.unlock(); }
}
```

---

## How to Spot
When a class uses a `ReentrantReadWriteLock` to guard access to state, any code that reads or writes that state outside a lock acquisition is suspect. Check every method that accesses fields protected by the lock and verify the lock is held for the full logical operation (read-check + action), not just the data-structure access portion. A `volatile` field alone does not make a check-then-act sequence thread-safe.

---

## Test
No test added.
