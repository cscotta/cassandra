**Commit**: 8468317dac · **Ticket**: KAFKA-19467
**Subsystem**: checkstyle/import-control.xml
**Category**: Logic error in condition

**Root cause**
This change adds the metric ControllerEventManager::AvgIdleRatio which
measures the amount of time the controller spends blocked waiting for
events vs the amount of time spent processing events. A value of 1.0
means that the controller spent the entire interval blocked waiting for
events. Reviewers: José Armando García Sancio <jsancio@apache.org>, Kevin Wu
 <kevin.wu2412@gmail.com>, Alyssa Huang <ahuang@confluent.io>, TengYao
 Chi <frankvicky@apache.org>, Reviewers: Chia-Ping Tsai
 <chia7712@apache.org>

**Before/After**
```java
// Before
                queue = new KafkaEventQueue(time, logContext, threadNamePrefix);

// After
    <allow pkg="org.apache.kafka.server.metrics" />
    <li>
        A new metric <code>AvgIdleRatio</code> has been added to the <code>ControllerEventManager</code> group. This metric measures the average idle ratio of the controller event queue thread,
        providing visibility into how much time the controller spends waiting for events versus processing them. The metric value ranges from 0.0 (always busy) to 1.0 (always idle).
    </li>
                queue = new KafkaEventQueue(
                    time,
                    logContext,
                    threadNamePrefix,
                    EventQueue.VoidEvent.INSTANCE,
                    controllerMetrics::updateIdleTime
                );
```

**How to spot**
When reviewing checkstyle/import-control.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19467; add a metric for controller thread idleness (#20422).

**Test**
Test modified in QuorumControllerMetricsTest.java covering the corrected behaviour.
