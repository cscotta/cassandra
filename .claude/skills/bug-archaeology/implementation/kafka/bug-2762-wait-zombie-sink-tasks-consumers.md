**Commit**: e93242283d · **Ticket**: KAFKA-15917
**Subsystem**: connect/runtime
**Category**: Logic error in condition

**Root cause**
Reviewers: Yash Mayya <yash.mayya@gmail.com>, Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
        waitForCondition(() -> {
            ConnectorOffsets offsets = connect.connectorOffsets(connectorName);
            if (offsets.offsets().size() != expectedPartitions) {
                return false;
            }
            for (ConnectorOffset offset: offsets.offsets()) {
                assertEquals(expectedTopic, offset.partition().get(SinkUtils.KAFKA_TOPIC_KEY));
                if ((Integer) offset.offset().get(SinkUtils.KAFKA_OFFSET_KEY) != expectedOffset) {
                    return false;

// After

    @Override
    public String toString() {
        return "{" +
                "partition=" + partition +
                ", offset=" + offset +
                '}';
    }

    @Override
    public String toString() {
        return Objects.toString(offsets);
```

**How to spot**
When reviewing connect/runtime code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15917: wait for zombie sink tasks' consumers to commit offsets before trying to modify their offsets in integratio.

**Test**
Test modified in OffsetsApiIntegrationTest.java covering the corrected behaviour.
