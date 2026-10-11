**Commit**: 92d8d4bd1f · **Ticket**: KAFKA-16970
**Subsystem**: metadata/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            return Objects.hash(salt, storedKey, serverKey, iterations);
        return Objects.hash(salt, storedKey, serverKey, iterations);

// After
            return Objects.hash(
                Arrays.hashCode(salt),
                Arrays.hashCode(storedKey),
                Arrays.hashCode(serverKey),
                iterations
            );
        return Objects.hash(
            Arrays.hashCode(salt),
            Arrays.hashCode(storedKey),
            Arrays.hashCode(serverKey),
            iterations
        );
```

**How to spot**
When reviewing metadata/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16970 fix hash implementation of `scramcredentialvalue`, `scramcredentialdata`, and `contextualrecord` (#16359).

**Test**
Test modified in ScramCredentialValueTest.java covering the corrected behaviour.
