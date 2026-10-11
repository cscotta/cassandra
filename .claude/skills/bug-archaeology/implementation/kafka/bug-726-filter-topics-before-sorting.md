**Commit**: d1a821226c · **Ticket**: KAFKA-15873
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Partially addresses KAFKA-15873. When filtering and sorting, we should
be applying the filter before the sort of topics. Order that
unauthorizedForDescribeTopicMetadata is added to not relevant as it is a
HashSet. Reviewers: TaiJuWu <tjwu1217@gmail.com>, Calvin Liu
 <caliu@confluent.io>, Matthias J.

**Before/After**
```java
// Before
        Stream<String> authorizedTopicsStream = topics.stream().sorted().filter(topicName -> {
        });

// After
        Stream<String> authorizedTopicsStream = topics.stream().filter(topicName -> {
        }).sorted();
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15873: filter topics before sorting (#19304).

**Test**
No test added.
