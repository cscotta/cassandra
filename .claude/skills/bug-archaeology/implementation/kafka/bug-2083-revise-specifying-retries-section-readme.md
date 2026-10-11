**Commit**: bc3e6369b9 · **Ticket**: KAFKA-17931
**Subsystem**: README.md
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
By default, each failed test is retried once up to a maximum of three total retries per test run.
Tests are retried at the end of the test task. Adjust these parameters in the following way:
Additionally, quarantined tests are automatically retried three times up to a total of
20 retries per run. This is controlled by similar parameters.
    ./gradlew test -PmaxQuarantineTestRetries=3 -PmaxQuarantineTestRetryFailures=20

// After
Retries are disabled by default, but you can set maxTestRetryFailures and maxTestRetries to enable retries.

The following example declares -PmaxTestRetries=1 and -PmaxTestRetryFailures=3 to enable a failed test to be retried once, with a total retry limit of 3.
The quarantinedTest task also has no retries by default, but you can set maxQuarantineTestRetries and maxQuarantineTestRetryFailures to enable retries, similar to the test task.
    ./gradlew quarantinedTest -PmaxQuarantineTestRetries=3 -PmaxQuarantineTestRetryFailures=20
```

**How to spot**
When reviewing README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17931 revise the `specifying-test-retries` section in readme (#17679).

**Test**
No test added.
