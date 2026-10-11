**Commit**: 4333af5c9f · **Ticket**: KAFKA-15045
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
To avoid confusion in 3.8/until we fully remove all the old task assignors and internal config, we should rename the old internal assignor classes like the StickyTaskAssignor so that they won't be mixed up with the new version of the assignor (which is also named StickyTaskAssignor)

Reviewers: Bruno Cadonna <cadonna@apache.org>, Josep Prat <josep.prat@aiven.io>

**Before/After**
```java
// Before
import org.apache.kafka.streams.processor.internals.assignment.StickyTaskAssignor;
        if (taskAssignor instanceof StickyTaskAssignor) {
            // special case: to preserve pre-existing behavior, we invoke the StickyTaskAssignor
    private final StickyTaskAssignor delegate;
        delegate = new StickyTaskAssignor(true);
public class StickyTaskAssignor implements LegacyTaskAssignor {
    private static final Logger log = LoggerFactory.getLogger(StickyTaskAssignor.class);
    public StickyTaskAssignor() {
    StickyTaskAssignor(fi

// After
import org.apache.kafka.streams.processor.internals.assignment.LegacyStickyTaskAssignor;
        if (taskAssignor instanceof LegacyStickyTaskAssignor) {
            // special case: to preserve pre-existing behavior, we invoke the LegacyStickyTaskAssignor
    private final LegacyStickyTaskAssignor delegate;
        delegate = new LegacyStickyTaskAssignor(true);
// Note: as of 3.8, this class has been renamed from StickyTaskAssignor to LegacyStickyTaskAssignor,
// and a new StickyTaskAssignor implementation was added that implements the new TaskAssignor interface.
// If you were previously plugging in the old StickyTaskAssignor via the internal.task.assignor.class config,
// you should migrate to the new TaskAssignor interface by removing the internal config and instead
// passing in the new StickyTaskAssignor class to the new public task.assignor.class config
public class LegacyStickyTaskAssignor implements LegacyTaskAssignor {
    private static final Logger log = LoggerFactory.getLogger(LegacyStickyTaskAssignor.class);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15045: (kip-924 pt. 25) rename old internal stickytaskassignor to legacystickytaskassignor (#16322).

**Test**
Test modified in StreamsConfigTest.java covering the corrected behaviour.
