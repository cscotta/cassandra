**Commit**: e32fb9772b · **Ticket**: KAFKA-19825
**Subsystem**: coordinator-common/src
**Category**: Logic error in condition

**Root cause**
Add an effective batch linger time metric for the group coordinator and
share coordinator. The effective batch linger time may exceed the
configured group.coordinator.append.linger.ms or
share.coordinator.append.linger.ms due to queuing delays in the
coordinator runtime. Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
                    runtimeMetrics.recordLingerTime(flushStartMs - currentBatch.appendTimeMs);
    /**
     * Record the effective batch linger time.
     *
     * @param durationMs The linger time in milliseconds.
     */
    void recordLingerTime(long durationMs);

    /**
     * The effective batch linger time metric name.
     */
    public static final String BATCH_LINGER_TIME_METRIC_NAME = "batch-linger-time-ms";
```

**How to spot**
When reviewing coordinator-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19825 [1/2]: add coordinator effective batch linger time metric (#20753).

**Test**
Test modified in CoordinatorRuntimeMetricsImplTest.java covering the corrected behaviour.
