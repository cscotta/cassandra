**Commit**: 04e6ef4750 · **Ticket**: KAFKA-15156
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        if (this.cipherInformation != null) {
            this.cipherInformation = cipherInformation;
        }

// After
        this.cipherInformation = cipherInformation;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15156 update cipherinformation correctly in defaultchannelmetadataregistry (#16169).

**Test**
Test modified in DefaultChannelMetadataRegistry.java covering the corrected behaviour.
