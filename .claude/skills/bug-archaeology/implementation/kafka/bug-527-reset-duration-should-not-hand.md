**Commit**: aa016f1bec · **Ticket**: KAFKA-20002
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This bug was introduced via KAFKA-18015. Reviewers: Lucas Brutschy <lbrutschy@confluent.io>, Vincent Potuček
 (@Pankraz76)

**Before/After**
```java
// Before
            final Task task = getActiveTask(partition);
            task.maybeInitTaskTimeoutOrThrow(nowMs, timeoutException);
            stateUpdater.add(task);

// After
            getActiveTask(partition).maybeInitTaskTimeoutOrThrow(nowMs, timeoutException);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20002: reset-by-duration should not hand back task to state-updater (#21167).

**Test**
No test added.
