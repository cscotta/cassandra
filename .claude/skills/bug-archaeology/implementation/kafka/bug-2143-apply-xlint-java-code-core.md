**Commit**: 09d76f917c · **Ticket**: KAFKA-16564
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  addParametersForTests = { name, options ->
    if (name == "compileTestJava" || name == "compileTestScala")
    options.encoding = 'UTF-8'
    options.compilerArgs << "-Xlint:all"
    // temporary exclusions until all the warnings are fixed
    if (!project.path.startsWith(":connect") && !project.path.startsWith(":storage"))
      options.compilerArgs << "-Xlint:-rawtypes"
    options.compilerArgs << "-Xlint:-serial"
    options.compilerArgs << "-Xlint:-try"
    options.compilerArgs << "-Werror"
    options.release = minJavaVersion
    addParametersForTests(name, options)

// After
  configureJavaCompiler = { name, options ->
    if (name == "compileTestJava" || name == "compileTestScala") {
    } else if (name == "compileJava" || name == "compileScala") {
      options.compilerArgs << "-Xlint:all"
      if (!project.path.startsWith(":connect") && !project.path.startsWith(":storage"))
        options.compilerArgs << "-Xlint:-rawtypes"
      options.compilerArgs << "-encoding" << "UTF-8"
      options.compilerArgs << "-Xlint:-rawtypes"
      options.compilerArgs << "-Xlint:-serial"
      options.compilerArgs << "-Xlint:-try"
      options.compilerArgs << "-Werror"
      options.compilerArgs += ["--release", String.valueOf(minJavaVersion)]
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16564 apply `xlint` to java code in core module (#16965).

**Test**
Test modified in SynchronizationTest.java covering the corrected behaviour.
