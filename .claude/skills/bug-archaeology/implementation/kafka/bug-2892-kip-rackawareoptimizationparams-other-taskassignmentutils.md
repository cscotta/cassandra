**Commit**: 0782232fbe · **Ticket**: KAFKA-15045
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
We now provide a way to more easily customize the rack aware
optimizations that we provide by way of a configuration class called
RackAwareOptimizationParams. We also simplified the APIs for the optimizeXYZ utility functions since
they were mutating the inputs anyway. Reviewers: Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
     * Return an {@code AssignmentError} for a task assignment created for an application.

// After
import java.util.TreeSet;
     * A simple config container for necessary parameters and optional overrides to apply when
     * running the active or standby task rack-aware optimizations.
     */
    public static class RackAwareOptimizationParams {
        private final ApplicationState applicationState;
        private final Optional<Integer> trafficCostOverride;
        private final Optional<Integer> nonOverlapCostOverride;
        private final Optional<SortedSet<TaskId>> tasksToOptimize;

        private RackAwareOptimizationParams(final ApplicationState applicationState,
                                           final Optional<Integer> trafficCostOverride,
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15045: (kip-924 pt. 22) add rackawareoptimizationparams and other minor taskassignmentutils changes (#16294).

**Test**
Test modified in TaskAssignmentUtilsTest.java covering the corrected behaviour.
