**Commit**: f7d07d62d9 · **Ticket**: KAFKA-18990
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Avoid redundant MetricName creation in BaseQuotaTest#produceUntilThrottled via moving metrics creation out of loop. Reviewers: PoAn Yang <payang@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        val metric = throttleMetric(QuotaType.PRODUCE, producerClientId)

// After
    val metric = throttleMetric(QuotaType.PRODUCE, producerClientId)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18990 avoid redundant metricname creation in basequotatest#produceuntilthrottled (#19215).

**Test**
Test modified in BaseQuotaTest.scala covering the corrected behaviour.
