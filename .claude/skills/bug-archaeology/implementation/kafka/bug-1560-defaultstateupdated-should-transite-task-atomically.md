**Commit**: f2f9e84bca · **Ticket**: KAFKA-17402
**Subsystem**: streams/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Bruno Cadonna <bruno@confluent.io>, Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
                updatingTasks.remove(task.id());

// After
                updatingTasks.remove(task.id());
```

**How to spot**
When reviewing streams/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17402: defaultstateupdated should transite task atomically (#18607).

**Test**
No test added.
