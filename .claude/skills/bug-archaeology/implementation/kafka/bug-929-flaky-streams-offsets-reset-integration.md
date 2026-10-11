**Commit**: d61b162b13 · **Ticket**: KAFKA-19244
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
This PR deflakes the
`testResetOffsetsWithDeleteSpecifiedInternalTopics()`

Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
            Set<String> internalTopicsAfterReset = getInternalTopics(appId);

                () -> internalTopicsAfterReset.size() == allInternalTopics.size(),


// After
                () -> getInternalTopics(appId).size() == allInternalTopics.size(),
            // verify that the specified internal topics were deleted
            Set<String> internalTopicsAfterReset = getInternalTopics(appId);
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19244: fix flaky streams offsets reset integration test (#20027).

**Test**
Test modified in ResetStreamsGroupOffsetTest.java covering the corrected behaviour.
