**Commit**: 6d72677eda · **Ticket**: KAFKA-18613
**Subsystem**: group-coordinator/src
**Category**: Resource leak

**Root cause**
Basic streams group heartbeat handling. The main part of are the unit
tests that make sure that we behave, for the most part, like a consumer
group. - No support for static membership
- No support for configurations (using constants instead)
- No support for regular expressions

Reviewers: Bill Bejeck <bill@confluent.io>, Bruno Cadonna
<cadonna@apache.org>

**Before/After**
```java
// Before
     * @return A Result containing the StreamsGroupHeartbeat response, a list of internal topics to be created and

// After
     * @return A result containing the StreamsGroupHeartbeat response, a list of internal topics to be created and
import org.apache.kafka.common.errors.StreamsInvalidTopologyException;
import org.apache.kafka.common.message.CreateTopicsRequestData.CreatableTopic;
import org.apache.kafka.common.message.StreamsGroupHeartbeatRequestData.Endpoint;
import org.apache.kafka.common.message.StreamsGroupHeartbeatRequestData.KeyValue;
import org.apache.kafka.common.message.StreamsGroupHeartbeatRequestData.TaskIds;
import org.apache.kafka.common.message.StreamsGroupHeartbeatRequestData.TaskOffset;
import org.apache.kafka.common.message.StreamsGroupHeartbeatRequestData.Topology;
import org.apache.kafka.common.message.StreamsGroupHeartbeatResponseData;
import org.apache.kafka.common.message.StreamsGroupHeartbeatResponseData.Status;
import org.apache.kafka.common.requests.StreamsGroupHeartbeatResponse;
import org.apache.kafka.coordinator.group.streams.assignor.StickyTaskAssignor;
```

**How to spot**
When reviewing group-coordinator/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18613: add streamsgroupheartbeat handler in the group coordinator (#19114).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
