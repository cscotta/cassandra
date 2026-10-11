**Commit**: e49c8df1f7 · **Ticket**: KAFKA-16533
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
Add support for handling the update voter RPC. The update voter RPC is used to automatically update
the voters supported kraft versions and available endpoints as the operator upgrades and
reconfigures the KRaft controllers. The add voter RPC is handled as follow:

1. Check that the leader has fenced the previous leader(s) by checking that the HWM is known;
   otherwise, return the REQUEST_TIMED_OUT error.

**Before/After**
```java
// Before
                AddRaftVoterResponse addResponse = (AddRaftVoterResponse) response;
      "about": "The error code, or 0 if there was no error" }

// After
import org.apache.kafka.common.requests.RemoveRaftVoterResponse;
                RemoveRaftVoterResponse addResponse = (RemoveRaftVoterResponse) response;
    { "name": "CurrentLeaderEpoch", "type": "int32", "versions": "0+",
      "about": "The current leader epoch of the partition, -1 for unknown leader epoch" },
      "about": "The error code, or 0 if there was no error" },
    { "name": "CurrentLeader", "type": "CurrentLeader", "versions": "0+",
      "taggedVersions": "0+", "tag": 0, "fields": [
        { "name": "LeaderId", "type": "int32", "versions": "0+", "default": "-1", "entityType" : "brokerId",
          "about": "The replica id of the current leader or -1 if the leader is unknown" },
        { "name": "LeaderEpoch", "type": "int32", "versions": "0+", "default": "-1",
          "about": "The latest known leader epoch" },
        { "name": "Host", "type": "string", "versions": "0+", "about": "The node's hostname" },
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16533; update voter handling.

**Test**
Test modified in RequestResponseTest.java covering the corrected behaviour.
