**Commit**: 0fde28049a · **Ticket**: KAFKA-17331
**Subsystem**: core/server/KafkaApis
**Category**: Missing null / bounds check

**Root cause**
The server-side `ListOffsets` handler accepted special negative timestamps (`EARLIEST_LOCAL_TIMESTAMP = -4`, `LATEST_TIERED_TIMESTAMP = -5`, `MAX_TIMESTAMP = -7`) that were only valid from specific API versions, but it never validated the request version before dispatching. Old clients sending a request at version 0–6 with `EARLIEST_LOCAL_TIMESTAMP` would accidentally receive results as if it were `EARLIEST_TIMESTAMP` because `UnifiedLog.legacyFetchOffsetsBefore` had a fallthrough case that mapped both to `startIndex = 0`.

**Before/After**
```java
// Before (no version check; falls through to startIndex=0 for any negative timestamp)
case ListOffsetsRequest.EARLIEST_LOCAL_TIMESTAMP =>
    startIndex = 0  // wrong: treated like EARLIEST on old versions

// After (version-gated before any dispatch)
} else if (partition.timestamp() < 0 &&
    (!timestampMinSupportedVersion.contains(partition.timestamp()) ||
     version < timestampMinSupportedVersion(partition.timestamp()))) {
    buildErrorResponse(Errors.UNSUPPORTED_VERSION, partition)
```

**How to spot**
Whenever a negative sentinel timestamp constant is added to the `ListOffsets` API, verify that the server explicitly rejects requests using that constant if the API version is below the version that introduced it. Missing such a check allows old clients to silently receive semantically wrong results instead of a clear `UNSUPPORTED_VERSION` error.

**Test**
`testListOffsetEarliestLocalTimestampWithUnsupportedVersion`, `testListOffsetLatestTieredTimestampWithUnsupportedVersion`, and related tests added.
