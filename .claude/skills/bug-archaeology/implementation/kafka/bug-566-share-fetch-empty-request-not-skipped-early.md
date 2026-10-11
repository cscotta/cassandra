**Commit**: 3e3f0c4315 · **Ticket**: KAFKA-19946

**Subsystem**: clients/src/main/java/org/apache/kafka/clients/consumer/internals/ShareSessionHandler.java

**Category**: Logic error in condition

**Root cause**
`ShareConsumeRequestManager` built a `ShareFetchRequest` unconditionally for every node with a session handler, then checked after building whether the request was effectively empty (no topics, no forgottenTopics). This late check meant the builder was always invoked even when the result would be discarded. The fix moves the emptiness check into `ShareSessionHandler.newShareFetchBuilder` by passing a `canSkipIfRequestEmpty` flag; when set, the method returns `null` immediately instead of constructing a request object that will never be sent.

**Before**
```java
// Before: build first, check after
ShareFetchRequest.Builder requestBuilder =
    handler.newShareFetchBuilder(groupId, shareFetchConfig);
if (isShareAcquireModeRecordLimit() && target.id() != fetchRecordsNodeId.get()) {
    ShareFetchRequestData data = requestBuilder.data();
    if (data.topics().isEmpty() && data.forgottenTopicsData().isEmpty()) {
        return null;
    }
}
```

**After**
```java
// After: pass skip flag, builder returns null when empty
boolean canSkipIfRequestEmpty = isShareAcquireModeRecordLimit()
    && target.id() != fetchRecordsNodeId.get();
ShareFetchRequest.Builder requestBuilder =
    handler.newShareFetchBuilder(groupId, shareFetchConfig, canSkipIfRequestEmpty);
if (requestBuilder == null) { return null; }
```

**How to spot**
In `ShareConsumeRequestManager`, search for patterns where a request builder is fully constructed before a null/skip guard is evaluated; any case where the guard could have been applied earlier (inside the builder) is a candidate for unnecessary object creation.

**Test**
Add a unit test in `ShareSessionHandlerTest` that calls `newShareFetchBuilder` with `canSkipIfRequestEmpty=true` on an empty session and asserts that `null` is returned without constructing request data.
