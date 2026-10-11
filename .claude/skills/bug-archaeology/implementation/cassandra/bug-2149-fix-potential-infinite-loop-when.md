# Bug 2149: Infinite Loop When Reloading ColumnFamilyStore After Comparator Change

**Commit**: 9b217e4676 · **Ticket**: CASSANDRA-5064
**Subsystem**: src/java/org/apache/cassandra/db/ColumnFamilyStore.java, src/java/org/apache/cassandra/db/Memtable.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`ColumnFamilyStore.reload()` detected a comparator change by looping: it checked the current memtable's comparator, called `maybeSwitchMemtable`, and repeated until the comparator matched. `maybeSwitchMemtable` was a no-op if the memtable was already frozen (returning `null`), and it checked the frozen flag both before and after acquiring the write lock. If another thread had already frozen the memtable by the time the reload loop called `maybeSwitchMemtable`, the method returned `null`, no flush future was waited on, and the loop re-checked the comparator—which was still the old one because the switch had not completed yet. This race produced an infinite spin. The fix replaced the retry loop with a single unconditional `switchMemtable(true, true)` call and refactored the method to accept a `forceSwitch` flag that bypasses the frozen/clean guards, while also making `forceFlush()` always return a non-null `Future` so callers no longer need null checks.

### Before / After

```java
// BEFORE – can loop forever if maybeSwitchMemtable returns null
while (true)
{
    AbstractType comparator = metadata.comparator;
    Memtable memtable = getMemtableThreadSafe();
    if (memtable.initialComparator == comparator)
        break;
    Future future = maybeSwitchMemtable(getMemtableThreadSafe(), true);
    if (future != null)
        future.get();
    // if future == null, loop again — infinite if memtable stays frozen
}
```

```java
// AFTER – single unconditional switch
if (getMemtableThreadSafe().initialComparator != metadata.comparator)
    switchMemtable(true, true);
```

---

## How to Spot

A `while(true)` loop that depends on a state change triggered by a method that can return without making progress (returning `null`, or a no-op) is a liveness hazard. Any such loop must have a guaranteed exit condition that cannot be defeated by a concurrent state transition.

---

## Test

No test added.
