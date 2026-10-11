**Commit**: 71495a2013 · **Ticket**: KAFKA-18568
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The reason for flakiness is PR #18080 which modifies the linger.ms config from 0 to 5. ClientIdQuotaTest are testing "Low enough quota that a producer sending a small payload in a tight loop should get throttled", thus this config change Influence this test scenario. This commits uses the older value of 0ms for linger.ms for ClientIdQuotaTest tests. Reviewers: Ismael Juma <ismael@juma.me.uk>, TaiJuWu <tjwu1217@gmail.com>, Divij Vaidya <diviv@amazon.com>

**Before/After**
```java
// Before
// (no removals)

// After
  this.producerConfig.setProperty(ProducerConfig.LINGER_MS_CONFIG, 0.toString)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18568: fix flaky test clientidquotatest (#18612).

**Test**
Test modified in BaseQuotaTest.scala covering the corrected behaviour.
