**Commit**: ed0e1086a9 · **Ticket**: KAFKA-17228
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
This patch enables the static member using the old protocol to be replaced in ConsumerGroupHeartbeat even if it hasn't sent a request to leave the group. This helps in cases where existing static member rejoins and triggers a group upgrade, because the classic static consumer doesn't send leave group request before shutting down. Reviewers: TengYao Chi <kitingiao@gmail.com>, David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
                if (!useClassicProtocol) {
                    // Static member rejoins with a different member id so it should replace
                    // the previous instance iff the previous member had sent a leave group.
import java.util.HashSet;
            .setClientId("client")
            .setClientHost("localhost/127.0.0.1")
            .setClientId("client")
            .setClientHost("localhost/127.0.0.1")
                    .setClientId("client")
                    .setClientHost("localhost/127.0.0.1")
            .setClientId("client")
            .setClientHost("localhost/127.0.0.1")

// After
                if (!useClassicProtocol && !existingStaticMemberOrNull.useClassicProtocol()) {
                    // If both the rejoining static member and the existing static member use the consumer
                    // protocol, replace the previous instance iff the previous member had sent a leave group.
import static org.apache.kafka.coordinator.group.GroupMetadataManagerTestContext.DEFAULT_CLIENT_ADDRESS;
import static org.apache.kafka.coordinator.group.GroupMetadataManagerTestContext.DEFAULT_CLIENT_ID;
            .setClientId(DEFAULT_CLIENT_ID)
            .setClientHost(DEFAULT_CLIENT_ADDRESS.toString())
            .setClientId(DEFAULT_CLIENT_ID)
            .setClientHost(DEFAULT_CLIENT_ADDRESS.toString())
                    .setClientId(DEFAULT_CLIENT_ID)
                    .setClientHost(DEFAULT_CLIENT_ADDRESS.toString())
            .setClientId(DEFAULT_CLIENT_ID)
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17228; static member using new protocol should always replace the one using the old protocol (#16800).

**Test**
Test modified in GroupMetadataManagerTest.java covering the corrected behaviour.
