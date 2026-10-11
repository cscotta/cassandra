**Commit**: 4aa81204ff · **Ticket**: KAFKA-19018
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
PR add `MaxRecords` to share fetch request and also adds
`AcquisitionLockTimeout` to share fetch response. PR also removes
internal broker config of `max.fetch.records`. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
                added, removed, acknowledgementBatches);
                                          int maxWait, int minBytes, int maxBytes, int batchSize,
                                          List<TopicIdPartition> send, List<TopicIdPartition> forget,
        ShareFetchResponseData data = toMessage(Errors.NONE, 0, partIterator, Collections.emptyList());
                                        List<Node> nodeEndpoints) {
        return new ShareFetchResponse(toMessage(error, throttleTimeMs, responseData.entrySet().iterator(), nodeEndpoints));
                                                   List<Node> nodeEndpoints) {

// After
                fetchConfig.maxPollRecords, added, removed, acknowledgementBatches);
                                          int maxWait, int minBytes, int maxBytes, int maxRecords,
                                          int batchSize, List<TopicIdPartition> send, List<TopicIdPartition> forget,
            data.setMaxRecords(maxRecords);
        ShareFetchResponseData data = toMessage(Errors.NONE, 0, partIterator, Collections.emptyList(), 0);
                                        List<Node> nodeEndpoints, int acquisitionLockTimeout) {
        return new ShareFetchResponse(toMessage(error, throttleTimeMs, responseData.entrySet().iterator(), nodeEndpoints, acquisitionLockTimeout));
                                                   List<Node> nodeEndpoints, int acquisitionLockTimeout) {
                .setAcquisitionLockTimeoutMs(acquisitionLockTimeout)
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19018,kafka-19063: implement maxrecords and acquisition lock timeout in share fetch request and response resp. (#1.

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
