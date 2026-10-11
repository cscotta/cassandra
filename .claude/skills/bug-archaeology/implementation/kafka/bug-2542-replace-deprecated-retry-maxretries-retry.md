**Commit**: d28ddcb9f4 · **Ticket**: KAFKA-17357
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    retry {
      maxRetries = userMaxTestRetries
      maxFailures = userMaxTestRetryFailures
    retry {
      maxRetries = userMaxTestRetries
      maxFailures = userMaxTestRetryFailures
    retry {
      maxRetries = userMaxTestRetries
      maxFailures = userMaxTestRetryFailures

// After
    develocity {
      testRetry {
        maxRetries = userMaxTestRetries
        maxFailures = userMaxTestRetryFailures
      }
    develocity {
      testRetry {
        maxRetries = userMaxTestRetries
        maxFailures = userMaxTestRetryFailures
      }
    develocity {
      testRetry {
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17357 replace the deprecated `retry.maxretries` / retry.maxfailures` (#16913).

**Test**
No test added.
