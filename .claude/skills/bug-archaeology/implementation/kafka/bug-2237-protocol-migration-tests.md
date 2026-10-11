**Commit**: 48e217608b · **Ticket**: KAFKA-17005
**Subsystem**: core/test (ConsumerProtocolMigrationTest)
**Category**: Missing override of default method

**Root cause**
There were no integration tests covering the KIP-848 online protocol migration path (classic ↔ consumer group protocol). Without these tests, regressions in upgrade/downgrade sequences, eager/cooperative strategy transitions, and static member behaviour during migration could go undetected. The design gap was the absence of test coverage for a complex distributed state machine transition.

**Before/After**
```scala
// Before – no migration integration test existed

// After – comprehensive test class added
class ConsumerProtocolMigrationTest extends BaseConsumerTest {
  def testUpgradeFromClassicToConsumer()
  def testDowngradeFromConsumerToClassic()
  def testBidirectionalMigration()
  def testMigrationWithStaticMembers()
  // ... more scenarios
}
```

**How to spot**
When implementing a stateful migration between two protocol versions, explicitly list all transition paths (upgrade, downgrade, partial, static members, eager vs cooperative) and verify each has at least one integration test before shipping.

**Test**
Entire test class `ConsumerProtocolMigrationTest` is the fix.
