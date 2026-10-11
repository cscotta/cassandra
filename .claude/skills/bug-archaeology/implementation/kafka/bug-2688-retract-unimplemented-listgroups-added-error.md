**Commit**: 28f6fbdd55 · **Ticket**: KAFKA-17144
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  //
  // Version 6 adds support for share groups (KIP-932).
  "validVersions": "0-6",
  //
  // Version 6 adds support for share groups (KIP-932).
  "validVersions": "0-6",

// After
  "validVersions": "0-5",
  "validVersions": "0-5",
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17144 retract unimplemented listgroups v6 added in error (#16635).

**Test**
No test added.
