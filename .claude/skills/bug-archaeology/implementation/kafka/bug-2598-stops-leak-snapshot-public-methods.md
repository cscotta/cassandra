**Commit**: 4e862c0903 · **Ticket**: KAFKA-15875
**Subsystem**: group-coordinator/src
**Category**: Resource leak

**Root cause**
* KAFKA-15875: Stops leak Snapshot in public methods

The Snapshot class is package protected but it's returned in
several public methods in SnapshotRegistry. To prevent this accidental leakage, these methods are made
package protected as well. For getOrCreateSnapshot a new
method called IdempotentCreateSnapshot is created that returns void. * Make builer package protected, replace <br> with <p>

Reviewers: Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
        snapshotRegistry.getOrCreateSnapshot(0);
        snapshotRegistry.getOrCreateSnapshot(offset);
        snapshotRegistry.getOrCreateSnapshot(lastWrittenOffset);
        snapshotRegistry.getOrCreateSnapshot(lastWrittenOffset);
            snapshotRegistry.getOrCreateSnapshot(lastWrittenOffset);
            snapshotRegistry.getOrCreateSnapshot(lastWrittenOffset);
        snapshotRegistry.getOrCreateSnapshot(1000);
        snapshotRegistry.getOrCreateSnapshot(2000);

// After
        snapshotRegistry.idempotentCreateSnapshot(0);
        snapshotRegistry.idempotentCreateSnapshot(offset);
        snapshotRegistry.idempotentCreateSnapshot(lastWrittenOffset);
        snapshotRegistry.idempotentCreateSnapshot(lastWrittenOffset);
            snapshotRegistry.idempotentCreateSnapshot(lastWrittenOffset);
            snapshotRegistry.idempotentCreateSnapshot(lastWrittenOffset);
        snapshotRegistry.idempotentCreateSnapshot(1000);
        snapshotRegistry.idempotentCreateSnapshot(2000);
```

**How to spot**
When reviewing group-coordinator/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15875: stops leak snapshot in public methods (#16807).

**Test**
Test modified in GroupMetadataManagerTestContext.java covering the corrected behaviour.
