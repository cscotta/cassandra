**Commit**: e88fede27e · **Ticket**: KAFKA-18025
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Develocity report: https://ge.apache.org/scans/tests?search.relativeStartTime=P28D&search.rootProjectNames=kafka&search.tags=github,trunk&search.timeZoneId=America%2FNew_York&tests.container=kafka.test.api.ShareConsumerTest#

Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
    @Flaky("KAFKA-18033")
    @Flaky("KAFKA-18033")
    @Flaky("KAFKA-18033")
    @Flaky("KAFKA-18033")
    @Flaky("KAFKA-18025")
    @Flaky("KAFKA-18033")
    @Flaky("KAFKA-18033")

// After
// (no additions)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18025 remove flaky tag from several tests in shareconsumertest (#18226).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
