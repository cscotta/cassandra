# Bug 1647: Fix Regression from CASSANDRA-6855

**Commit**: 233761e539 · **Ticket**: CASSANDRA-6855
**Subsystem**: src/java/org/apache/cassandra/cql3 (QueryOptions, CollectionSerializer)
**Category**: Wrong constant / default value

---

## Root Cause

CASSANDRA-6855 introduced protocol-version-aware collection serialization: protocol v3 uses 4-byte length prefixes while v2 uses 2-byte prefixes. `CollectionSerializer.serialize()` and `deserialize()` were correctly updated to use protocol version 3 for internal (non-user-facing) calls. However, `QueryOptions.forInternalCalls()` still constructed a `DefaultQueryOptions` with protocol version `0`. When internal CQL calls (e.g., schema reads, system table queries) went through the collection deserialization path, version 0 triggered the v2 (2-byte) code path, corrupting collection values that had been serialized with 4-byte headers. The fix sets the internal protocol version to `3` in `forInternalCalls()`.

### Before / After

```java
// BEFORE
public static QueryOptions forInternalCalls(ConsistencyLevel consistency, List<ByteBuffer> values)
{
    return new DefaultQueryOptions(consistency, values, false, SpecificOptions.DEFAULT, 0);
}
```

```java
// AFTER
public static QueryOptions forInternalCalls(ConsistencyLevel consistency, List<ByteBuffer> values)
{
    return new DefaultQueryOptions(consistency, values, false, SpecificOptions.DEFAULT, 3);
}
```

---

## How to Spot

When a serialization format becomes version-dependent, audit every construction site of the options/context object that carries the version. Factory methods for "internal" or "test" use are especially likely to be left with a stale default (often 0) that silently selects an old format.

---

## Test

No test added. The regression manifests as corrupted collection reads during internal schema or system table operations after upgrading to a build containing CASSANDRA-6855.
