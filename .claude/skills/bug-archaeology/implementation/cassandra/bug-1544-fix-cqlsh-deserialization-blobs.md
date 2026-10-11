# Bug 1544: Fix cqlsh Deserialization of Blobs

**Commit**: 5001b382ed · **Ticket**: CASSANDRA-7603
**Subsystem**: bin/cqlsh (Python CQL shell)
**Category**: Wrong serialization / deserialization

---

## Root Cause

The cqlsh shell monkey-patched `cassandra.cqltypes.BytesType.deserialize` to call `BytesType.validate` as a workaround for the Python driver returning BLOBs as `str`. However, `validate` does not return the value as a `bytearray`, which cqlsh expects for proper BLOB display and handling. The fix replaces this with a lambda that converts the raw bytes to `bytearray`.

### Before / After

```python
# BEFORE
#Python driver returns BLOBs as string, but we expect them as buffer()
cassandra.cqltypes.BytesType.deserialize = staticmethod(cassandra.cqltypes.BytesType.validate)
```

```python
# AFTER
# the python driver returns BLOBs as string, but we expect them as bytearrays
cassandra.cqltypes.BytesType.deserialize = staticmethod(lambda byts, protocol_version: bytearray(byts))
```

---

## How to Spot

When a workaround patches a type's deserialization to call `validate` instead of actually converting the value, verify that `validate` returns the expected output type — it typically returns the input unchanged (or raises), not a converted form.

---

## Test

No test added.
