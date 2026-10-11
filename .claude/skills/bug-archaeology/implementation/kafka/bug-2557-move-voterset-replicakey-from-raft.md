**Commit**: 81f0b13a70 · **Ticket**: KAFKA-17238
**Subsystem**: metadata/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.raft.{KafkaRaftClient, OffsetAndEpoch}
import org.apache.kafka.raft.internals.VoterSetTest
import org.apache.kafka.raft.internals.VoterSet;
import org.apache.kafka.raft.internals.ReplicaKey;
import org.apache.kafka.raft.internals.VoterSet;
import org.apache.kafka.raft.internals.ReplicaKey;
import org.apache.kafka.raft.internals.VoterSet;
import org.apache.kafka.raft.internals.VoterSet;
import org.apache.kafka.raft.internals.ReplicaKey;
import org.apache.kafka.raft.internals.ReplicaKey;

import org.apache.kafka.raft.internals.ReplicaKey;

// After
import org.apache.kafka.raft.{KafkaRaftClient, OffsetAndEpoch, VoterSetTest}
import org.apache.kafka.raft.VoterSet;
```

**How to spot**
When reviewing metadata/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17238 move voterset and replicakey from raft.internals to raft (#16775).

**Test**
Test modified in DumpLogSegmentsTest.scala covering the corrected behaviour.
