**Commit**: 4e2f26bfc6 · **Ticket**: KAFKA-16917
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            return Collections.unmodifiableMap(topicFutures);
        return Collections.unmodifiableMap(topicFutures);
        return new DescribeConfigsResult(new HashMap<>(nodeFutures.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
    var describeResult = admin.describeConfigs(Seq(topicResource1, topicResource2).asJava)

// After
            return new HashMap<>(topicFutures);
        return new HashMap<>(topicFutures);
        return new DescribeConfigsResult(
            nodeFutures.entrySet()
                .stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (oldValue, newValue) -> {
                        // Duplicate keys should not occur, throw an exception to signal this issue
                        throw new IllegalStateException(String.format("Duplicate key for values: %s and %s", oldValue, newValue));
                    },
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16917 describetopicsresult should use mutable map in order to keep compatibility (#16250).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
