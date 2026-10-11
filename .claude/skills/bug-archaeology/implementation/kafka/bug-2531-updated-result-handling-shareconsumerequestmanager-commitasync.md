**Commit**: 932e84096a · **Ticket**: KAFKA-17325
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Currently we were not updating the result count when we merged commitAsync() requests into one batch in ShareConsumeRequestManager, so this led to lesser acknowledgements sent to the application thread (ShareConsumerImpl) than expected. Fix : Now if the acknowledge response came from a commitAsync, then we do not wait for other requests to complete, we always prepare a background event to be sent. This PR also fixes a bug in ShareConsumeRequestManager, where during the final ShareAcknowledge sent during close(), we also pick up any piggybacked acknowledgements which were waiting to be sent along with ShareFetch. Reviewers:  Andrew Schofield <aschofield@confluent.io>,  Manikumar Reddy <maniku...

**Before/After**
```java
// Before
        if (acknowledgeRequestState == null || (!acknowledgeRequestState.onClose && acknowledgeRequestState.isEmpty())) {
                            resultHandler
        final AtomicInteger resultCount = new AtomicInteger();
        final ResultHandler resultHandler = new ResultHandler(resultCount, Optional.empty());
                        resultCount.incrementAndGet();
                                    resultHandler
                    Acknowledgements acknowledgements = acknowledgementsMap.get(tip);
                    if (acknowledgements != null) {

// After
import java.util.Locale;
        if (acknowledgeRequestState == null || (!acknowledgeRequestState.onClose() && acknowledgeRequestState.isEmpty())) {
                            resultHandler,
                            AcknowledgeRequestType.COMMIT_SYNC
        final ResultHandler resultHandler = new ResultHandler(Optional.empty());
                                    resultHandler,
                                    AcknowledgeRequestType.COMMIT_ASYNC
                    Acknowledgements acknowledgements = acknowledgementsMap.getOrDefault(tip, Acknowledgements.empty());

                    if (fetchAcknowledgementsMap.get(tip) != null) {
                        acknowledgements.merge(fetchAcknowledgementsMap.remove(tip));
                    }
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17325: updated result handling in shareconsumerequestmanager::commitasync(). (#16903).

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
