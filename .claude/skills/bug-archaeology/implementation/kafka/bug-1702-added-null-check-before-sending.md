**Commit**: 3c7ed3333d · **Ticket**: KAFKA-18397
**Subsystem**: clients/src
**Category**: Missing null / bounds check

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
            if (acknowledgements != null) {
                maybeSendShareAcknowledgeCommitCallbackEvent(Collections.singletonMap(partition, acknowledgements));

// After
            if (!isCommitAsync && acknowledgements != null) {
                if (acknowledgements != null) {
                    maybeSendShareAcknowledgeCommitCallbackEvent(Collections.singletonMap(partition, acknowledgements));
                }
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
    @Test
    public void testResultHandlerOnCommitAsync() {
        buildRequestManager();
        // Enabling the config so that background event is sent when the acknowledgement response is received.
        shareConsumeRequestManager.setAcknowledgementCommitCallbackRegistered(true);

```

**How to spot**
When reviewing clients/src code, look for missing null / bounds check patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18397: added null check before sending background event from shareconsumerequestmanager. (#18419).

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
