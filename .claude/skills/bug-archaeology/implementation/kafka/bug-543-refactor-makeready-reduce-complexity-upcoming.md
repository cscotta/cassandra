**Commit**: 70b8af7e50 · **Ticket**: KAFKA-10357
**Subsystem**: streams/src
**Category**: State not cleaned up

**Root cause**
This PR reduces the cyclomatic complexity of the makeReady method by
extracting parts of its logic into smaller, well-named helper methods. This structural cleanup makes the code easier to understand and
maintain. These changes are intended to prepare for upcoming work related to
[KIP-698](https://cwiki.apache.org/confluence/display/KAFKA/KIP-698),
which will introduce additional branching logic to makeReady. Refactoring now ensures that future modifications can be made in a more
modular and testable way.

**Before/After**
```java
// Before
    static class ValidationResult {

                maybeThrowTimeoutException(
                    Arrays.asList(topicDescriptionsStillToValidate, topicConfigsStillToValidate),
                    deadline,
                    String.format("Could not validate internal topics within %d milliseconds. " +
                        "This can happen if the Kafka cluster is temporarily not available.", retryTimeoutMs)
                );
            final Map<String, List<TopicPartitionInfo>> existed = getTopicPartitionInfo(topicsToDescribe, null);
        long currentWallClockMs = time.milliseconds();
        Set<String> topicsNotReady = new HashSet<>(topics.keySet());
            topicsNotReady = validateTopics(topicsNotReady, topics, tempUnknownTopics);

// After
    public static class ValidationResult {

                maybeThrowTimeout(new TimeoutContext(
                        new HashSet<String>() {{
                            addAll(topicDescriptionsStillToValidate);
                            addAll(topicConfigsStillToValidate);
                        }},
                        deadline,
                        "Validation timeout",
                        String.format("Could not validate internal topics within %d milliseconds. " +
                            "This can happen if the Kafka cluster is temporarily not available.", retryTimeoutMs),
                        null
```

**How to spot**
When reviewing streams/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-10357: refactor makeready to reduce complexity and support upcoming kip-698 changes (#20326).

**Test**
Test modified in InternalTopicManagerTest.java covering the corrected behaviour.
