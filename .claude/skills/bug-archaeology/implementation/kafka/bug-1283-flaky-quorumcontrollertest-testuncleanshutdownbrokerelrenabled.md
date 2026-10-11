**Commit**: 71875ec58e · **Ticket**: KAFKA-18845
**Subsystem**: metadata/src
**Category**: State not cleaned up

**Root cause**
There're two root causes:
1. When we unclean shutdown `brokerToBeTheLeader`, we didn't wait for
the result. That means when we send heartbeat to unfence broker, it has
chance to use stale broker epoch to send the request. [0]
2.

**Before/After**
```java
// Before
                        setLogDirs(List.of(Uuid.randomUuid())).
                    setLogDirs(List.of(Uuid.randomUuid())).
            active.registerBroker(
                    setLogDirs(List.of(Uuid.randomUuid())).

// After
        Map<Integer, Uuid> brokerLogDirs = allBrokers.stream().collect(
            Collectors.toMap(identity(), brokerId -> Uuid.randomUuid())
        );
                        setLogDirs(List.of(brokerLogDirs.get(brokerId))).
                    setLogDirs(List.of(brokerLogDirs.get(brokerToUncleanShutdown))).
            CompletableFuture<BrokerRegistrationReply> replyLeader = active.registerBroker(
                    setLogDirs(List.of(brokerLogDirs.get(brokerToBeTheLeader))).
            brokerEpochs.put(brokerToBeTheLeader, replyLeader.get().epoch());
            partition = active.replicationControl().getPartition(topicIdFoo, 0);
            int[] expectedIsr = {brokerToBeTheLeader};
            assertArrayEquals(expectedIsr, partition.elr, "The ELR for topic partition foo-0 was " + Arrays.toString(partition.elr) +
                ". It is expected to be " + Arrays.toString(expectedIsr));
```

**How to spot**
When reviewing metadata/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18845: fix flaky quorumcontrollertest#testuncleanshutdownbrokerelrenabled (#19240).

**Test**
Test modified in QuorumControllerTest.java covering the corrected behaviour.
