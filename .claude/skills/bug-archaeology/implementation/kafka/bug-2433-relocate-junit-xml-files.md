**Commit**: 84aa5d7a63 · **Ticket**: KAFKA-17479
**Subsystem**: .github/scripts
**Category**: Resource leak

**Root cause**
Recently, we fixed caching for ":jar" and ":test" tasks. A side effect of this is that the test results will be restored as part of the Gradle cache resolution. This means test tasks which are skipped (as a result of FROM-CACHE) will still have test results in their build directory. To avoid incorrectly reporting these results in the job summary, this patch uses a doLast task handler to relocate JUnit XML files into a new directory.

**Before/After**
```java
// Before
                        default="**/test-results/**/*.xml",
        continue-on-error: true

// After
                        default="build/junit-xml/**/*.xml",

    doLast {
      def dest = rootProject.layout.buildDirectory.dir("junit-xml/${project.name}").get().asFile
      println "Copy JUnit XML for ${project.name} to $dest"
      ant.copy(todir: "$dest") {
        ant.fileset(dir: "${test.reports.junitXml.entryPoint}")
      }
    }
```

**How to spot**
When reviewing .github/scripts code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17479 relocate junit xml files [2/n] (#17098).

**Test**
No test added.
