**Commit**: 1a3dce72fa · **Ticket**: KAFKA-18289
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    @Deprecated
    protected DescribeTopicsResult(Map<String, KafkaFuture<TopicDescription>> futures) {
        this(null, futures);
    }

    /**
     * @return a map from topic names to futures which can be used to check the status of
     *         individual topics if the request used topic names, otherwise return null.
     *
     * @deprecated Since 3.1.0 use {@link #topicNameValues} instead
     */
    @Deprecated

// After
                        <li>The <code>org.apache.kafka.clients.admin.DescribeTopicsResult.DescribeTopicsResult(Map&lt;String, KafkaFuture&lt;TopicDescription&gt;&gt;)</code> method was removed.
                            Please use <code>org.apache.kafka.clients.admin.DescribeTopicsResult.DescribeTopicsResult(Map&lt;Uuid, KafkaFuture&lt;TopicDescription&gt;&gt;, Map&lt;String, KafkaFuture&lt;TopicDescription&gt;&gt;)</code> instead.
                        </li>
                        <li>
                            The <code>values()</code> method was removed from the <code>org.apache.kafka.clients.admin.DescribeTopicsResult</code>.
                            Please use <code>topicNameValues()</code> instead.
                        </li>
                        <li>
                            The <code>all()</code> method was removed from the <code>org.apache.kafka.clients.admin.DescribeTopicsResult</code>.
                            Please use <code>allTopicNames()</code> instead.
                        </li>
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18289 remove deprecated methods of describetopicsresult (#18255).

**Test**
No test added.
