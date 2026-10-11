**Commit**: 92672d1df8 · **Ticket**: KAFKA-17470
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
    private class OffsetCommitRequestState extends RetriableRequestState {
                    onFailedAttempt(currentTimeMs);
                    onFailedAttempt(currentTimeMs);

// After
    class OffsetCommitRequestState extends RetriableRequestState {
            boolean failedRequestRegistered = false;
                    if (!failedRequestRegistered) {
                        onFailedAttempt(currentTimeMs);
                        failedRequestRegistered = true;
                    }

            boolean failedRequestRegistered = false;
                    if (!failedRequestRegistered) {
                        onFailedAttempt(currentTimeMs);
                        failedRequestRegistered = true;
                    }
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17470: commitrequestmanager should record failed request only once even if multiple errors in response (#17109).

**Test**
Test modified in CommitRequestManagerTest.java covering the corrected behaviour.
