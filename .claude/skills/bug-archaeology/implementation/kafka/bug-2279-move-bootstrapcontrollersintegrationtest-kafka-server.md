**Commit**: 93e27c7413 · **Ticket**: KAFKA-17658
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
package kafka.test.server;

// After
package kafka.server;
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17658 move bootstrapcontrollersintegrationtest to kafka.server (#17356).

**Test**
Test modified in BootstrapControllersIntegrationTest.java covering the corrected behaviour.
