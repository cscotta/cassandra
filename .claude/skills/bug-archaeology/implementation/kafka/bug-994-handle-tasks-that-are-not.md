**Commit**: 678d456ad7 · **Ticket**: KAFKA-19044
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
A heartbeat might be sent to the group coordinator, claiming to own
tasks that we do  not know about. We need some logic to handle those
requests. In KIP-1071, we propose  to return `INVALID_REQUEST` error
whenever this happens, effectively letting the  clients crash. This behavior will, however, make topology updates impossible.

**Before/After**
```java
// Before
import org.apache.kafka.image.TopicImage;
import java.util.stream.Stream;

// After
import org.apache.kafka.coordinator.group.streams.topics.ConfiguredSubtopology;
import java.util.SortedMap;
    /**
     * Validates that the requested tasks exist in the configured topology and partitions are valid.
     * If tasks is null, does nothing. If an invalid task is found, throws InvalidRequestException.
     *
     * @param subtopologySortedMap The configured topology.
     * @param tasks                The list of requested tasks.
     */
    private static void throwIfRequestContainsInvalidTasks(
        SortedMap<String, ConfiguredSubtopology> subtopologySortedMap,
        List<StreamsGroupHeartbeatRequestData.TaskIds> tasks
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19044: handle tasks that are not present in the current topology (#19722).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
