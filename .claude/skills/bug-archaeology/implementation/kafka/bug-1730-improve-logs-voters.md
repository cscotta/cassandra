**Commit**: 2e4a378c27 · **Ticket**: KAFKA-18131
**Subsystem**: raft/src
**Category**: State not cleaned up

**Root cause**
Currently, the log of LeaderState#timeUntilCheckQuorumExpires uses streams without a terminal operator, resulting in output like java.util.stream.ReferencePipeline$3@39660237. This PR aims to fix this issue and improve the log message. Reviewers: Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
                voterStates.values().stream().map(voter -> voter.replicaKey)
        return String.format("ReplicaKey(id=%d, directoryId=%s)", id, directoryId);

// After
                voterStates.values()
                    .stream()
                    .map(voter -> voter.replicaKey)
                    .collect(Collectors.toUnmodifiableSet())
        return String.format("ReplicaKey(id=%d, directoryId=%s)", id, directoryId.map(Uuid::toString).orElse("<undefined>"));
```

**How to spot**
When reviewing raft/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18131: improve logs for voters (#18028).

**Test**
No test added.
