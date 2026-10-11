**Commit**: f6a78c4c2b · **Ticket**: KAFKA-19246
**Subsystem**: clients / OffsetFetchResponse; core / KafkaApis; group-coordinator / GroupCoordinatorService
**Category**: Wrong serialization / deserialization

**Root cause**
`OffsetFetchResponse` version 1 (and version 0) does not have a top-level error code field; errors must be propagated at the per-partition level. The response construction logic in `KafkaApis` and `GroupCoordinatorService` was setting `errorCode` on the `OffsetFetchResponseGroup` object without checking whether that field exists in the requested API version. For version 1 clients the top-level error code was silently dropped; the caller received success with `INVALID_OFFSET` partitions instead of the actual error.

**Before/After**
```java
// Before — in KafkaApis, sets group-level error regardless of version
futures += CompletableFuture.completedFuture(
    new OffsetFetchResponseData.OffsetFetchResponseGroup()
        .setGroupId(groupOffsetFetch.groupId)
        .setErrorCode(Errors.GROUP_AUTHORIZATION_FAILED.code))

// After — delegates to a helper that puts the error at the right level
futures += CompletableFuture.completedFuture(
    OffsetFetchResponse.groupError(groupOffsetFetch,
        Errors.GROUP_AUTHORIZATION_FAILED, request.header.apiVersion()))
```

**How to spot**
Whenever an API response has a version-dependent error representation (top-level vs. per-partition), a single code path that always writes to the top-level field will silently swallow errors for older clients. Search for `setErrorCode` calls in response builders that do not condition on `version < TOP_LEVEL_ERROR_MIN_VERSION`.

**Test**
`OffsetFetchResponseTest.testSingleGroupWithError` — parameterized over all OffsetFetch API versions; for version < 2 asserts the error appears in the per-partition `errorCode`, for version >= 2 asserts the group-level `errorCode`.
