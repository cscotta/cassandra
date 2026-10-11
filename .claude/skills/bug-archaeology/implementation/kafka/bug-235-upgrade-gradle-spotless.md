**Commit**: 7aaf4de820 · **Ticket**: KAFKA-20182
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Upgrades Gradle from 9.2.1 to 9.4.1 and the Spotless plugin from 8.0.0
to 8.3.0. The upgrade was previously blocked because Spotless' default
`removeUnusedImports()` relies on google-java-format, which depends on
`com.sun.tools.javac.*` internals and breaks under the newer
Gradle/Spotless worker setup. Worked around by switching to the
`cleanthat-javaparser-unnecessaryimport` engine, which uses JavaParser
and does not touch JDK internals:

```
removeUnusedImports('cleanthat-javaparser-unnecessaryimport')
```

The wrapper files (`gradle/wrapper/gradle-wrapper.properties`,
`gradlew`, `wrapper.gradle`) were regenerated following the procedure
documented in `gradle/wrapper/README.md`, using the ...

**Before/After**
```java
// Before
  id 'com.diffplug.spotless' version "8.0.0"
      removeUnusedImports()
  gradle: "9.2.1",
distributionSha256Sum=72f44c9f8ebcb1af43838f45ee5c4aa9c5444898b3468ab3f4af7b6076c5bc3f
distributionUrl=https\://services.gradle.org/distributions/gradle-9.2.1-bin.zip
#       https://github.com/gradle/gradle/blob/HEAD/platforms/jvm/plugins-application/src/main/resources/org/gradle/api/internal/plugins/unixStartScript.txt
REQUIRED_WRAPPER_JAR_CHECKSUM="423cb469ccc0ecc31f0e4e1c309976198ccb734cdcbb7029d4bda0f18f57e8d9"
    if ! curl -s -S --retry 3 -L -o "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "https://raw.githubusercontent.com/gradle/gradle/v9.2.1/gradle/wrapper/gradle-wrapper.jar"; then

// After
  id 'com.diffplug.spotless' version "8.3.0"
      // The default engine (google-java-format) uses the JDK compiler's internal
      // parser (com.sun.tools.javac.*), which JPMS blocks without --add-exports.
      // Gradle 9.3+ / Spotless 8.x no longer pass those exports to the worker JVM,
      // so the default engine fails at runtime. The cleanthat engine uses the
      // JavaParser library instead, which ships its own Java parser and does not
      // rely on any JDK internal classes, so it works out of the box.
      // See KAFKA-20182.
      removeUnusedImports('cleanthat-javaparser-unnecessaryimport')
  gradle: "9.4.1",
distributionSha256Sum=2ab2958f2a1e51120c326cad6f385153bb11ee93b3c216c5fccebfdfbb7ec6cb
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.1-bin.zip
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20182 upgrade gradle to 9.4.1 and spotless to 8.3.0 (#22012).

**Test**
No test added.
