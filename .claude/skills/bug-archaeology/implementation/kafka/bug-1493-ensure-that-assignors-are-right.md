**Commit**: d19b605210 · **Ticket**: KAFKA-18320
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
The full class name of the assignors if part of our public api. Hence, we should ensure that they are not changed by mistake. This patch adds a unit test verifying them. Reviewers: Sean Quah <squah@confluent.io>, Jeff Kim <jeff.kim@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
    @Test
    public void testConsumerGroupAssignorsDefault() {
        // The full class name of the assignors is part of our public api. Hence,
        // we should ensure that they are not changed by mistake.
        assertEquals(
            List.of(
                "org.apache.kafka.coordinator.group.assignor.UniformAssignor",
                "org.apache.kafka.coordinator.group.assignor.RangeAssignor"
            ),
            GroupCoordinatorConfig.CONSUMER_GROUP_ASSIGNORS_DEFAULT
        );
    }
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18320; ensure that assignors are at the right place (#18750).

**Test**
Test modified in GroupCoordinatorConfigTest.java covering the corrected behaviour.
