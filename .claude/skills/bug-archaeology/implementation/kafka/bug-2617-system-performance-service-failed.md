**Commit**: 5a602b2f86 · **Ticket**: KAFKA-17235
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
related to https://issues.apache.org/jira/browse/KAFKA-17235

The root cause of this issue is a change we introduced in KAFKA-16879, where we modified the PushHttpMetricsReporter constructor to use Time.System [1]. However, Time.System doesn't exist in Kafka versions 0.8.2 and 0.9. In test_performance_services.py, we have system tests for Kafka versions 0.8.2 and 0.9 [2]. These tests always use the tools JAR from the trunk branch, regardless of the Kafka version being tested [3], while the client JAR aligns with the Kafka version specified in the test suite [4].

**Before/After**
```java
// Before
    private final Time time;
        time = Time.SYSTEM;
        time = mockTime;
            long now = time.milliseconds();

// After
import java.util.function.Supplier;
    private final Supplier<Long> currentTimeMillis;
        // In test_performance_services.py, we have system tests for Kafka versions 0.8.2 and 0.9.
        // These tests always use the tools jar from the trunk branch, regardless of the Kafka version being tested,
        // while the client jar aligns with the Kafka version specified in the test suite. To ensure these system test
        // passed, we need to make this class compatible with older client jars. This discrepancy force us not to use
        // `Time.SYSTEM` here as there is no such field in the older Kafka version.
        currentTimeMillis = System::currentTimeMillis;
        currentTimeMillis = mockTime::milliseconds;
            long now = currentTimeMillis.get();
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17235 system test test_performance_service.py failed (#16789).

**Test**
No test added.
