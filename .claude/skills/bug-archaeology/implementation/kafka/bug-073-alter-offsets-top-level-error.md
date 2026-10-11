**Commit**: 729f9ccf06 · **Ticket**: KAFKA-19440
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/admin/internals/AlterShareGroupOffsetsHandler.java
**Category**: Incorrect filtering / wrong query result

**Root cause**
`AlterShareGroupOffsetsHandler.handleResponse()` only processed per-partition error codes from the response body. If the broker returned a top-level error (e.g., `NOT_COORDINATOR` or `GROUP_ID_NOT_FOUND`) in `response.data().errorCode()`, the handler ignored it and returned an empty success result — making the caller believe all partition offset alterations succeeded when the entire RPC actually failed. The internal representation was also changed from `Map<TopicPartition, Errors>` to `Map<TopicPartition, ApiException>` so that `null` means "no error" (matching the convention used by other Admin handlers) instead of relying on `Errors.NONE` comparison.

**Before/After**
```java
// Before: top-level error code ignored; per-partition loop only
for (AlterShareGroupOffsetsResponseData.AlterShareGroupOffsetsResponseTopic topic : response.data().responses()) {
    for (...partition...) {
        Errors error = Errors.forCode(partition.errorCode());
        ...
    }
}
// response.data().errorCode() never read

// After: top-level error checked first
if (response.data().errorCode() != Errors.NONE.code()) {
    final Errors topLevelError = Errors.forCode(response.data().errorCode());
    // propagate to all requested partitions
    ...
}
```

**How to spot**
Admin RPC handlers that only process the per-partition or per-topic sub-list of a response without first checking the top-level `errorCode` field will silently succeed on broker-level failures. Every `handleResponse()` implementation should check `response.data().errorCode()` before entering partition-level loops.

**Test**
`AlterShareGroupOffsetsHandlerTest` was updated to include test cases for top-level errors, verifying that coordinator redirect (NOT_COORDINATOR) and other top-level errors propagate correctly to the returned futures.
