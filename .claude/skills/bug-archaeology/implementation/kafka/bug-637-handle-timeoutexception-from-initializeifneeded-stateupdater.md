**Commit**: 9b0c9dbb10 · **Ticket**: KAFKA-19864
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
Handle TimeoutException in StateUpdater code path in `TaskManager.java`

Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
            verify(restoredTask).clearTaskTimeout();
        final Task task00 = new StateMachineTask(taskId00, taskId00Partitions, true, stateManager) {
            @Override
            public void initializeIfNeeded() {
                throw new LockException("can't lock");
            }
        };
        final Task task01 = new St

// After
                task.clearTaskTimeout();
        } catch (final TimeoutException timeoutException) {
            // A timeout can occur either during producer initialization OR while fetching committed offsets.
            // Retry in the next iteration.
            task.maybeInitTaskTimeoutOrThrow(nowMs, timeoutException);
            tasks.addPendingTasksToInit(Collections.singleton(task));
            updateOrCreateBackoffRecord(task.id(), nowMs);
            log.info("Encountered timeout exception. Reattempting initialization in the next iteration. Error message was: {}",
                     timeoutException.getMessage());
        verify(task00, never()).clearTaskTimeout();
        verify(task01).clearTaskTimeout();
        verify(tasks).addPendingTasksToInit(
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19864: handle timeoutexception from initializeifneeded() in stateupdater code (#20829).

**Test**
Test modified in TaskManagerTest.java covering the corrected behaviour.
