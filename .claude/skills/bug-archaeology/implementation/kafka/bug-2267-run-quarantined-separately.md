**Commit**: d38a90df2b · **Ticket**: KAFKA-17672
**Subsystem**: .github/workflows
**Category**: Logic error in condition

**Root cause**
Introduce new quarantinedTest that excludes tests tagged with "flaky". Also introduce two new build parameters "maxQuarantineTestRetries" and "maxQuarantineTestRetryFailures". Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
          -PmaxTestRetries=1 -PmaxTestRetryFailures=10 \
          test
      - name: Archive JUnit reports
            **/build/reports/tests/test/*
          ${{ inputs.test-module }}:test --tests ${{ inputs.test-pattern }}
    ./gradlew test # runs both unit and integration tests
    ./gradlew test --rerun
    ./gradlew unitTest --rerun
    ./gradlew integrationTest --rerun
By default, each failed test is retried once up to a maximum of five retries per test run. Tests are retried at the end of the test task. Adjust these parameters in the following way:
    ./gradlew test -PmaxTestRetries=1 -PmaxTestRetryFailures=5


// After
          -PmaxTestRetries=1 -PmaxTestRetryFailures=3 \
          -PmaxQuarantineTestRetries=3 -PmaxQuarantineTestRetryFailures=0 \
          quarantinedTest test
      - name: Archive JUnit HTML reports
            **/build/reports/tests/*
          -PmaxTestRetries=${{ inputs.test-repeat }} -PmaxTestRetryFailures=0 \
          -PmaxQuarantineTestRetries=${{ inputs.test-repeat }} -PmaxQuarantineTestRetryFailures=0 \
          ${{ inputs.test-module }}:test ${{ inputs.test-module }}:quarantinedTest --tests ${{ inputs.test-pattern }}
    ./gradlew test  # runs both unit and integration tests
    ./gradlew quarantinedTest  # runs the quarantined tests

    ./gradlew test --rerun-tasks
```

**How to spot**
When reviewing .github/workflows code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17672 run quarantined tests separately (#17329).

**Test**
Test modified in OffsetsApiIntegrationTest.java covering the corrected behaviour.
