# Bug 0396: 4.0 Node Repair PrepareMessage Serialization Breaks Connection to 3.x Node

**Commit**: efe830e1f7 · **Ticket**: CASSANDRA-16542
**Subsystem**: src/java/org/apache/cassandra/repair/messages/PrepareMessage.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`PrepareMessage.Serializer.serializedSize()` contained a `Preconditions.checkArgument` that asserted the messaging version must equal `MessagingService.current_version` (the 4.0 version). When a 4.0 coordinator computed the serialized size of a `PrepareMessage` to send to a 3.x node, the version parameter was the 3.x version, causing the precondition to throw an exception. This exception was unhandled at the framing layer and broke the connection entirely. The fix removes the over-restrictive version guard; the size calculation already handled different versions correctly elsewhere.

### Before / After

```java
// BEFORE
public long serializedSize(PrepareMessage message, int version)
{
    Preconditions.checkArgument(version == MessagingService.current_version, MIXED_MODE_ERROR);
    long size;
    ...
}
```

```java
// AFTER
public long serializedSize(PrepareMessage message, int version)
{
    long size;
    ...
}
```

---

## How to Spot

Precondition guards inside serializer `serializedSize()` or `serialize()` methods that restrict the version to `current_version` will always fire during mixed-version cluster operations. Serializers must be able to handle any supported wire version; version checks belong in the coordinator routing logic, not in the size-calculation path.

---

## Test

No new test added; the fix is a one-line deletion, and the existing mixed-version messaging infrastructure validates the corrected behaviour.
