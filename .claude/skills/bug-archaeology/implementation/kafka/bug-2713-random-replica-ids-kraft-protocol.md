**Commit**: e1bf155270 · **Ticket**: KAFKA-17055
**Subsystem**: raft/src
**Category**: Wrong serialization / deserialization

**Root cause**
All of the tests in KafkakRaftClientTest and KafkaRaftClientSnapshotTest use well known ids like 0, 1, etc. Because of this those tests were not able to catch a bug in the BeginQuorumEpoch schema were the default value for VoterId was 0 instead of -1. Improve those tests by using random valid replica id to lower the probability that the replica id will match the default value of the schema. Reviewers: José Armando García Sancio <jsancio@apache.org>

**Before/After**
```java
// Before
        int localId = 0;
        int localId = 0;
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
        int localId = 0;
    @ValueSource(booleans = {true, false})
        int localId = 0;
    @ValueSource(booleans = {true, false})
        int localId = 0;
    @ValueSource(booleans = {false, true})
        int localId = 0;
    @ValueSource(booleans = {false, true})
        int localId = 0;

// After
import java.util.concurrent.ThreadLocalRandom;
        int localId = randomReplicaId();
        int localId = randomReplicaId();
    @CsvSource({ "false,false", "false,true", "true,false", "true,true" })
        int localId = randomReplicaId();
    @ValueSource(booleans = { true, false })
        int localId = randomReplicaId();
    @ValueSource(booleans = { true, false })
        int localId = randomReplicaId();
    @ValueSource(booleans = { false, true })
        int localId = randomReplicaId();
    @ValueSource(booleans = { false, true })
```

**How to spot**
When reviewing raft/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17055; use random replica ids in kraft protocol tests.

**Test**
Test modified in KafkaRaftClientSnapshotTest.java covering the corrected behaviour.
