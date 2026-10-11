# Bug 2235: Bit Arithmetic Mangles Reconstructed Message Timestamp in IncomingTcpConnection

**Commit**: 822ee88a38 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/net/IncomingTcpConnection.java
**Category**: Logic error in condition

---

## Root Cause

When receiving a message in version 1.2+ of the Cassandra binary protocol, only the low 32 bits of the millisecond timestamp are transmitted on the wire to save bandwidth; the receiver reconstructs the full 64-bit timestamp by combining the current system clock's high 32 bits with the received low 32 bits. The original code used `|` (OR) with `0x00000000FFFFFFFFL` instead of `&` (AND) with the high-bit mask, which forced the lower 32 bits of the current time to all-ones before OR-ing in the received value. This produced a timestamp with the wrong high half. Additionally, `input.readInt()` returns a signed 32-bit integer; sign-extension when widening to `long` would corrupt the upper 32 bits unless the sign bit was explicitly stripped.

The corrected expression masks the current time to keep only the high 32 bits (`& 0xFFFFFFFF00000000L`), reads the wire value as a signed int and strips the sign bit with a `<< 2) >> 2` round-trip, then OR-combines the two halves.

### Before / After

```java
// BEFORE — wrong mask ORed in, sign extension ignored
? (System.currentTimeMillis() | 0x00000000FFFFFFFFL) & input.readInt()
```

```java
// AFTER — high bits from clock, low bits from wire, sign stripped
? (System.currentTimeMillis() & 0xFFFFFFFF00000000L) | ((input.readInt() << 2) >> 2)
```

---

## How to Spot

Bit-field reconstruction of a wide integer from separately transmitted parts is error-prone. Review every bitmask that ORs or ANDs a literal: verify the mask isolates the intended bits, and verify that narrower integer reads are explicitly zero-extended or sign-stripped before being combined with a wider value.

---

## Test

No test added.
