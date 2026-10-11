**Commit**: cb33e98dfc · **Ticket**: KAFKA-18748
**Subsystem**: .github/actions
**Category**: Logic error in condition

**Root cause**
Split the JUnit tests into "new", "flaky", and the remainder. On PR builds, "new" tests are anything that do not exist on trunk. They are run with zero tolerance for flakiness. On trunk builds, "new" tests are anything added in the last 7 days.

**Before/After**
```java
// Before
    description: "The test suite to run. Either 'test' or 'quarantinedTest'."
        -PmaxTestRetries=1 -PmaxTestRetryFailures=10 \
        -PmaxQuarantineTestRetries=3 -PmaxQuarantineTestRetryFailures=0 \
        build/junit-xml/module[/sub-module]/[task]/TEST-class.method.xml
    This method strips off a base path and assumes all path segments leading up to the suite name
    Returns a tuple of (module, task)

// After
    description: "The Gradle task name to run."
    required: true
  test-xml-output:
    description: "Output directory for JUnit XML results"
  test-retries:
    description: "The number of retries for a given test should we allow"
    required: true
    default: "0"
  test-repeat:
    description: "The number of times to repeat the integration tests"
    required: true
    default: "1"
```

**How to spot**
When reviewing .github/actions code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18748 run new tests separately in prs (#18770).

**Test**
Test modified in Flaky.java covering the corrected behaviour.
