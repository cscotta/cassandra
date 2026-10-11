**Commit**: 042da16fd6 · **Ticket**: KAFKA-18557
**Subsystem**: share-coordinator/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
            ShareCoordinatorTestConfig.createConfig(ShareCoordinatorTestConfig.testConfigMap()),
            ShareCoordinatorTestConfig.createConfig(ShareCoordinatorTestConfig.testConfigMap()),
            ShareCoordinatorTestConfig.createConfig(ShareCoordinatorTestConfig.testConfigMap()),
            ShareCoordinatorTestConfig.createConfig(ShareCoordinatorTestConfig.testConfigMap()),
            ShareCoordinatorTestConfig.createConfig(ShareCoordinatorTestConfig.testConfigMap()),
            ShareCoordinatorTestConfig.createConfig(ShareCoordinatorTestConfig.testConfigMap()),
            ShareCoordinatorTestConfig.createConfig(ShareCoordinatorTestConfig.testConfigMap()),
            ShareCoordinatorTestConfig.createConfig(ShareCoordinatorTestConfig.testConfigMap()),
            ShareCoordinatorTestConfig.createConfig(ShareCoordinatorTestConfig.testConfigMap()),
            ShareCoordinatorTestConfig.createConfig(ShareCoordinatorTestConfig.testConfigMap()),
            ShareCoordinatorTestConfig.createConfig(ShareCoordinatorTestConfig.

// After
            ShareCoordinatorTestConfig.testConfig(),
            ShareCoordinatorTestConfig.testConfig(),
            ShareCoordinatorTestConfig.testConfig(),
            ShareCoordinatorTestConfig.testConfig(),
            ShareCoordinatorTestConfig.testConfig(),
            ShareCoordinatorTestConfig.testConfig(),
            ShareCoordinatorTestConfig.testConfig(),
            ShareCoordinatorTestConfig.testConfig(),
            ShareCoordinatorTestConfig.testConfig(),
            ShareCoordinatorTestConfig.testConfig(),
```

**How to spot**
When reviewing share-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18557 streamline codebase with testconfig() (#18582).

**Test**
Test modified in ShareCoordinatorServiceTest.java covering the corrected behaviour.
