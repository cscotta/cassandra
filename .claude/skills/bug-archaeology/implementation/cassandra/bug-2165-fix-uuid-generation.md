# Bug 2165: Time-based UUID timestamp arithmetic uses subtraction instead of addition

**Commit**: f726cb23f5 · **Ticket**: CASSANDRA-5001
**Subsystem**: src/java/org/apache/cassandra/utils/UUIDGen.java
**Category**: Wrong constant / default value

---

## Root Cause

`UUIDGen.getAdjustedTimestamp()` converted a UUID v1 timestamp (100-nanosecond intervals since the Gregorian epoch, 15 Oct 1582) to a Unix millisecond timestamp by subtracting `START_EPOCH` instead of adding it. Since `START_EPOCH` is a negative constant representing the millisecond offset from the Gregorian epoch to the Unix epoch, subtracting it doubled the error and produced timestamps far in the past. Additionally, the clock-sequence-and-node LSB construction in `getClockSeqAndNode()` had incorrect bit-shift masks that did not conform to RFC 4122, producing malformed UUIDs with a wrong variant field.

### Before / After

```java
// BEFORE - wrong sign, produces timestamp ~122 years before Unix epoch
return (uuid.timestamp() / 10000) - START_EPOCH;
```

```java
// AFTER - correct addition since START_EPOCH is negative
return (uuid.timestamp() / 10000) + START_EPOCH;
```

```java
// BEFORE - incorrect bit masks for clock sequence and variant
lsb |= (clock & 0x3f00000000000000L) >>> 56;
lsb |= 0x0000000000000080;
lsb |= (clock & 0x00ff000000000000L) >>> 48;
lsb |= makeNode(addr);
```

```java
// AFTER - RFC 4122 compliant layout
lsb |= 0x8000000000000000L;                 // variant (2 bits)
lsb |= (clock & 0x0000000000003FFFL) << 48; // clock sequence (14 bits)
lsb |= makeNode(addr);                      // 6 bytes
```

---

## How to Spot

When a constant is defined with a sign chosen to make one direction of conversion work, double-check that every call site uses the complementary sign. A negative epoch-offset constant requires addition when converting to Unix time; using subtraction is the classic mistake. Also verify that bit-field constructions against a well-known standard (here RFC 4122) cover the exact bit positions specified.

---

## Test

A new `testUUIDTimestamp` test verifies that the millisecond timestamp extracted from a freshly-generated UUID is within 10 ms of `System.currentTimeMillis()`.
