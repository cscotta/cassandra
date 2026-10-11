# Bug 0076: Accord Commit Message Serializes Deps When NoDeps

**Commit**: 70382b13cc · **Ticket**: CASSANDRA-20241
**Subsystem**: service/accord/serializers/CommitSerializers.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`CommitSerializers` serialized `partialDeps` unconditionally for every `Commit` message, regardless of whether the message's `kind.withDeps` was `HasDeps` or `NoDeps`. When `kind.withDeps == NoDeps`, the `partialDeps` field is null and no dependency bytes should be written; the receiver, however, unconditionally tried to deserialize dependency bytes at the same position. This produced a deserialization mismatch: the reader consumed bytes that belonged to the subsequent `route` field as if they were dependency data, or read past the end of the buffer. The fix gates both the serialization and deserialization of `partialDeps` on `kind.withDeps == Commit.WithDeps.HasDeps`.

### Before / After

```java
// BEFORE – always serializes deps regardless of kind
DepsSerializers.partialDeps.serialize(msg.scope, msg.partialDeps, out, version);
```

```java
// AFTER – only serializes when HasDeps
if (msg.kind.withDeps == Commit.WithDeps.HasDeps)
    DepsSerializers.partialDeps.serialize(msg.scope, msg.partialDeps, out, version);
```

---

## How to Spot

Whenever a serializer has multiple message variants (encoded via a `kind`/`type` enum) and some variants omit certain fields, every field's read and write must be wrapped in the same guard condition. During review, check that every conditional field has matching guards in `serialize`, `deserialize`, and `serializedSize`; a mismatch in any one of the three causes either silent data corruption or a size-calculation error that leads to truncation or buffer overrun.

---

## Test

No dedicated test added; covered by Accord integration/simulation tests that exercise `Commit` message round-trips.
