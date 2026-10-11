**Commit**: b73e31eb15 · **Ticket**: KAFKA-17641
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
Introduces v2 of Vote RPC and implements the handling of the new version of the RPC. Many references to "candidate" in the Vote RPC are changed to the more generic "replica". Replicas sending Vote request with PreVote set to true are not candidate. They are instead prospective candidate that are attempting to become candidate.

**Before/After**
```java
// Before
                                                   int candidateEpoch,
                                                   int candidateId,
                                   .setCandidateEpoch(candidateEpoch)
                                   .setCandidateId(candidateId)
  // Version 1 adds voter key and candidate directory id (KIP-853)
  "validVersions": "0-1",
            { "name": "CandidateEpoch", "type": "int32", "versions": "0+",
              "about": "The bumped epoch of the candidate sending the request."},
            { "name": "CandidateId", "type": "int32", "versions": "0+", "entityType": "brokerId",
              "about": "The replica id of the voter sending the request."},
            { "name": "CandidateDirectoryId", "type": "uuid", "versions": "1+", "ignorable": true,
              "about": "The directory id of the voter sending the request." },

// After
                                                   int replicaEpoch,
                                                   int replicaId,
                                   .setReplicaEpoch(replicaEpoch)
                                   .setReplicaId(replicaId)
  // Version 1 adds voter key and directory id (KIP-853)
  // Version 2 adds PreVote field and renames candidate to replica
  "validVersions": "0-2",
            { "name": "ReplicaEpoch", "type": "int32", "versions": "0+",
              "about": "The epoch of the voter sending the request"},
            { "name": "ReplicaId", "type": "int32", "versions": "0+", "entityType": "brokerId",
              "about": "The replica id of the voter sending the request"},
            { "name": "ReplicaDirectoryId", "type": "uuid", "versions": "1+", "ignorable": true,
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17641; update vote rpc with prevote field (#17807).

**Test**
Test modified in RequestResponseTest.java covering the corrected behaviour.
