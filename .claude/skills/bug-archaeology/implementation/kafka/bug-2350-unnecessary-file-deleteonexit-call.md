**Commit**: a407cc3a31 · **Ticket**: KAFKA-17575
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
TestUtils.tempDirectory already registers a shutdown hook for deleting the temp directory. There's no reason to also call File.deleteOnExit, since that just registers another hook to do the same thing. Reviewers: TengYao Chi <kitingiao@gmail.com>, Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        file.deleteOnExit();

// After
// (no additions)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17575 remove unnecessary file.deleteonexit call (#17226).

**Test**
Test modified in TestUtils.java covering the corrected behaviour.
