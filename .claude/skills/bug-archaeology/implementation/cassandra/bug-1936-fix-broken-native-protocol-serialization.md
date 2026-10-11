# Bug 1936: Missing paging state flag and dead size accumulation in native protocol serialization

**Commit**: 06dc4d0fd2 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cql3/QueryOptions.java, src/java/org/apache/cassandra/transport/messages/RegisterMessage.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

Two independent serialization bugs existed in the native protocol layer. First, `QueryOptions` serialization omitted the `PAGING_STATE` flag even when a paging state was present, so the recipient would not know to read the paging state field from the wire. Second, `RegisterMessage`'s `encodedSize` method computed the per-element size but discarded the result by never accumulating it into `size`, always returning a fixed value of 2 regardless of how many event types were registered.

### Before / After

```java
// BEFORE — QueryOptions: paging state flag never added
if (options.getPageSize() >= 0)
    flags.add(Flag.PAGE_SIZE);
if (options.getSerialConsistency() != ConsistencyLevel.SERIAL)
    flags.add(Flag.SERIAL_CONSISTENCY);
```

```java
// AFTER — flag added when paging state is non-null
if (options.getPageSize() >= 0)
    flags.add(Flag.PAGE_SIZE);
if (options.getPagingState() != null)
    flags.add(Flag.PAGING_STATE);
if (options.getSerialConsistency() != ConsistencyLevel.SERIAL)
    flags.add(Flag.SERIAL_CONSISTENCY);
```

```java
// BEFORE — RegisterMessage: computed size is discarded
for (Event.Type type : msg.eventTypes)
    CBUtil.sizeOfEnumValue(type);   // return value dropped
return size;

// AFTER — size is accumulated
for (Event.Type type : msg.eventTypes)
    size += CBUtil.sizeOfEnumValue(type);
return size;
```

---

## How to Spot

When reviewing `encodedSize` / `serializedSize` methods that iterate over collections, verify that each loop iteration's computed contribution is actually added to the accumulator. Also audit flag-building blocks in protocol encoders: each optional field should have a corresponding flag-emission guarded by a null/presence check.

---

## Test

No test added.
