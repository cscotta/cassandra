**Commit**: 236f3d422f · **Ticket**: KAFKA-17496
**Subsystem**: jmh-benchmarks/src
**Category**: Logic error in condition

**Root cause**
Bring the homogeneous case from ServerSideAssignorBenchmark to TargetAssignmentBuilderBenchmark. Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
            .withSubscriptionType(HOMOGENEOUS)
            HOMOGENEOUS,
        return AssignorBenchmarkUtils.createHomogeneousMembers(
            memberCount - 1,
            this::memberId,
            this::rackId,
            allTopicNames
        );

// After
    /** The number of homogeneous subgroups to create for the heterogeneous subscription case. */
import org.apache.kafka.coordinator.group.api.assignor.SubscriptionType;
    @Param({"HOMOGENEOUS", "HETEROGENEOUS"})
    private SubscriptionType subscriptionType;

    /** The number of homogeneous subgroups to create for the heterogeneous subscription case. */
    private static final int MAX_BUCKET_COUNT = 5;

            .withSubscriptionType(subscriptionType)
            subscriptionType,
        if (subscriptionType == HOMOGENEOUS) {
            return AssignorBenchmarkUtils.createHomogeneousMembers(
```

**How to spot**
When reviewing jmh-benchmarks/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17496: add heterogeneous case to targetassignmentbuilderbenchmark (#17277).

**Test**
No test added.
