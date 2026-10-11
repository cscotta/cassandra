**Commit**: 588d107ec2 · **Ticket**: KAFKA-19101
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
It seems `timeMs` this parameter never used in Kafka project, the method
init commit is

https://github.com/apache/kafka/commit/b5f90daf13b4945305951ca0eecdb454a4dcafc2

Reviewers: Jhen-Yung Hsu <jhenyunghsu@gmail.com>, PoAn Yang
<payang@apache.org>, TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai
<chia7712@gmail.com>

**Before/After**
```java
// Before
  protected var lastRecordedTimeMs = 0L
    lastThrottleTimeMs = ControllerMutationQuotaManager.throttleTimeMs(e, timeMs)
  def throttleTimeMs(e: QuotaViolationException, timeMs: Long): Long = {
        val throttleTimeMs = ControllerMutationQuotaManager.throttleTimeMs(e, timeMs).toInt
    assertEquals(0, throttleTimeMs(new QuotaViolationException(metric, 0, 10), time.milliseconds()))
    assertEquals(500, throttleTimeMs(new QuotaViolationException(metric, -5, 10), time.milliseconds()))
    assertEquals(1000, throttleTimeMs(new QuotaViolationException(metric, -10, 10), time.milliseconds()))

// After
  private var lastRecordedTimeMs = 0L
    lastThrottleTimeMs = ControllerMutationQuotaManager.throttleTimeMs(e)
  def throttleTimeMs(e: QuotaViolationException): Long = {
        val throttleTimeMs = ControllerMutationQuotaManager.throttleTimeMs(e).toInt
    assertEquals(0, throttleTimeMs(new QuotaViolationException(metric, 0, 10)))
    assertEquals(500, throttleTimeMs(new QuotaViolationException(metric, -5, 10)))
    assertEquals(1000, throttleTimeMs(new QuotaViolationException(metric, -10, 10)))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19101 remove controllermutationquotamanager#throttletimems unused parameter (#19410).

**Test**
Test modified in ControllerMutationQuotaManagerTest.scala covering the corrected behaviour.
