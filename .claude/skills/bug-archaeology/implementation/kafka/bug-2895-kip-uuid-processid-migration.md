**Commit**: 8b6013f851 · **Ticket**: KAFKA-15045
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
This PR changes the assignment process to use ProcessId instead of UUID. Reviewers: Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
              files="(InternalTopologyBuilder|KafkaStreams|KStreamImpl|KTableImpl).java"/>
public class ProcessId {
        return "ProcessId{id=" + id + "}";
import java.util.UUID;
        final Map<UUID, Optional<String>> clientRacks = new HashMap<>();
        final List<UUID> clientIds = new ArrayList<>();
        final Map<UUID, KafkaStreamsAssignment> assignmentsByUuid = new HashMap<>();

        for (final Map.Entry<ProcessId, KafkaStreamsAssignment> entry : kafkaStreamsAssignments.entrySet()) {
            final UUID uuid = entry.getKey().id();
            clientIds.add(uuid);
            clientRacks.put(uuid, kafkaStreamsStates.get(entry.getKey()).rackId());

// After
              files="(InternalTopologyBuilder|KafkaStreams|KStreamImpl|KTableImpl|StreamsPartitionAssignor).java"/>
public class ProcessId implements Comparable<ProcessId> {
        return id.toString();

    @Override
    public int compareTo(final ProcessId o) {
        return this.id.compareTo(o.id);
    }
        final List<ProcessId> clientIds = new ArrayList<>(kafkaStreamsStates.keySet());
            kafkaStreamsAssignments,
            kafkaStreamsStates,
            kafkaStreamsAssignments,
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15045: (kip-924 pt. 21) uuid to processid migration (#16269).

**Test**
Test modified in HighAvailabilityStreamsPartitionAssignorTest.java covering the corrected behaviour.
