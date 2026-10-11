**Commit**: 4d6cf3efef · **Ticket**: KAFKA-18913
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Updated the code to start the State Updater Thread only after the Stream
Thread is started. Changes done :
1. Moved the starting of the StateUpdater thread to a new init method in
the TaskManager. 2.

**Before/After**
```java
// Before
            maybeCreateAndStartStateUpdater(
    private static StateUpdater maybeCreateAndStartStateUpdater(final boolean stateUpdaterEnabled,
            final StateUpdater stateUpdater = new DefaultStateUpdater(
            stateUpdater.start();
            return stateUpdater;

// After
            maybeCreateStateUpdater(
    private static StateUpdater maybeCreateStateUpdater(final boolean stateUpdaterEnabled,
            return new DefaultStateUpdater(
            if (stateUpdaterEnabled) {
                taskManager.init();
            }
    void init() {
        if (stateUpdater != null) {
            this.stateUpdater.start();
        }
    }
import org.junit.jupiter.params.provider.ValueSource;
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18913: start state updater in task manager (#19889).

**Test**
Test modified in StreamThreadTest.java covering the corrected behaviour.
