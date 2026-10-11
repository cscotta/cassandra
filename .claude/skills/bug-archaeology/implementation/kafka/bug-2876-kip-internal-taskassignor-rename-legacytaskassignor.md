**Commit**: 103ff5c0f0 · **Ticket**: KAFKA-15045
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Since the new public API for TaskAssignor shared a name, this rename will prevent users from confusing the internal definition with the public one. Reviewers: Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
import org.apache.kafka.streams.processor.internals.assignment.TaskAssignor;
    private Supplier<TaskAssignor> internalTaskAssignorSupplier;
        internalTaskAssignorSupplier = assignorConfiguration::taskAssignor;
            final TaskAssignor taskAssignor = createTaskAssignor(lagComputationSuccessful);
    private TaskAssignor createTaskAssignor(final boolean lagComputationSuccessful) {
        final TaskAssignor taskAssignor = internalTaskAssignorSupplier.get();
    public TaskAssignor taskAssignor() {
            return Utils.newInstance(internalTaskAssignorClass, TaskAssignor.class);
public class FallbackPriorTaskAssignor implements TaskAssignor {

// After
import org.apache.kafka.streams.processor.internals.assignment.LegacyTaskAssignor;
    private Supplier<LegacyTaskAssignor> legacyTaskAssignorSupplier;
        legacyTaskAssignorSupplier = assignorConfiguration::taskAssignor;
            final LegacyTaskAssignor taskAssignor = createTaskAssignor(lagComputationSuccessful);
    private LegacyTaskAssignor createTaskAssignor(final boolean lagComputationSuccessful) {
        final LegacyTaskAssignor taskAssignor = legacyTaskAssignorSupplier.get();
    public LegacyTaskAssignor taskAssignor() {
            return Utils.newInstance(internalTaskAssignorClass, LegacyTaskAssignor.class);
public class FallbackPriorTaskAssignor implements LegacyTaskAssignor {
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15045: (kip-924 pt. 24) internal taskassignor rename to legacytaskassignor (#16318).

**Test**
Test modified in TaskAssignorIntegrationTest.java covering the corrected behaviour.
