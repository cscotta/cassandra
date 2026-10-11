**Commit**: 8c0a0e07ce · **Ticket**: KAFKA-17587
**Subsystem**: .github/scripts
**Category**: Logic error in condition

**Root cause**
This patch reorganizes our test infrastructure into three Gradle modules:

":test-common:test-common-internal-api" is now a minimal dependency which exposes interfaces and annotations only. It has one project dependency on server-common to expose commonly used data classes (MetadataVersion, Feature, etc). Since this pulls in server-common, this module is Java 17+. It cannot be used by ":clients" or other Java 11 modules.

**Before/After**
```java
// Before
    return m.group(1)
  modulesNeedingJava11 = [":clients", ":generator", ":streams", ":streams:test-utils", ":streams-scala", ":test-common:test-common-runtime"]
    project(":test-common:test-common-runtime")
  log4jRuntimeLibs = [
    releaseOnly log4jRuntimeLibs
    testImplementation project(':test-common')
    testImplementation project(':test-common:test-common-api')
project(':test-common') {
  // Test framework stuff. Implementations that support test-common-api
    archivesName = "kafka-test-common"
    implementation project(':core')
    implementation project(':metadata')

// After
    if m is None:
        raise ValueError(f"Could not parse test name '{test_name}'. Expected a valid Java method name.")
    else:
        return m.group(1)
  modulesNeedingJava11 = [":clients", ":generator", ":streams", ":streams:test-utils", ":streams-scala", ":test-common:test-common-util"]
    libs.jacksonDatabindYaml,
    project(":test-common:test-common-util")
  log4jReleaseLibs = [
    releaseOnly log4jReleaseLibs
    testImplementation project(':test-common:test-common-runtime')
    testImplementation project(':test-common:test-common-internal-api')
    testImplementation project(':test-common:test-common-util')
```

**How to spot**
When reviewing .github/scripts code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17587 refactor test infrastructure (#18602).

**Test**
Test modified in import-control-test-common-internal-api.xml covering the corrected behaviour.
