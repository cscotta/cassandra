**Commit**: 7c34cf6e2c · **Ticket**: KAFKA-17664
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
What
Currently, we prepare a ShareAcknowledgeCommitCallback event for every ShareAcknowledgeResponse and send it over to the application thread. In cases where the acknowledgement commit callback handler is not configured by the user, this event is not used in the application thread. So we can generate this event based on whether the callback was configured. In this PR, we have a new event which the application thread sends whenever the user enables or disables the commit callback handler, thereby letting the ShareConsumeRequestManager know if it has to send the background event or not.

**Before/After**
```java
// Before
                    ShareAcknowledgementCommitCallbackEvent event = new ShareAcknowledgementCommitCallbackEvent(acksMap);
                    backgroundEventHandler.add(event);
                    ShareAcknowledgementCommitCallbackEvent event = new ShareAcknowledgementCommitCallbackEvent(acksMap);
                    backgroundEventHandler.add(event);
                    acknowledgeRequestState.processingComplete();
                ShareAcknowledgementCommitCallbackEvent event = new ShareAcknowledgementCommitCallbackEvent(result);
                backgroundEventHandler.add(event);

// After
    private boolean isAcknowledgementCommitCallbackRegistered = false;

    public void setAcknowledgementCommitCallbackRegistered(boolean isAcknowledgementCommitCallbackRegistered) {
        this.isAcknowledgementCommitCallbackRegistered = isAcknowledgementCommitCallbackRegistered;
    }

    private void maybeSendShareAcknowledgeCommitCallbackEvent(Map<TopicIdPartition, Acknowledgements> acknowledgementsMap) {
        if (isAcknowledgementCommitCallbackRegistered) {
            ShareAcknowledgementCommitCallbackEvent event = new ShareAcknowledgementCommitCallbackEvent(acknowledgementsMap);
            backgroundEventHandler.add(event);
        }
    }
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17664: added check before sending background event after shareacknowledge (#17332).

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
