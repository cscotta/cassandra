**Commit**: 8107bda695 · **Ticket**: KAFKA-17654
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: 陳昱霖(Yu-Lin Chen) <chenyulin0719@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        requestInFlight.compareAndSet(false, true) ) {


    verifyFailure(manager)

// After
        requestInFlight.compareAndSet(false, true)) {
    // two block requests are sent in this case:
    // 1. the first generateProducerId(), there is no Producer ID available.
    // 2. the second generateProducerId(), the second block request will fail.
    verifyFailureWithoutGenerateProducerId(manager)
    verifyFailureWithoutGenerateProducerId(manager)
  }

  private def verifyFailureWithoutGenerateProducerId(manager: MockProducerIdManager): Unit = {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17654 fix flaky produceridmanagertest#testunrecoverableerrors (#17361).

**Test**
Test modified in ProducerIdManagerTest.scala covering the corrected behaviour.
