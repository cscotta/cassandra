**Commit**: 4b80591df2 · **Ticket**: KAFKA-15549
**Subsystem**: LICENSE-binary
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
swagger-annotations-2.2.8
# New version of Swagger 2.2.14 requires minimum JDK 11.
swaggerVersion=2.2.8

// After
swagger-annotations-2.2.25
swaggerVersion=2.2.25
```

**How to spot**
When reviewing LICENSE-binary code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15549 bump swagger dependency version from 2.2.8 to 2.2.25 (#17730).

**Test**
No test added.
