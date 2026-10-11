# Bug 0093: Paxos V2 Prepare Response Serializes ConsensusMigratedAt Only for Permitted Branch

**Commit**: 4c575a7e48 · **Ticket**: CASSANDRA-19023
**Subsystem**: service/paxos — PaxosPrepare.ResponseSerializer
**Category**: Wrong serialization / deserialization

---

## Root Cause

`PaxosPrepare.ResponseSerializer` serialized the `maybeConsenusMigratedAt` field inside the `else` branch (i.e., only for `Permitted` responses), but the field is present on all `Response` objects regardless of whether the response is a rejection or a permission. As a result, rejected responses were missing the `ConsensusMigratedAt` payload that the deserializer expected to read for version >= VERSION_51, causing deserialization to read the wrong bytes. Additionally, the `serializedSize` method used an early `return` inside the `else` (Permitted) branch that bypassed adding the `ConsensusMigratedAt` size entirely, leading to under-counted buffer allocations. The fix moves both the `serialize` call and the `serializedSize` contribution for `ConsensusMigratedAt` outside the if/else so they apply to every response.

### Before / After

```java
// BEFORE — ConsensusMigratedAt written only in the Permitted branch
} else {
    // ... write Permitted fields ...
    if (version >= MessagingService.VERSION_51)
        ConsensusMigratedAt.serializer.serialize(response.maybeConsenusMigratedAt, out, version);
}
```

```java
// AFTER — written unconditionally after both branches
} // end if/else
if (version >= MessagingService.VERSION_51)
    ConsensusMigratedAt.serializer.serialize(response.maybeConsenusMigratedAt, out, version);
```

```java
// BEFORE — serializedSize returns early from Permitted branch, misses ConsensusMigratedAt
} else {
    Permitted permitted = (Permitted) response;
    return 1 + VIntCoding.computeUnsignedVIntSize(permitted.lowBound) + ...;
}
```

```java
// AFTER — accumulate into size, fall through to add ConsensusMigratedAt
long size = 1; // flags
if (response.isRejected()) {
    size += Ballot.sizeInBytes();
} else {
    Permitted permitted = (Permitted) response;
    size += VIntCoding.computeUnsignedVIntSize(permitted.lowBound) + ...;
}
// size += ConsensusMigratedAt contribution follows
```

---

## How to Spot

When a field is declared on a base class and must be serialized for all subtype responses, look for any branch (`if isRejected / else`) inside `serialize` or `serializedSize` — if the field write appears in only one branch, that is the bug. The `serializedSize` early-return pattern (`return X + ...` inside an `else` block) is especially dangerous because it silently bypasses any trailing shared-field size contributions. Prefer accumulator variables over early returns in `serializedSize` implementations.

---

## Test

No dedicated test added; the fix is a structural correction in the serializer.
