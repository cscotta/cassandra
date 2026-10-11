**Commit**: bbcc297e37 · **Ticket**: KAFKA-18358
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Divij Vaidya <diviv@amazon.com>

**Before/After**
```java
// Before
      break;
    def receiptFile = file("$buildDir/kafka/$buildVersionFileName")
    from("$buildDir") {
    delete "$buildDir/kafka/"
    into "$buildDir/dependant-libs-${versions.scala}"
    into "$buildDir/dependant-testlibs"
    from (project(':clients').testJar ) { "$buildDir/dependant-testlibs" }
    def receiptFile = file("$buildDir/kafka/$buildVersionFileName")
    from("$buildDir") {
    delete "$buildDir/kafka/"
    def receiptFile = file("$buildDir/kafka/$buildVersionFileName")
    from("$buildDir") {

// After
      break
    def receiptFile = file("${layout.buildDirectory.get().asFile.path}/kafka/$buildVersionFileName")
    from("${layout.buildDirectory.get().asFile.path}") {
    delete "${layout.buildDirectory.get().asFile.path}/kafka/"
    into "${layout.buildDirectory.get().asFile.path}/dependant-libs-${versions.scala}"
    into "${layout.buildDirectory.get().asFile.path}/dependant-testlibs"
    from (project(':clients').testJar ) { "${layout.buildDirectory.get().asFile.path}/dependant-testlibs" }
    def receiptFile = file("${layout.buildDirectory.get().asFile.path}/kafka/$buildVersionFileName")
    from("${layout.buildDirectory.get().asFile.path}") {
    delete "${layout.buildDirectory.get().asFile.path}/kafka/"
    def receiptFile = file("${layout.buildDirectory.get().asFile.path}/kafka/$buildVersionFileName")
    from("${layout.buildDirectory.get().asFile.path}") {
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18358: replace deprecated $builddir variable in build.gradle  (#18343).

**Test**
No test added.
