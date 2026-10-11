**Commit**: 2a6f449af7 · **Ticket**: KAFKA-17352
**Subsystem**: settings.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
    id 'com.gradle.enterprise' version '3.14.1'
    id 'com.gradle.common-custom-user-data-gradle-plugin' version '1.11.1'
gradleEnterprise {
        capture { taskInputFiles = true }
        publishAlways()
        publishIfAuthenticated()
    remote(gradleEnterprise.buildCache) {

// After
    id 'com.gradle.develocity' version '3.17.6'
    id 'com.gradle.common-custom-user-data-gradle-plugin' version '2.0.2'
develocity {
    projectId = "kafka"
        publishing.onlyIf { it.authenticated }
    remote(develocity.buildCache) {
```

**How to spot**
When reviewing settings.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17352 migrate from gradle enterprise gradle plugin to develocity gradle plugin (#16902).

**Test**
No test added.
