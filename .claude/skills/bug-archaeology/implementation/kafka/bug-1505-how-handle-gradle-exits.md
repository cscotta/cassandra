**Commit**: 617196c68e · **Ticket**: KAFKA-18636
**Subsystem**: .github/scripts
**Category**: Resource leak

**Root cause**
This patch removes the explicit failure of test tasks in Gradle when there is a flaky test. This also fixes a fall-through case in junit.py where we did not recognize an error prior to running the tests (such as the javadoc task). Additionally, this patch removes usages of ignoreFailures in our CI and changes the XML copy task to a finalizer task instead of doLast closure. Reviewers: Jun Rao <junrao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    elif test_exit_code in (0, 1):
            exit(0)
        logger.debug(f"Gradle had unexpected exit code {test_exit_code}. Failing this step")
        exit(1)
  test {
    ext {
      isGithubActions = System.getenv('GITHUB_ACTIONS') != null
      hadFailure = false  // Used to track if any tests failed, see afterSuite below
    ignoreFailures = userIgnoreFailures || ext.isGithubActions
    // As we process results, check if there were any test failures.
    afterSuite { desc, result ->
      if (result.resultType == TestResult.ResultType.FAILURE) {

// After
    elif test_exit_code == 1 or quarantined_test_exit_code == 1:
            logger.debug("There was an error during the test or quarantinedTest task. Please check the logs")
            exit(1)
        # Normal exit
        logger.debug(summary)
        exit(0)
  // Used by :test and :quarantinedTest tasks
  isGithubActions = System.getenv('GITHUB_ACTIONS') != null



  // These two tasks will copy JUnit XML files out of the sub-project's build directory and into
```

**How to spot**
When reviewing .github/scripts code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18636 fix how we handle gradle exits in ci (#18681).

**Test**
No test added.
