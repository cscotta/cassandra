# Bug 0366: Wrong Serializer Used for FWD_FRM Parameter in Pre-4.0 Mixed-Mode Clusters

**Commit**: 4d4e1e88d0 · **Ticket**: CASSANDRA-16808
**Subsystem**: src/java/org/apache/cassandra/net/ParamType.java, net/ForwardingInfo.java, net/Message.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

The `RESPOND_TO` (`FWD_FRM`) message parameter was wired to `InetAddressAndPort.Serializer.inetAddressAndPortSerializer`, which writes a length-prefixed address+port for 4.0 messages and a plain address (no length byte, no port) for pre-4.0 messages. However, the pre-4.0 wire format for this specific parameter omits the length prefix entirely and carries only the raw address bytes, while `inetAddressAndPortSerializer` uses a different byte-count encoding for that case. Consequently, when a 4.0 node received a forwarded mutation from a 3.x node, it mis-decoded the `FWD_FRM` field. A companion bug in `ForwardingInfo.useSameMessageID()` also failed to compare the original message ID against the per-hop IDs because it started iteration at index 1, skipping the first entry.

### Before / After

```java
// BEFORE — wrong serializer for FWD_FRM; pre-4.0 encoding differs from inetAddressAndPortSerializer
RESPOND_TO (1, "FWD_FRM", inetAddressAndPortSerializer),
```

```java
// AFTER — dedicated FwdFrmSerializer handles the pre-4.0 raw-bytes special case
RESPOND_TO (1, "FWD_FRM", fwdFrmSerializer),
```

```java
// BEFORE — useSameMessageID() starts at index 1, misses comparison against original id
public boolean useSameMessageID() {
    long id = messageIds[0];
    for (int i = 1; i < messageIds.length; i++)
        if (id != messageIds[i]) return false;
    return true;
}
```

```java
// AFTER — original message id passed in; loop starts at 0
public boolean useSameMessageID(long id) {
    for (int i = 0; i < messageIds.length; i++)
        if (id != messageIds[i]) return false;
    return true;
}
```

---

## How to Spot

When a versioned serializer handles two wire formats (old and new) with structurally different layouts, any shared serializer that treats both as a single parameterised variant risks mis-reading one of them. Review every `IVersionedSerializer` that branches on `version < VERSION_40`: if the branch changes the number of bytes read without a corresponding size check or length-prefix read, the caller's offset arithmetic will be wrong for one of the versions.

---

## Test

`MixedModeMessageForwardTest.checkWritesForwardedToOtherDcTest` — a 2-DC, 4-node cluster is upgraded node-by-node from 3.0 to 4.0 while issuing `CL.ALL` writes coordinated from each node, then verifying the writes appear on all local replicas.
