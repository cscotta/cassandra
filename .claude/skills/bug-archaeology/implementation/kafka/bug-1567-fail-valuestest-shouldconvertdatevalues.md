**Commit**: b4674c002f · **Ticket**: KAFKA-18585
**Subsystem**: connect/api
**Category**: Logic error in condition

**Root cause**
Reviewers: Divij Vaidya <diviv@amazon.com>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
        java.util.Date current = new java.util.Date();
        assertEquals(currentDate, d3);

// After
        LocalDateTime localTime = LocalDateTime.now();
        ZoneOffset zoneOffset = ZoneId.systemDefault().getRules().getOffset(localTime);
        java.util.Date current = new java.util.Date(localTime.toEpochSecond(zoneOffset) * 1000);
        LocalDateTime localTimeTruncated = localTime.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime date3 = LocalDateTime.ofInstant(Instant.ofEpochMilli(d3.getTime()), ZoneId.systemDefault());
        assertEquals(localTimeTruncated, date3);
```

**How to spot**
When reviewing connect/api code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18585 fix fail test valuestest#shouldconvertdatevalues (#18611).

**Test**
Test modified in ValuesTest.java covering the corrected behaviour.
