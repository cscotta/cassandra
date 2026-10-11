**Commit**: bcbc72e29b · **Ticket**: KAFKA-16720
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
    <!-- share coordinator -->

// After
    <!-- share coordinator and persister-->
    <suppress checks="ClassDataAbstractionCouplingCheck"
              files="(ShareCoordinatorServiceTest|DefaultStatePersisterTest|PersisterStateManagerTest).java"/>
    /**
     * List the share group offsets available in the cluster for the specified share groups.
     *
     * @param groupSpecs Map of share group ids to a spec that specifies the topic partitions of the group to list offsets for.
     * @param options The options to use when listing the share group offsets.
     * @return The ListShareGroupOffsetsResult
     */
    ListShareGroupOffsetsResult listShareGroupOffsets(Map<String, ListShareGroupOffsetsSpec> groupSpecs, ListShareGroupOffsetsOptions options);

```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: [kafka-16720] adminclient support for listsharegroupoffsets (1/n) (#18571).

**Test**
Test modified in MockAdminClient.java covering the corrected behaviour.
