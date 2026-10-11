# Bug 1528: Avoid NPEs When Receiving Table Changes for Unknown Keyspace

**Commit**: 729d0b8aaf · **Ticket**: CASSANDRA-7689
**Subsystem**: src/java/org/apache/cassandra/service/MigrationManager.java
**Category**: Missing null / bounds check

---

## Root Cause

When announcing a new or dropped `UserType` via `MigrationManager`, the serialized schema mutation did not include the keyspace row. If the receiving node did not yet know about that keyspace, it could not look up the keyspace context and threw a NullPointerException. The fix wraps both `announceNewType` and `announceTypeDrop` with `addSerializedKeyspace(...)` so the keyspace entry is always present in the propagated mutation.

### Before / After

```java
// BEFORE
public static void announceNewType(UserType newType, boolean announceLocally)
{
    announce(UTMetaData.toSchema(newType, FBUtilities.timestampMicros()), announceLocally);
}

public static void announceTypeDrop(UserType droppedType, boolean announceLocally)
{
    announce(UTMetaData.dropFromSchema(droppedType, FBUtilities.timestampMicros()), announceLocally);
}
```

```java
// AFTER
public static void announceNewType(UserType newType, boolean announceLocally)
{
    announce(addSerializedKeyspace(UTMetaData.toSchema(newType, FBUtilities.timestampMicros()), newType.keyspace), announceLocally);
}

public static void announceTypeDrop(UserType droppedType, boolean announceLocally)
{
    announce(addSerializedKeyspace(UTMetaData.dropFromSchema(droppedType, FBUtilities.timestampMicros()), droppedType.keyspace), announceLocally);
}
```

---

## How to Spot

When schema mutations are assembled and broadcast, verify that every dependent entity (type, table, index) includes its parent keyspace row in the same mutation batch. Any `announce*` helper that omits the keyspace wrapper is a candidate for this NPE on nodes with incomplete schema state.

---

## Test

No test added.
