# Bug 1436: Fix IAE in Tuples.InValue with Pre-v3 Protocol

**Commit**: 1ef7d056d1 · **Ticket**: CASSANDRA-8062
**Subsystem**: src/java/org/apache/cassandra/cql3/Tuples.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`Tuples.InValue.fromSerialized()` deserialized a list of tuple values from a prepared statement binding by calling `ListType.compose(value)`. The `compose()` method always uses the v3 native protocol serialization format for collections, but clients using the v1 or v2 protocol send collection data in a different binary format (2-byte length prefixes instead of 4-byte). Passing a v1/v2-encoded buffer to `compose()` caused an `IllegalArgumentException` when the method tried to interpret the shorter length fields as v3 4-byte integers. The fix passes the `QueryOptions` (which carries the protocol version) to `deserializeForNativeProtocol()` so the correct wire format is used.

### Before / After

```java
// BEFORE
public static InValue fromSerialized(ByteBuffer value, ListType type) throws InvalidRequestException {
    List<?> l = (List<?>)type.compose(value);  // always uses v3 format
    ...
}
```

```java
// AFTER
public static InValue fromSerialized(ByteBuffer value, ListType type, QueryOptions options) throws InvalidRequestException {
    List<?> l = (List<?>)type.getSerializer().deserializeForNativeProtocol(value, options.getProtocolVersion());
    ...
}
```

---

## How to Spot

Any deserialization of collection or tuple values that does not thread `QueryOptions` (or at minimum the protocol version) through to the actual byte-level decoder is suspicious. The native protocol changed collection length encoding between v2 and v3, so a hard-coded `compose()` call works only for one protocol generation. Review all call sites that deserialize user-supplied bound values from prepared statements to confirm they respect the negotiated protocol version.

---

## Test

No test added.
