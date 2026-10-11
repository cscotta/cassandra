**Commit**: ba774a09f4 · **Ticket**: KAFKA-8862
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
We should provide the same informative error message for both timeout
cases. Reviewers: Kirk True <ktrue@confluent.io>, Andrew Schofield <aschofield@confluent.io>, Ismael Juma <ismael@juma.me.uk>

**Before/After**
```java
// Before
                final String errorMessage = String.format("Topic %s not present in metadata after %d ms.",
                        topic, maxWaitMs);
                final String errorMessage = partitionsCount == null ?
                        String.format("Topic %s not present in metadata after %d ms.",
                                topic, maxWaitMs) :
                        String.format("Partition %d of topic %s with partition count %d is not present in metadata after %d ms.",
                                partition, topic, partitionsCount, maxWaitMs);

// After
                final String errorMessage = getErrorMessage(partitionsCount, topic, partition, maxWaitMs);
                final String errorMessage = getErrorMessage(partitionsCount, topic, partition, maxWaitMs);
    private String getErrorMessage(Integer partitionsCount, String topic, Integer partition, long maxWaitMs) {
        return partitionsCount == null ?
            String.format("Topic %s not present in metadata after %d ms.",
                topic, maxWaitMs) :
            String.format("Partition %d of topic %s with partition count %d is not present in metadata after %d ms.",
                partition, topic, partitionsCount, maxWaitMs);
    }
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-8862: improve producer error message for failed metadata update (#18587).

**Test**
No test added.
