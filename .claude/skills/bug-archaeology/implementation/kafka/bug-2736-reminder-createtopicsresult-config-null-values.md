**Commit**: 9b02260ac6 · **Ticket**: KAFKA-17090
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
     * Note that the values for the type and documentation fields will be null.
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17090 add reminder to createtopicsresult#config for null values of type and documentation (#16539).

**Test**
No test added.
