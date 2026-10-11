# Bug 1609: IllegalArgumentException in CqlStorage

**Commit**: dfcb73426c · **Ticket**: CASSANDRA-7287
**Subsystem**: hadoop/pig/AbstractCassandraStorage
**Category**: Wrong serialization / deserialization

---

## Root Cause

`AbstractCassandraStorage.columnValueToObject()` called `validator.compose(value)` for all types except `DecimalType` and `InetAddressType`. For `CollectionType` validators, `compose()` assumes the CQL native-protocol v3 serialization format for collection values (4-byte element-count prefix, 4-byte element-length prefix per element). However, data arriving through the CQL-over-Thrift interface is encoded in the older pre-v3 format (2-byte element counts and lengths). The format mismatch caused `IllegalArgumentException` when Pig tried to read collection columns via CQL storage.

The fix adds an explicit branch for `CollectionType` that calls `deserializeForNativeProtocol(value, 1)` with protocol version 1, which correctly handles the pre-v3 wire encoding.

### Before / After

```java
// BEFORE — CollectionType falls through to compose(), which uses v3 format
if (validator instanceof DecimalType || validator instanceof InetAddressType)
    return validator.getString(value);
else
    return validator.compose(value);  // throws IAE on pre-v3 collection bytes
```

```java
// AFTER — CollectionType uses protocol-version-aware deserialization
if (validator instanceof DecimalType || validator instanceof InetAddressType)
    return validator.getString(value);

if (validator instanceof CollectionType) {
    // CQL-over-thrift uses the pre-v3 collection format (version 1)
    return ((CollectionSerializer)validator.getSerializer())
               .deserializeForNativeProtocol(value, 1);
}

return validator.compose(value);
```

---

## How to Spot

When data flows across two subsystems that use different wire-format versions (e.g., Thrift vs. native-protocol CQL), any shared deserialization helper that assumes one wire format is a latent bug for the other path. Look for calls to `compose()` or `deserialize()` on collection types in code paths that originate from Thrift/legacy interfaces.

---

## Test

No test added in this commit. The scenario is: a Pig script reads a CQL table that has a collection column (list, set, or map) via `CqlStorage`; the job should complete without `IllegalArgumentException`.
