**Commit**: 0c97338959 · **Ticket**: KAFKA-18796
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Minor PR to correct the check for the presence of acquisition lock in
`assertionFailedMessage` method in `SharePartitionTest`

Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
                        sharePartition.cachedState().get(entry.getKey()).offsetState().get(offset).acquisitionLockTimeoutTask() == null));
                    sharePartition.cachedState().get(entry.getKey()).batchAcquisitionLockTimeoutTask() == null));

// After
                        sharePartition.cachedState().get(entry.getKey()).offsetState().get(offset).acquisitionLockTimeoutTask() != null));
                    sharePartition.cachedState().get(entry.getKey()).batchAcquisitionLockTimeoutTask() != null));
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18796-2: corrected the check for acquisition lock timeout in sh… (#19338).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
