**Commit**: da8fe6355b · **Ticket**: KAFKA-16915
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
Extend LeaderChangeMessage schema to support version 1 of the message. The leader will continue to write version 0 of the schema. This is needed so that in the future the leader can write version 1 of the message and be guaranteed that all of the replicas in the cluster support version 1 of the schema. Reviewers: José Armando García Sancio <jsancio@apache.org>

**Before/After**
```java
// Before
  "validVersions": "0",
      {"name": "VoterId", "type": "int32", "versions": "0+"}
import org.apache.kafka.common.message.LeaderChangeMessage;
import org.apache.kafka.common.message.LeaderChangeMessage.Voter;
            LeaderChangeMessage.HIGHEST_SUPPORTED_VERSION,
            "Expected LEADER_CHANGE control record type(2), but found COMMIT",
        testDeserializeRecord(ControlRecordType.LEADER_CHANGE);
        final int leaderId = 1;
        final int voterId = 2;
        LeaderChangeMessage data = new LeaderChangeMessage()
            .setLeaderId(leaderId)
            .setVoters(Collections.singletonList(new Voter().setVoterId(voterId)));

// After
  // Version 1 adds VoterDirectoryId (KIP-853)
  "validVersions": "0-1",
      {"name": "VoterId", "type": "int32", "versions": "0+"},
      {"name": "VoterDirectoryId", "type": "uuid", "versions": "1+",
        "about": "The directory id of the voter"}
import org.apache.kafka.common.message.VotersRecord.Voter;
import java.util.List;
            (short) 0,
            "Expected KRAFT_VOTERS control record type(6), but found COMMIT",
        testDeserializeRecord(ControlRecordType.KRAFT_VOTERS);
        final int voterId = 0;
        final List<Voter> voters = Collections.singletonList(
```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16915; leaderchangemessage supports directory id (#16668).

**Test**
Test modified in ControlRecordUtilsTest.java covering the corrected behaviour.
