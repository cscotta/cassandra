**Commit**: a7e40b7c5a · **Ticket**: KAFKA-18736
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
The Streams heartbeat request has some fields that are always sent. Those are:
- group ID
- member ID
- member epoch
- group instance ID (if static membership is used)

Then it has fields that are only sent when joining:
- topology and topology epoch
- rebalance timeout
- process ID
- endpoint
- client tags

Finally, the assignment is only sent if it changed compared to the last
sent request. Reviewers: Bill Bejeck <bill@confluent.io>, Chia-Ping Tsai
<chia7712@gmail.com>

**Before/After**
```java
// Before
            StreamsGroupHeartbeatRequestData.Topology topology = new StreamsGroupHeartbeatRequestData.Topology();
            topology.setSubtopologies(getTopologyFromStreams(streamsRebalanceData.subtopologies()));
            topology.setEpoch(streamsRebalanceData.topologyEpoch());
            data.setRebalanceTimeoutMs(rebalanceTimeoutMs);
            data.setTopology(topology);
            data.setProcessId(streamsRebalanceData.processId().toString());
            streamsRebalanceData.endpoint().ifPresent(userEndpoint -> {
                data.setUserEndpoint(new StreamsGroupHeartbeatRequestData.Endpoint()
                    .setHost(userEndpoint.host())
                    .setPort(userEndpoint.port())
                );
            });

// After
        // Fields of StreamsGroupHeartbeatRequest sent in the most recent request
        static class LastSentFields {

            private StreamsRebalanceData.Assignment assignment = StreamsRebalanceData.Assignment.EMPTY;

            LastSentFields() {
            }

            void reset() {
                assignment = StreamsRebalanceData.Assignment.EMPTY;
            }
        }
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18736: do not send fields if not needed (#19181).

**Test**
Test modified in StreamsGroupHeartbeatRequestManagerTest.java covering the corrected behaviour.
