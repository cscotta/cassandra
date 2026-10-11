**Commit**: ea60666513 · **Ticket**: KAFKA-16921
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  // Remove the relevant project name once it's converted to JUnit 5
  def shouldUseJUnit5 = !(["runtime"].contains(it.project.name))

    if (shouldUseJUnit5)
      useJUnitPlatform()
    if (shouldUseJUnit5) {
      if (project.name == 'streams') {
        useJUnitPlatform {
          includeTags "integration"
          includeTags "org.apache.kafka.test.IntegrationTest"
	  // Both engines are needed to run JUnit 4 tests alongside JUnit 5 tests.
          // junit-vintage (JUnit 4) can be removed once the JUnit 4 migration is complete.

// After
    useJUnitPlatform()
    if (project.name == 'streams') {
      useJUnitPlatform {
        includeTags "integration"
        includeTags "org.apache.kafka.test.IntegrationTest"
        // Both engines are needed to run JUnit 4 tests alongside JUnit 5 tests.
        // junit-vintage (JUnit 4) can be removed once the JUnit 4 migration is complete.
        includeEngines "junit-vintage", "junit-jupiter"
      useJUnitPlatform {
        includeTags "integration"
        includeTags 'org.apache.kafka.test.IntegrationTest'
    if (project.name == 'streams') {
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16921 [1/n] migrate all junit 4 code to junit 5 for connect module (#16253).

**Test**
Test modified in ConnectStandaloneTest.java covering the corrected behaviour.
