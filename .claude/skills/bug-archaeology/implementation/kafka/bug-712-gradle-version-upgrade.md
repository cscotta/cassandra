**Commit**: c6bbbbe24d · **Ticket**: KAFKA-19174
**Subsystem**: .github/actions
**Category**: Logic error in condition

**Root cause**
List of changes:
- prerequisite Jira ticket:
  - [KAFKA-19591](https://issues.apache.org/jira/browse/KAFKA-19591)
- mandatory version upgrades:
  - Gradle version: 8.14.3 -->> 9.1.0
  - Gradle Shadow plugin: 8.3.6 -->> 8.3.9
  - Gradle dependencycheck plugin: 8.2.1 -->> 12.1.3
  - Gradle spotbugs plugin: 6.2.3 -->> 6.2.5
  - Gradle spotless plugin: 6.25.0 -->> 7.2.1
- build logic will be refactored to accommodate Gradle 9 breaking
changes
- (optional): a dozen of Gradle plugins versions will also be upgraded
- other JIRA tickets that had to be solved all along:
  - [KAFKA-16801](https://issues.apache.org/jira/browse/KAFKA-16801)
  - [KAFKA-19654](https://issues.apache.org/jira/browse/KAFKA-1...

**Before/After**
```java
// Before
      uses: gradle/actions/setup-gradle@94baf225fe0a508e581a564467443d0e2379123b # v4.3.0
  id 'com.github.ben-manes.versions' version '0.48.0'
  id 'org.owasp.dependencycheck' version '8.2.1'
  id "com.github.spotbugs" version '6.2.3' apply false
  id 'com.gradleup.shadow' version '8.3.6' apply false
  id 'com.diffplug.spotless' version "6.25.0"
  gradleVersion = versions.gradle
println("Starting build with version $version (commit id ${commitId == null ? "null" : commitId.take(8)}) using Gradle $gradleVersion, Java ${JavaVersion.current()} and Scala ${versions.scala}")
  gradle: "8.14.3",
  zinc: "1.9.2",
distributionSha256Sum=bd71102213493060956ec229d946beee57158dbd89d0e62b91bca0fa2c5f3531
distributionUrl=https\://services.gradle.org/distributions/gradle-8.14.3-bin.zip

// After
      uses: gradle/actions/setup-gradle@748248ddd2a24f49513d8f472f81c3a07d4d50e1 # v4.4.4
  id 'com.github.ben-manes.versions' version '0.52.0'
  id 'org.owasp.dependencycheck' version '12.1.3'
  id "com.github.spotbugs" version '6.2.5' apply false
  id 'com.gradleup.shadow' version '8.3.9' apply false
  id 'com.diffplug.spotless' version "7.2.1"
println("Starting build with version $version (commit id ${commitId == null ? "null" : commitId.take(8)}) using Gradle $versions.gradle, Java ${JavaVersion.current()} and Scala ${versions.scala}")
  tasks.withType(AbstractArchiveTask).configureEach {
    reproducibleFileOrder = false
    preserveFileTimestamps = true
    useFileSystemPermissions()
  }
```

**How to spot**
When reviewing .github/actions code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19174 gradle version upgrade 8 -->> 9 (#19513).

**Test**
No test added.
