**Commit**: 3479ce793b · **Ticket**: KAFKA-18202
**Subsystem**: storage/src
**Category**: State not cleaned up

**Root cause**
This change handles rejecting non-zero sequences when there is an empty
producerIDState with TV2. The scenario will be covered with the
re-triable OutOfOrderSequence error. For Transactions V2 with empty state:  ✅ Allow only sequence 0 is allowed for
new producers or after state cleanup (new validation added)  ❌ Don't allow any
non-zero sequence is rejected with our specific error message  ❌ Don't allow any epoch
bumps still require sequence 0 (existing validation remains)

For Transactions V1 with empty state:  ✅ Allow ANY sequence number is allowed
(0, 5, 100, etc.)  ❌ Don't allow epoch bumps still require sequence 0 (existing
validation)

Reviewers: Justine Olshan <jolshan@confluent.io>, ...

**Before/After**
```java
// Before
  def testTransactionIsOngoingAndVerificationGuard(appendOrigin: AppendOrigin): Unit = {
    var sequence = if (appendOrigin == AppendOrigin.CLIENT) 3 else 0

// After
  def testTransactionIsOngoingAndVerificationGuardTV2(appendOrigin: AppendOrigin): Unit = {
    // For TV2, when there's no existing producer state, sequence must be 0 for both CLIENT and COORDINATOR
    var sequence = 0
  @ParameterizedTest
  @EnumSource(value = classOf[AppendOrigin], names = Array("CLIENT", "COORDINATOR"))
  def testTransactionIsOngoingAndVerificationGuardTV1(appendOrigin: AppendOrigin): Unit = {
    val producerStateManagerConfig = new ProducerStateManagerConfig(86400000, false)

    val producerId = 23L
    val producerEpoch = 1.toShort
    // For TV1, can start with non-zero sequences even with non-zero epoch when no existing producer state
    var sequence = if (appendOrigin == AppendOrigin.CLIENT) 3 else 0
```

**How to spot**
When reviewing storage/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18202: add rejection for non-zero sequences in tv2 (kip-890) (#19902).

**Test**
Test modified in UnifiedLogTest.scala covering the corrected behaviour.
