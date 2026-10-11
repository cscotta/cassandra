**Commit**: 23de98cdc5 · **Ticket**: KAFKA-17554
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import org.junit.jupiter.api.Disabled;
    @Disabled("KAFKA-17554")
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17554 disable testfuturecompletionoutsidepoll (#18138).

**Test**
Test modified in ConsumerNetworkClientTest.java covering the corrected behaviour.
