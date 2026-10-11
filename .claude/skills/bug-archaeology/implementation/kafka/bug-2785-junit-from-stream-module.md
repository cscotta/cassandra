**Commit**: 9b4f13efbc · **Ticket**: KAFKA-15623
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    useJUnitPlatform()
    if (project.name == 'streams') {
      useJUnitPlatform {
        includeTags "integration"
        includeTags "org.apache.kafka.test.IntegrationTest"
        // Both engines are needed to run JUnit 4 tests alongside JUnit 5 tests.
        // junit-vintage (JUnit 4) can be removed once the JUnit 4 migration is complete.
        includeEngines "junit-vintage", "junit-jupiter"
      }
    } else {
      useJUnitPlatform {
        includeTags "integration"

// After
    useJUnitPlatform {
      includeEngines 'junit-jupiter'
    }
    useJUnitPlatform {
      includeTags "integration"
      includeEngines 'junit-jupiter'
    useJUnitPlatform {
      excludeTags "integration"
      includeEngines 'junit-jupiter'
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
@Suite
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15623 remove junit 4 from stream module  (#16447).

**Test**
Test modified in IntegrationTest.java covering the corrected behaviour.
