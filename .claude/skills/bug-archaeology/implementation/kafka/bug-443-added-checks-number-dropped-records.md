**Commit**: b974ccd8d2 · **Ticket**: KAFKA-19096
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Follow up to https://github.com/apache/kafka/pull/20605 to improve test
coverage. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
        Assertions.assertEquals(1.0, getDroppedRecordsTotalMetric(context));

// After
import static org.junit.jupiter.api.Assertions.assertEquals;

        // test dropped-records sensors
        assertEquals(0.0, getDroppedRecordsTotalMetric(context));

        // test dropped-records sensors
        assertEquals(0.0, getDroppedRecordsTotalMetric(context));

        // test dropped-records sensors
        assertEquals(0.0, getDroppedRecordsTotalMetric(context));

        // test dropped-records sensors
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19096: added checks for the number of dropped records to foreign key join tests. (#21111).

**Test**
Test modified in ForeignTableJoinProcessorSupplierTests.java covering the corrected behaviour.
