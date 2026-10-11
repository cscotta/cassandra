# Bug 0886: Clustering key value > 64 KB silently accepted, then crashes serialization

**Commit**: 452d626a7a · **Ticket**: CASSANDRA-11882
**Subsystem**: CQL / Storage Engine
**Category**: Missing null / bounds check

---

## Root Cause
Clustering key components and partition key values are serialized with a 2-byte (unsigned short, max 65535) length prefix. When a client supplied a value larger than 65535 bytes as a clustering key, `SelectStatement.getRequestedBound()` built the composite bound without validation. The composite was later serialized via `ByteBufferUtil.writeWithShortLength()`, which had only an `assert` with a bare numeric value (`assert 0 <= length && length <= MAX_UNSIGNED_SHORT : length`) — providing no useful message. The fix adds an explicit validation call on the composite bounds for SELECT, and improves the assertion message in `writeWithShortLength` to describe the limit. As a secondary fix, `OutboundTcpConnection.flush()` now catches `Throwable` (not just `Exception`) and routes it through `JVMStabilityInspector`, preventing silent JVM-level errors from being swallowed.

### Before / After
```java
// BEFORE — no size check on clustering key bound
private List<Composite> getRequestedBound(Bound b, QueryOptions options) {
    return buildBound(b, cfm.clusteringColumns(), columnRestrictions, isReversed, cfm.comparator, options);
}

// AFTER — validates each composite component against the 64K limit
private List<Composite> getRequestedBound(Bound b, QueryOptions options) {
    List<Composite> bound = buildBound(..., options);
    for (Composite c : bound) {
        if (!c.isEmpty())
            QueryProcessor.validateComposite(c, cfm.comparator);
    }
    return bound;
}

// BEFORE — unhelpful assertion message
assert 0 <= length && length <= FBUtilities.MAX_UNSIGNED_SHORT : length;

// AFTER — descriptive message
assert 0 <= length && length <= FBUtilities.MAX_UNSIGNED_SHORT :
    String.format("Attempted serializing to buffer exceeded maximum of %s bytes: %s",
                  FBUtilities.MAX_UNSIGNED_SHORT, length);
```

---

## How to Spot
Any code path that accepts user-supplied byte buffers destined for `writeWithShortLength()` should validate the size before reaching serialization. Review all `getRequestedBound`, `buildBound`, and similar CQL-to-storage translation methods for missing `validateComposite` or equivalent guards. Also check that `catch (Exception e)` blocks in I/O threads are widened to `catch (Throwable e)` if the underlying operation can surface `Error` subclasses.

---

## Test
`InsertTest.testOverlyLargeInsertPK/CK()` and `SelectTest.testOverlyLargeSelectPK/CK/KeyIn()` were added to assert that `InvalidRequestException` is thrown for values above `MAX_UNSIGNED_SHORT`.
