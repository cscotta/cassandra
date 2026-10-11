# Bug 0803: crossNode Flag Always False on Received Messages

**Commit**: 9d9a1a1224 · **Ticket**: CASSANDRA-12791
**Subsystem**: Messaging / Query Monitoring
**Category**: Logic error in condition

---

## Root Cause
The original `ConstructionTime` wrapper class carried both a timestamp and a `crossNode` boolean. When a message arrived from a remote node, the `crossNode` field in `ConstructionTime` was supposed to be set to `true`, enabling cross-node timeout tracking. However, the boolean was stored in the object at deserialization time, and a subtle bug caused it to remain `false`—meaning queries routed from a remote coordinator were timed out using the local construction time instead of the sender's construction time, defeating the purpose of `cross_node_timeout`. The fix replaced the `ConstructionTime` object with a plain `long` timestamp and derived `isCrossNode()` by comparing the sender's `InetAddress` against the local broadcast address, making the calculation correct and unforgeable.

### Before / After
```java
// BEFORE — crossNode came from ConstructionTime object that could be wrong
public static ConstructionTime readTimestamp(InetAddress from, DataInputPlus input, long timestamp)
        throws IOException {
    int partial = input.readInt();
    long crossNodeTimestamp = ...;
    if (DatabaseDescriptor.hasCrossNodeTimeout()) {
        return new ConstructionTime(crossNodeTimestamp, timestamp != crossNodeTimestamp);
    } else {
        return new ConstructionTime(); // crossNode = false
    }
}

// AFTER — isCrossNode derived from source address, impossible to be wrong
public boolean isCrossNode() {
    return !from.equals(DatabaseDescriptor.getBroadcastAddress());
}
```

---

## How to Spot
When a boolean property of a message is computed at serialization/deserialization time and stored inside the message object, check whether the same property could be derived deterministically from other message fields (such as the sender address). Storing a derived truth in a mutable field creates divergence risk; prefer deriving it on demand from an authoritative source. Also look for cases where a constructor sets a boolean to a default value that is only updated on one code path—any code path that bypasses the update silently preserves the wrong default.

---

## Test
Existing `MonitoringTaskTest`, `MessagingServiceTest`, and `MockMessagingServiceTest` were updated to use the new API; they provide coverage of the monitoring timeout path.
