**Commit**: 42fd4d1d11 · **Ticket**: KAFKA-7776
**Subsystem**: connect/api
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
        java.util.Date current = new java.util.Date();
        t1 = Values.convertToTime(Timestamp.SCHEMA, current);
        assertEquals(new java.util.Date(currentMillis), t1);
        // ISO8601 strings - currently broken because tokenization breaks at colon
        java.util.Date t3 = Values.convertToTime(Time.SCHEMA, Long.toString(currentMillis));
        assertEquals(currentMillis, t3.getTime());
        java.util.Date t4 = Values.convertToTime(Time.SCHEMA, currentMillis);
        assertEquals(currentMillis, t4.getTime());
        d1 = Values.convertToDate(Timestamp.SCHEMA, currentDate);
        assertEquals(currentDate, d1);
        // ISO8601 strings - currently broken because tokenization breaks at colon
        java.util.Date d3 = Values.convertToDate(Date.SCHEMA, Long.toString(days));

// After
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
        LocalDateTime localTime = LocalDateTime.now();
        LocalTime localTimeTruncated = localTime.toLocalTime().truncatedTo(ChronoUnit.MILLIS);
        ZoneOffset zoneOffset = ZoneId.systemDefault().getRules().getOffset(localTime);
        java.util.Date current = new java.util.Date(localTime.toEpochSecond(zoneOffset) * 1000);
```

**How to spot**
When reviewing connect/api code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-7776: tests for iso8601 in connect value parsing (#18374).

**Test**
Test modified in ValuesTest.java covering the corrected behaviour.
