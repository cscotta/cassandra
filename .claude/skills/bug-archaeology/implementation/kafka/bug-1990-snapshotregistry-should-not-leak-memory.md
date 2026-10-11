**Commit**: 5fba067aaa · **Ticket**: KAFKA-18063
**Subsystem**: server-common/src
**Category**: Resource leak

**Root cause**
SnapshotRegistry needs to have a reference to all snapshot data structures. However, this should
not be a strong reference, but a weak reference, so that these data structures can be garbage
collected as needed. This PR also adds a scrub mechanism so that we can eventually reclaim the
slots used by GC'ed Revertable objects in the SnapshotRegistry.revertables array. Reviewers: David Jacot <david.jacot@gmail.com>

**Before/After**
```java
// Before

 * A registry containing snapshots of timeline data structures.
 * We generally expect a small number of snapshots-- perhaps 1 or 2 at a time.
 * Therefore, we use ArrayLists here rather than a data structure with higher overhead.
     * Collection of all Revertable registered with this registry
    private final List<Revertable> revertables = new ArrayList<>();
        revertables.add(revertable);
     * Delete all snapshots and resets all of the Revertable object registered.
        for (Revertable revertable : revertables) {
            revertable.reset();

// After
import java.lang.ref.WeakReference;
 * A registry containing snapshots of timeline data structures. All timeline data structures must
 * be registered here, so that they can be reverted to the expected state when desired.
 * Because the registry only keeps a weak reference to each timeline data structure, it does not
 * prevent them from being garbage collected.
     * A collection of all Revertable objects registered here. Since we store only weak
     * references, every time we access a revertable through this list, we must check to
     * see if it has been garbage collected. If so, WeakReference.get will return null.
     *
     * Although the garbage collector handles freeing the underlying Revertables, over
     * time slots in the ArrayList will fill up with expired references. Therefore, after
     * enough registrations, we scrub the ArrayList of the expired references by creating
```

**How to spot**
When reviewing server-common/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18063: snapshotregistry should not leak memory (#17898).

**Test**
Test modified in SnapshotRegistryTest.java covering the corrected behaviour.
