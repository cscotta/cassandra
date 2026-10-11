**Commit**: 7b6d4fbc57 · **Ticket**: KAFKA-17333
**Subsystem**: raft/src
**Category**: State not cleaned up

**Root cause**
When a voter fails as leader (LeaderState) the quorum-state still states that it is the leader of
the epoch. When the voter starts it never starts as leader and instead starts as resigned
(ResignedState) if it was previously a leader. This causes the KRaft client to immediately notify
the state machine (e.g QuorumController) that it is leader or active. This is incorrect for two
reasons.

**Before/After**
```java
// Before
        maybeFireLeaderChange();
        } else {

// After
        } else if (!quorum.isResigned()) {
            /* Should not fire leader change while in the resigned state for two reasons.
             * 1. The epoch start offset is not tracked but the leader is the local replica.
             *    Listener cannot be notify of leadership until they have caught to the latest
             *    epoch.
             * 2. It is not pratical to notify of local leadership since any write operation
             *    (prepareAppend and schedulePreparedAppend) will fail with NotLeaderException
             */
                logger.debug("Notifying listener {} of new leadership {}", listenerName(), leaderAndEpoch);
        log.info("Attempting durable transition to {} from {}", newState, state);
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
```

**How to spot**
When reviewing raft/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17333; resignedstate should not notify of leader change (#16900).

**Test**
Test modified in KafkaRaftClientTest.java covering the corrected behaviour.
