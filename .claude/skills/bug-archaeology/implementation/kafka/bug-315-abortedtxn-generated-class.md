## Commit
70dfc4236c

## Ticket
KAFKA-19562

## Subsystem
Storage / Transaction Index

## Category
Wrong serialization / deserialization

## Root cause
`AbortedTxn` was a hand-written class that used `ByteBuffer` field comparison in `equals()`. Because `ByteBuffer.equals()` compares remaining bytes positionally, two logically identical records could compare unequal after a `ByteBuffer.rewind()` or partial read. The fix replaces the hand-written class with a generated protocol class (`AbortedTxn.json`) that copies fields at construction time and compares by value.

## Before
```java
// AbortedTxn.java
public boolean equals(Object o) {
    return buffer.equals(((AbortedTxn) o).buffer); // ByteBuffer position-sensitive
}
```

## After
```java
// Generated from AbortedTxn.json — copies producerId, firstOffset, lastOffset, lastStableOffset
// equals() compares fields by value, not buffer position
```

## How to spot
Any `equals()` that delegates to `ByteBuffer.equals()` without first calling `rewind()` is fragile. Prefer generated protocol classes for wire-format types to avoid hand-maintaining equality semantics.

## Test
`TransactionIndexTest` verifies round-trip serialization and correct equality after the migration.
