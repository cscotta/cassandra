**Commit**: 9818e9db26 · **Ticket**: KAFKA-17278
**Subsystem**: raft/src
**Category**: Logic error in condition

**Root cause**
Adds RPC compatibility tests for FETCH and FETCH_SNAPSHOT. Reviewers: José Armando García Sancio <jsancio@apache.org>

**Before/After**
```java
// Before
// (no removals)

// After
    // Test that the replicaDirectoryId field introduced in version 17 is ignorable for older versions.
    // This is done by setting a FetchPartition's replicaDirectoryId explicitly to a non-zero uuid and
    // checking that the FetchRequestData can still be written to an older version specified by
    // testCase.version.
    @ParameterizedTest
    @MethodSource("singletonFetchRequestTestCases")
    public void testFetchRequestV17Compatibility(final FetchRequestTestCase testCase) {
        FetchRequestData fetchRequestData = RaftUtil.singletonFetchRequest(
            topicPartition,
            Uuid.ONE_UUID,
            partition -> partition
                .setPartitionMaxBytes(10)
```

**How to spot**
When reviewing raft/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17278; add kraft rpc compatibility tests (#17801).

**Test**
Test modified in RaftUtilTest.java covering the corrected behaviour.
