**Commit**: 1e5b6b8e83 · **Ticket**: KAFKA-17693
**Subsystem**: clients/test (ListOffsetsRequestTest)
**Category**: Logic error in condition

**Root cause**
`testCheckEarliestLocalTimestampVersion` and `testCheckLatestTieredTimestampVersion` constructed `NodeApiVersions` using an overload that accepted `List<ApiVersion>` but did not propagate those versions into the internal lookup map, so the version range checks under test always reflected the default supported ranges rather than the injected values. The tests were asserting against phantom behaviour and providing false confidence.

**Before/After**
```java
// Before – broken constructor, versions not wired in
NodeApiVersions versions = new NodeApiVersions(List.of(
    new ApiVersion().setApiKey(ApiKeys.LIST_OFFSETS.id)
        .setMinVersion((short)0).setMaxVersion((short)7)));
// assertions passed regardless of injected data

// After – tests removed entirely; coverage via integration tests
// (testCheckEarliestLocalTimestampVersion deleted)
// (testCheckLatestTieredTimestampVersion deleted)
```

**How to spot**
When a test constructs a mock/stub object via a non-standard constructor, verify with a debugger or log that the stub data actually flows into the code path under test; if assertions pass with obviously wrong input the test is likely exercising nothing.

**Test**
Tests removed; no replacement unit test added.
