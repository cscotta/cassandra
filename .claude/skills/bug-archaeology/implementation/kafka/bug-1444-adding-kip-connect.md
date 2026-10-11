**Commit**: c13324fc16 · **Ticket**: KAFKA-15995
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Greg Harris <gharris1727@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    <suppress checks="ParameterNumber"
              files="WorkerSinkTaskTest.java"/>

import org.apache.kafka.common.metrics.PluginMetrics;


    /**
     * Get a {@link PluginMetrics} that can be used to define metrics
     *
     * <p>This method was added in Apache Kafka 4.1. Connectors that use this method but want to
     * maintain backward compatibility so they can also be deployed to older Connect runtimes
     * should guard the call to this method with a try-catch block, since calling this method will result in a
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular:  kafka-15995: adding kip-877 support to connect  (#17804).

**Test**
Test modified in SinkConnectorTest.java covering the corrected behaviour.
