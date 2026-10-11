**Commit**: 62379d7d53 · **Ticket**: KAFKA-17479
**Subsystem**: .github/workflows
**Category**: Logic error in condition

**Root cause**
The ignoreFailures property was removed in #17066 to prevent test failures from being cached. However, this breaks the JUnit report and makes the github workflow less user friendly. The problem is that we are copying the junit test report files into a new directory (added in #17098) in a Gradle doLast closure. If we don't run with ignoreFailures=true, then this closure will not run and the test failures won't be processed by junit.py.

**Before/After**
```java
// Before
  userIgnoreFailures = project.hasProperty('ignoreFailures') ? ignoreFailures : false
    ignoreFailures = userIgnoreFailures

    doLast {
      def dest = rootProject.layout.buildDirectory.dir("junit-xml/${project.name}").get().asFile
      println "Copy JUnit XML for ${project.name} to $dest"
      ant.copy(todir: "$dest") {
        ant.fileset(dir: "${test.reports.junitXml.entryPoint}")
      }
    }

// After
        continue-on-error: true
  userIgnoreFailures = project.hasProperty('ignoreFailures') ? ignoreFailures.toBoolean() : false
    ext {
      isGithubActions = System.getenv('GITHUB_ACTIONS') != null
      hadFailure = false  // Used to track if any tests failed, see afterSuite below
    }

    ignoreFailures = userIgnoreFailures || ext.isGithubActions

    // As we process results, check if there were any test failures.
    afterSuite { desc, result ->
      if (result.resultType == TestResult.ResultType.FAILURE) {
```

**How to spot**
When reviewing .github/workflows code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17479 fix ignorefailures logic in ci workflow [3/n] (#17106).

**Test**
No test added.
