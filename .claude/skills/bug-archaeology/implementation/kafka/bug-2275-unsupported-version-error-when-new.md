**Commit**: 5b3027dfcb · **Ticket**: KAFKA-15859
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Luke Chen <showuon@gmail.com>

**Before/After**
```java
// Before
    { "name": "TimeoutMs", "type": "int32", "versions": "10+",

// After
    { "name": "TimeoutMs", "type": "int32", "versions": "10+", "ignorable": true,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15859: fix the unsupported version error when new admin connects to old broker (#17358).

**Test**
No test added.
