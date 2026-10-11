**Commit**: d6133f6997 · **Ticket**: KAFKA-18988
**Subsystem**: connect/runtime
**Category**: Logic error in condition

**Root cause**
Reviewers: Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
                workerId, generation()));
                workerId, generation()));
                workerId, generation()));
                workerId, generation()));
                workerId, generation()));
                trace(cause), workerId, generation()));
        statusBackingStore.put(new TaskStatus(id, TaskStatus.State.RUNNING, workerId, generation()));
        statusBackingStore.putSafe(new TaskStatus(id, TaskStatus.State.FAILED, workerId, generation(), trace(cause)));
        statusBackingStore.putSafe(new TaskStatus(id, TaskStatus.State.UNASSIGNED, workerId, generation()));
        statusBackingStore.put(new TaskStatus(id, TaskStatus.State.RUNNING, workerId, generation()));
        statusBackingStore.put(new TaskStatus(id, TaskStatus.State.PAUSED, workerId, generation()));
        statusBackingStore.put(new ConnectorStatus(connector, ConnectorStatus.State.DESTROYED, workerId, generation()));

// After
                workerId, generation(), worker.connectorVersion(connector)));
                workerId, generation(), worker.connectorVersion(connector)));
                workerId, generation(), worker.connectorVersion(connector)));
                workerId, generation(), worker.connectorVersion(connector)));
                workerId, generation(), worker.connectorVersion(connector)));
                trace(cause), workerId, generation(), worker.connectorVersion(connector)));
        statusBackingStore.put(new TaskStatus(id, TaskStatus.State.RUNNING, workerId, generation(), null,
                worker.taskVersion(id)));
        statusBackingStore.putSafe(new TaskStatus(id, TaskStatus.State.FAILED, workerId, generation(), trace(cause),
                worker.taskVersion(id)));
        statusBackingStore.putSafe(new TaskStatus(id, TaskStatus.State.UNASSIGNED, workerId, generation(), null,
                worker.taskVersion(id)));
```

**How to spot**
When reviewing connect/runtime code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18988: connect multiversion support (updates to status and metrics) (#17988).

**Test**
Test modified in AbstractWorkerSourceTaskTest.java covering the corrected behaviour.
