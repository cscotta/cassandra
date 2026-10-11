**Commit**: a0309be7f4 · **Ticket**: KAFKA-17098
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
When Streams tries to transit a restored active task to RUNNING, the
first thing it does is getting the committed offsets for this task. If getting the offsets expires a timeout, Streams does not re-throw
the error initially, but tries to get the committed offsets later
until a Streams-specific timeout is hit. Restored active tasks from the state updater are removed from the
output queue of the restored tasks in the state updater. If a
timeout occurs, the restored task is neither added to the
task registry nor re-added to the state updater.

**Before/After**
```java
// Before
                    "Could not complete restoration for %s due to the following exception; will retry",

// After
            stateUpdater.add(task);
                    "Could not complete restoration for %s due to the following exception; adding the task " +
                        "back to the state updater and will retry",
        verify(stateUpdater).add(task);
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17098: re-add task to state updater if transit to running fails (#16570).

**Test**
Test modified in TaskManagerTest.java covering the corrected behaviour.
