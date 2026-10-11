**Commit**: 14a9130f6f · **Ticket**: KAFKA-17793
**Subsystem**: metadata/src
**Category**: Resource leak

**Root cause**
As described in KIP-500, the Kafka controller monitors the liveness of each broker in the cluster. It gathers this information from heartbeats sent from the brokers themselves. In some rare cases, the main controller thread may get blocked for several seconds at a time. In the current code, this will result in the controller being unable to update the last contact times for the brokers during this time.

**Before/After**
```java
// Before
 * The BrokerHeartbeatManager manages all the soft state associated with broker heartbeats.
 * Soft state is state which does not appear in the metadata log.  This state includes
 * things like the last time each broker sent us a heartbeat.  As of KIP-841, the controlled
 * shutdown state is no longer treated as soft state and is persisted to the metadata log on broker
 * controlled shutdown requests.
         * The last time we received a heartbeat from this broker, in monotonic nanoseconds.
         * When this field is updated, we also may have to update the broker's position in
         * the unfenced list.
        long lastContactNs;
        long metadataOffset;
        /**
         * The previous entry in the unfenced list, or null if the broker is not in that list.

// After
 * The BrokerHeartbeatManager manages some of the soft state associated with broker heartbeats.
 * For example, it stores the last metadata offset which each broker reported. It contains the
 * BrokerHeartbeatTracker, which stores the last time we received a heartbeat from each broker.
 * In addition to storing this soft state, the BrokerHeartbeatManager aggregates some information
 * about brokers (such as whether they're fenced or not) into a single place.
         * True if this broker is fenced.
        private boolean fenced;
        private long metadataOffset;
        BrokerHeartbeatState(
            int id,
            boolean fenced,
            long metadataOffset,
```

**How to spot**
When reviewing metadata/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17793: improve kcontroller robustness against long delays (#17502).

**Test**
Test modified in BrokerHeartbeatManagerTest.java covering the corrected behaviour.
