**Commit**: 979740b49d · **Ticket**: KAFKA-17589
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
This patch completely removes the compile-time dependency on core for both test and main sources by introducing two new modules. 1) `test-common` include all the common test implementation code (including dependency on :core for BrokerServer, ControllerServer, etc)
2) `test-common:api` new sub-module that just includes interfaces including our junit extension

Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
    from (configurations.testRuntimeClasspath) {
      include('slf4j-log4j12*')
    dependsOn('copyDependantLibs')
    from (configurations.testRuntimeClasspath) {
      include('slf4j-log4j12*')
      include('reload4j*jar')
    }
    from (configurations.testRuntimeClasspath) {
      include('slf4j-log4j12*')
      include('reload4j*jar')
    }
    testImplementation project(':core').sourceSets.test.output

// After
    testImplementation project(':test-common')
    testImplementation project(':test-common:test-common-api')
    from (configurations.compileClasspath) {
    dependsOn copyDependantLibs
project(':test-common') {
  base {
    archivesName = "kafka-test-common"
  }

  dependencies {
    implementation libs.scalaJava8Compat
    implementation project(':core')
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17589 move junit extensions to test-common module (#17318).

**Test**
Test modified in EmbeddedKafkaCluster.java covering the corrected behaviour.
