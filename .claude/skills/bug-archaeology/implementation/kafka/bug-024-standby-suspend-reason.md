**Commit**: e31ae3ebfc · **Ticket**: KAFKA-16836
**Subsystem**: streams — DefaultStateUpdater / StoreChangelogReader / TaskManager
**Category**: Wrong constant / default value

**Root cause**
`StoreChangelogReader.unregister()` inferred the `SuspendReason` for the standby update listener by checking `task.state() == RUNNING`. Since `unregister()` is called from within the `DefaultStateUpdater` thread where *all* standby tasks are always in `RUNNING` state, the check always evaluated to `true` and reported `PROMOTED` — even when the task was actually being migrated to another broker. This caused `StandbyUpdateListener.onUpdateSuspended()` to always fire with `PROMOTED` instead of `MIGRATED`, giving incorrect signals to applications using standby listeners.

**Before/After**
```java
// Before — reason inferred from task state inside unregister (always PROMOTED)
public void unregister(Collection<TopicPartition> revokedChangelogs) {
    // ...
    SuspendReason reason = task.state() == RUNNING ? PROMOTED : MIGRATED;  // always PROMOTED
}

// After — reason passed explicitly from TaskManager through the call chain
TaskManager → StateUpdater.remove(taskId, reason)
    → DefaultStateUpdater → ChangelogReader.unregister(partitions, reason)
```

**How to spot**
Any code that infers context (like "why is this being removed?") from observable state within a subsystem will be wrong when that subsystem's state is always the same regardless of the true cause. The caller has the correct context; the callee should receive it explicitly. Search for `SuspendReason` inference inside `unregister` or similar cleanup methods.

**Test**
`DefaultStateUpdaterTest` and `StoreChangelogReaderTest` — updated to pass `SuspendReason` explicitly and verify the correct reason is forwarded to the standby update listener callback in each removal scenario (PROMOTED vs MIGRATED).
