**Commit**: 88adb94c81 · **Ticket**: KAFKA-18093
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    /**
     * @return a map from topic names to futures which can be used to check the status of
     * individual deletions if the deleteTopics request used topic names. Otherwise return null.
     * @deprecated Since 3.0 use {@link #topicNameValues} instead
     */
    @Deprecated
    public Map<String, KafkaFuture<Void>> values() {
        return nameFutures;
    }


// After
                        <li>The <code>org.apache.kafka.clients.admin.DeleteTopicsResult.values()</code> method was removed.
                            Please use <code>org.apache.kafka.clients.admin.DeleteTopicsResult.topicNameValues()</code> instead.
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18093 remove deprecated deletetopicsresult#values (#18250).

**Test**
No test added.
