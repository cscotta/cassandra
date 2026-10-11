**Commit**: 0799715f2c · **Ticket**: KAFKA-17795
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    <suppress checks="ImportControl" files="kafka/security/JaasTestUtils.java" />

// After
    <suppress checks="ImportControl" files="(JaasTestUtils).java" />
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17795 fix the file path of suppressions.xml to run build on windows (#17674).

**Test**
No test added.
