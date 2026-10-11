**Commit**: 94f5039350 · **Ticket**: KAFKA-17378
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        if (acknowledgeRequestState == null || (!acknowledgeRequestState.onClose() && acknowledgeRequestState.isEmpty())) {
            if (onCommitAsync) {
                isAsyncDone.set(true);
            return Optional.empty();
        } else if (!acknowledgeRequestState.maybeExpire()) {
            if (acknowledgeRequestState.canSendRequest(currentTimeMs)) {
                acknowledgeRequestState.onSendAttempt(currentTimeMs);
                if (onCommitAsync) {
                    isAsyncDone.set(true);
                return Optional.of(acknowledgeRequestState.buildRequest(currentTimeMs));
            } else {
                if (onCommitAsync) {

// After
        boolean asyncDone = true;
        try {
            if (acknowledgeRequestState == null || (!acknowledgeRequestState.onClose() && acknowledgeRequestState.isEmpty())) {
                return Optional.empty();

            if (acknowledgeRequestState.maybeExpire()) {
                // Fill in TimeoutException
                for (TopicIdPartition tip : acknowledgeRequestState.incompleteAcknowledgements.keySet()) {
                    metricsManager.recordFailedAcknowledgements(acknowledgeRequestState.getIncompleteAcknowledgementsCount(tip));
                    acknowledgeRequestState.handleAcknowledgeTimedOut(tip);
                acknowledgeRequestState.incompleteAcknowledgements.clear();
                return Optional.empty();
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17378 fixes for performance testing (#16942).

**Test**
No test added.
