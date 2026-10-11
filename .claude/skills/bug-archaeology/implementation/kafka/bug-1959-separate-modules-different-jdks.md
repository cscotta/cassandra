**Commit**: 056a76e2b9 · **Ticket**: KAFKA-17811
**Subsystem**: .github/workflows
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        java: [ 23, 11 ]  # If we change these, make sure to adjust ci-complete.yml
        java: [ 23, 11 ]
  minJavaVersion = 11
  configureJavaCompiler = { name, options ->
    if (name == "compileTestJava" || name == "compileTestScala") {
      options.compilerArgs += ["--release", String.valueOf(minJavaVersion)]
    } else if (name == "compileJava" || name == "compileScala") {
      options.compilerArgs << "-Xlint:all"
      options.compilerArgs << "-encoding" << "UTF-8"
      options.compilerArgs << "-Xlint:-rawtypes"
      options.compilerArgs += ["--release", String.valueOf(minJavaVersion)]
    configureJavaCompiler(name, options)

// After
        java: [ 23, 17 ]  # If we change these, make sure to adjust ci-complete.yml
        java: [ 23, 17 ]
  minClientJavaVersion = 11
  minNonClientJavaVersion = 17
  // The connect:api module also belongs to the clients module, but it has already been bumped to JDK 17 as part of KIP-1032.
  modulesNeedingJava11 = [":clients", ":streams", ":streams:test-utils", ":streams-scala", ":test-common:test-common-runtime"]

  configureJavaCompiler = { name, options, projectPath ->

    def releaseVersion = modulesNeedingJava11.any { projectPath == it } ? minClientJavaVersion : minNonClientJavaVersion

    options.compilerArgs << "-encoding" << "UTF-8"
```

**How to spot**
When reviewing .github/workflows code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17811 separate modules to use different jdks (#17522).

**Test**
Test modified in SynchronizationTest.java covering the corrected behaviour.
