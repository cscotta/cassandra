**Commit**: 671cbedc1b · **Ticket**: KAFKA-18219
**Subsystem**: metadata/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Kuan-Po Tseng <brandboat@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                    log.error("Triggering unclean leader election for offline partition {}-{}.",

// After
                    log.info("Triggering unclean leader election for offline partition {}-{}.",
```

**How to spot**
When reviewing metadata/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18219 use info level instead of error after successfully performing an unclean leader election (#18159).

**Test**
No test added.
