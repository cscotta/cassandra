## Bug 505 — Docs: UUID fields cannot be null; zero-UUID sentinel not documented

**Commit:** 934094ff8a
**Ticket:** KAFKA-20020
**Subsystem:** clients / message schema docs
**Category:** Wrong serialization/deserialization

### Root cause
The `README.md` for Kafka's message schema generator stated that "Booleans, ints, floats and uuid can never be null" without explaining the consequence for UUIDs: because `UUIDFieldType.canBeNullable()` returns `false` (inherited default), UUID fields use the all-zeros UUID (`Uuid.ZERO_UUID`) as a sentinel for "no UUID" rather than a null value. Developers reading the docs would not know the correct idiom for representing an absent UUID and might attempt to use `null` (causing serialization errors) or not recognise the zero-UUID sentinel when parsing.

### Before
```
Booleans, ints, floats and uuid can never be null.
```

### After
```
Booleans, ints, floats and uuid can never be null. Uuid fields use a special zero uuid
value (all bits set to 0) as a sentinel to represent "no UUID" instead of null.
```

### How to spot
Any code that compares a UUID field against `null` to detect absence, or that tries to set a UUID field to `null`, is likely wrong. Use `Uuid.ZERO_UUID` (or `.equals(Uuid.ZERO_UUID)`) instead.

### Test
Documentation-only change; no automated test.
