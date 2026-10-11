**Commit**: 6f4d4255a6 · **Ticket**: KAFKA-18613
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Tests for joining with missing source topics,
internal topics, incorrectly partitioned topics,
and stale topologies. Reviewers: Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
        StreamsTopology updatedTopology;
            StreamsGroupTopologyValue recordValue = convertToStreamsGroupTopologyRecord(topology);

            updatedTopology = StreamsTopology.fromHeartbeatRequest(topology);


            } else if (!updatedTopology.equals(group.topology().get())) {
            updatedTopology = group.topology().get();
        return updatedTopology;
            log,
                                              final Logger log,
        if (fixedRepartitionTopics.isEmpty() && flexibleRepartitionTopics.isEmpty()) {

// After
            StreamsTopology streamsTopologyFromRequest = StreamsTopology.fromHeartbeatRequest(topology);
                StreamsGroupTopologyValue recordValue = convertToStreamsGroupTopologyRecord(topology);
                return streamsTopologyFromRequest;
            } else if (group.topology().get().topologyEpoch() > topology.epoch()) {
                log.info("[GroupId {}][MemberId {}] Member joined with stale topology epoch {}", groupId, memberId, topology.epoch());
                return group.topology().get();
            } else if (!group.topology().get().equals(streamsTopologyFromRequest)) {
            } else {
                log.debug("[GroupId {}][MemberId {}] Member joined with currently initialized topology {}", groupId, memberId, topology.epoch());
                return group.topology().get();
            return group.topology().get();
        // ensure the co-partitioning topics within the group have the same number of partitions,
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18613: improve test coverage for missing topics (#19189).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
