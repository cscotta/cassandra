**Commit**: d55d0e1a8f · **Ticket**: KAFKA-17349
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  def testZkControllerId(): Unit = {
  def testZkControllerIdWhenZnodeIsDeleted(): Unit = {

// After
  def testZkControllerId(quorum: String): Unit = {
  def testZkControllerIdWhenZnodeIsDeleted(quorum: String): Unit = {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17349 make sure all it have "quorum" input arguments (#16896).

**Test**
Test modified in ControllerIdMetricTest.scala covering the corrected behaviour.
