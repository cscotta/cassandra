**Commit**: b8de78cafd · **Ticket**: KAFKA-19210
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
The test testShareGroupHeartbeatInitializeOnPartitionUpdate was flaky
earlier. The shareGroupStatePartitionMetadataRecord that is created
during heartbeat contains 2 topics to be initialized, but the order in
which they appear in the list is not deterministic. The test is changed
to simply see whether the contents of the record is correct instead of
directly comparing it with an expected record which may contains the
correct topics, but in some different order. Reviewers: Sushant Mahajan <smahajan@confluent.io>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
        assertTrue(result.records().contains(
            newShareGroupStatePartitionMetadataRecord(groupId, mkShareGroupStateMap(List.of(
                    mkShareGroupStateMetadataEntry(t1Uuid, t1Name, List.of(0, 1)),
                    mkShareGroupStateMetadataEntry(t2Uuid, t2Name, List.of(0, 1))
                )),
                Map.of(),
                Map.of()
            ))
        assertTrue(result.records().contains(
            newShareGroupStatePartitionMetadataRecord(groupId, mkShareGroupStateMap(List.of(
                    mkShareGroupStateMetadataEntry(t1Uuid, t1Name, List.of(2, 3))
                )),

// After
        CoordinatorRecord expected = newShareGroupStatePartitionMetadataRecord(
            groupId,
            mkShareGroupStateMap(List.of(
                mkShareGroupStateMetadataEntry(t1Uuid, t1Name, List.of(0, 1)),
                mkShareGroupStateMetadataEntry(t2Uuid, t2Name, List.of(0, 1))
            )),
            Map.of(),
            Map.of()
        Optional<CoordinatorRecord> actual = result.records().stream().filter(record -> record.key() instanceof ShareGroupStatePartitionMetadataKey)
            .findAny();
        assertTrue(actual.isPresent());
        assertRecordEquals(expected, actual.get());
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19210: resolved the flakiness in testsharegroupheartbeatinitializeonpartitionupdate (#19601).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
