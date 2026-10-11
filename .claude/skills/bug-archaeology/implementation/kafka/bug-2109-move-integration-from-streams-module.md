**Commit**: 5a3b544d61 · **Ticket**: KAFKA-17880
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    testImplementation project(':server')
    testImplementation project(':core')
    testImplementation project(':tools')
    testImplementation project(':test-common')
    testImplementation project(':storage')
    testImplementation project(':group-coordinator')
    testImplementation project(':transaction-coordinator')
    testImplementation project(':server-common')
    testImplementation project(':server-common').sourceSets.test.output
    testImplementation project(':server')
    testImplementation project(':group-coordinator')
    testImplementation project(':group-coordinator')

// After
            ':streams:integration-tests',
project(':streams:integration-tests') {
  apply plugin: 'scala'

  base {
    archivesName = "kafka-streams-integration-tests"
  }

  dependencies {
    testImplementation project(':clients').sourceSets.test.output
    testImplementation project(':group-coordinator')
    testImplementation project(':server')
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17880 move integration test from streams module to streams/integration-tests module (#17615).

**Test**
Test modified in AbstractJoinIntegrationTest.java covering the corrected behaviour.
