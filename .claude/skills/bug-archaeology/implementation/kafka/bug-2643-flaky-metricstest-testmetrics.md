**Commit**: 047b5aefd0 · **Ticket**: KAFKA-16346
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    verifyYammerMetricRecorded(s"$requestMetricsPrefix,name=MessageConversionsTimeMs,request=Produce", value => value > 0.0)

// After
    // if message conversion run too fast, the Math.round(value) may be 0.0, so using count to check whether the metric is updated
    assertTrue(yammerHistogram(s"$requestMetricsPrefix,name=MessageConversionsTimeMs,request=Produce").count() > 0, "MessageConversionsTimeMs count should be > 0")
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16346 fix flaky metricstest.testmetrics (#15502).

**Test**
Test modified in MetricsTest.scala covering the corrected behaviour.
