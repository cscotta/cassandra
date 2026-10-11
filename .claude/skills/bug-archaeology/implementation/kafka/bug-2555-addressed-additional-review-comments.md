**Commit**: 011d35237c · **Ticket**: KAFKA-16752
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
                        // The request batch is a subset or per offset state is managed hence update

// After
                        // The request batch is a subset and requires per offset state hence initialize
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16752: [minor] addressed additional review comments (#16891).

**Test**
No test added.
