**Commit**: ced34e3176 · **Ticket**: KAFKA-16379
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
This patch introduces a wrapper around [HdrHistogram](https://github.com/HdrHistogram/HdrHistogram) to use for group coordinator histograms, event queue time, event processing time, flush time, and purgatory time. Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
    implementation libs.hdrHistogram
                <allow pkg="org.HdrHistogram" />
import static org.apache.kafka.coordinator.common.runtime.CoordinatorRuntime.CoordinatorWriteEvent.NOT_QUEUED;

                    long flushStartMs = time.milliseconds();
                    runtimeMetrics.recordFlushTime(time.milliseconds() - flushStartMs);
                long flushStartMs = time.milliseconds();
                runtimeMetrics.recordFlushTime(time.milliseconds() - flushStartMs);

        /**
         * Indicates that the event was not appended to the deferred event queue.
         */
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16379; coordinator event queue, processing, flush, purgatory time histograms (#16949).

**Test**
Test modified in CoordinatorRuntimeTest.java covering the corrected behaviour.
