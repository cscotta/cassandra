## Commit

707a44a6cb — KAFKA-19068: ControlRecord constructor performs duplicate type check already done by RecordsIterator

## Ticket

KAFKA-19068

## Subsystem

KRaft / Raft records parsing

## Category

Logic error in condition

## Root cause

`RecordsIterator.decodeControlRecord` parsed the control record type from the key buffer and then passed both the type and the message to the `ControlRecord` public constructor. The constructor contained a second independent switch statement that re-validated that the type matched the message class, throwing `IllegalArgumentException` on mismatch. This dual check meant that valid type-message pairs passed through two identical switch statements unnecessarily, and any new control record type had to be added in two places — risking divergence. Making the constructor private and introducing a static factory `ControlRecord.of(ByteBuffer key, ByteBuffer value)` consolidates the type check into one place.

## Before / After

```java
// Before — constructor repeats type check already done by caller
public ControlRecord(ControlRecordType recordType, ApiMessage message) {
    if (!typeMatchesMessage(recordType, message))
        throw new IllegalArgumentException("type doesn't match message");
    ...
}

// After — private constructor, single type-check in factory
public static ControlRecord of(ByteBuffer key, ByteBuffer value) {
    ControlRecordType type = ControlRecordType.parse(key);
    ApiMessage message = switch (type) { ... };
    return new ControlRecord(type, message);  // private, no re-check
}
```

## How to spot

Search for constructors in record or event classes that validate a type-tag field by re-inspecting the same condition already checked by the caller factory. Any such double validation is a signal that the constructor should be made private and replaced with a static factory that owns the single point of validation.

## Test

`ControlRecordTest` was updated to use `ControlRecord.of(...)` and verify that unknown types throw `IllegalArgumentException` from the factory, not the constructor.
