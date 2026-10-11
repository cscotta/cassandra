**Commit**: 8a90ca0528 · **Ticket**: KAFKA-18076
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    if (isZkMigrationTest()) {
      cfgs.foreach(_.setProperty(KRaftConfigs.MIGRATION_ENABLED_CONFIG, "true"))
    }
  def isZkMigrationTest(): Boolean = {
    TestInfoUtils.isZkMigrationTest(testInfo)
  }

  def isZkMigrationTest(testInfo: TestInfo): Boolean = {
    if (!isKRaft(testInfo)) {
      false
    } else {
      testInfo.getDisplayName.contains("quorum=zkMigration")

// After
        enableZkApiForwarding = config.migrationEnabled && config.interBrokerProtocolVersion.isApiForwardingEnabled
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18076 remove `iszkmigrationtest` and related code (#17922).

**Test**
Test modified in IntegrationTestHarness.scala covering the corrected behaviour.
