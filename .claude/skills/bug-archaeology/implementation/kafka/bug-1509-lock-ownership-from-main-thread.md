**Commit**: 20b073bbee · **Ticket**: KAFKA-18498
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
Once a StreamThread receives its assignment, it will close the startup tasks. But during the closing process, the StandbyTask.closeClean() method will eventually call theStatemanagerUtil.closeStateManager method which needs to lock the state directory, but locking requires the calling thread be the current owner. Since the main thread grabs the lock on startup but moves on without releasing it, we need to update ownership explicitly here in order for the stream thread to close the startup task and begin processing. Reviewers: Matthias Sax <mjsax@apache.org>, Nick Telford

**Before/After**
```java
// Before
import java.util.HashMap;
    private final HashMap<TaskId, Thread> lockedTasksToOwner = new HashMap<>();
                if (predicate.test(entry.getValue()) && tasksForLocalState.remove(entry.getKey()) != null) {
from ducktape.mark import ignore
    @ignore
    @ignore
    @ignore
    @ignore

// After
    private final ConcurrentMap<TaskId, Thread> lockedTasksToOwner = new ConcurrentHashMap<>();
                if (predicate.test(entry.getValue()) && removeStartupTask(entry.getKey()) != null) {
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18498: update lock ownership from main thread (#18732).

**Test**
Test modified in streams_eos_test.py covering the corrected behaviour.
