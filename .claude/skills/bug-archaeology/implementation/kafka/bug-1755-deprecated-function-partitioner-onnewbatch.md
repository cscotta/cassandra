**Commit**: e6d2421136 · **Ticket**: KAFKA-18295
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    /**
     * Call deprecated {@link Partitioner#onNewBatch}
     */
    @SuppressWarnings("deprecation")
    private void onNewBatch(String topic, Cluster cluster, int prevPartition) {
        assert partitioner != null;
        partitioner.onNewBatch(topic, cluster, prevPartition);
    }

            // A custom partitioner may take advantage on the onNewBatch callback.
            boolean abortOnNewBatch = partitioner != null;


// After
                    serializedValue, headers, appendCallbacks, remainingWaitMs, nowMs, cluster);
            // unknown. Note that the `Sender` will refuse to dequeue
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18295 remove deprecated function partitioner#onnewbatch (#18282).

**Test**
Test modified in KafkaProducerTest.java covering the corrected behaviour.
