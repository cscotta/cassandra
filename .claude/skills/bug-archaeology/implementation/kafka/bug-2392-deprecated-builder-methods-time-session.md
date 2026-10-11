**Commit**: e0ee73e98b · **Ticket**: KAFKA-16332
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Removed deprecated methods:
 - TimeWindows#of
 - TimeWindows#grace
 - SessionWindows#with
 - SessionWindows#grace
 - SlidingWindows#withTimeDifferencAndGrace

Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
import static org.apache.kafka.streams.kstream.Windows.DEPRECATED_DEFAULT_24_HR_GRACE_PERIOD;
    // flag to check if the grace is already set via ofInactivityGapAndGrace or ofInactivityGapWithNoGrace
    private final boolean hasSetGrace;

    private SessionWindows(final long gapMs, final long graceMs, final boolean hasSetGrace) {
        this.hasSetGrace = hasSetGrace;
        return new SessionWindows(inactivityGapMs, afterWindowEndMs, true);
    }

    /**
     * Create a new window specification with the specified inactivity gap.
     *

// After
    private SessionWindows(final long gapMs, final long graceMs) {
        return new SessionWindows(inactivityGapMs, afterWindowEndMs);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16332 remove deprecated builder methods for time/session/join/slidingwindows (#17126).

**Test**
Test modified in TopologyTest.java covering the corrected behaviour.
