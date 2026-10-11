**Commit**: 53032d2e4f · **Ticket**: KAFKA-19361
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
`mapKey` only affects the in-memory data structure, not serialization:

  - In `MessageDataGenerator#generateVariableLengthReader`,
`isStructArrayWithKeys` only controls which collection class is
instantiated (`ImplicitLinkedHashMultiCollection` vs `ArrayList`), not
how bytes are read from the network. - In
`MessageDataGenerator#generateClassWriter`, the serialization logic has
no branch for `mapKey` or `hasKeys` — all fields are written the same
way regardless of `mapKey`. Therefore, adding or removing `mapKey` on an existing field is a
compatible    change, as demonstrated in #19815 where `mapKey` was added
to    already-released RPCs without bumping `validVersions`. Reviewers: Chia-Ping T...

**Before/After**
```java
// Before
// (no removals)

// After
Note that "mapKey" does not affect how the data is sent over the network.  It
only affects the in-memory data structure used by the generated code.  Adding or
removing "mapKey" on an existing field is not an incompatible change and does
not require a new version in "validVersions".

```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19361 document that `mapkey` does not break the serialization compatibility (#21441).

**Test**
No test added.
