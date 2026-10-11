**Commit**: 397ae598e6 · **Ticket**: KAFKA-17848
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
For delayed fetch, tryComplete can be called again after onComplete. As the requests are processed with parallel threads hence this scenario can occur. We attain locks in tryComplete which keeps pending as onComplete is never called when request is already completed. Reviewers: Abhinav Dixit <adixit@confluent.io>, Andrew Schofield <aschofield@confluent.io>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
        log.trace("Completing the delayed share fetch request for group {}, member {}, " +
                        "topic partitions {}", shareFetchData.groupId(),
            shareFetchData.memberId(), shareFetchData.partitionMaxBytes().keySet());
                topicPartitionData, shareFetchData.groupId(), shareFetchData.fetchParams());
        log.trace("Try to complete the delayed share fetch request for group {}, member {}, topic partitions {}",
            shareFetchData.groupId(), shareFetchData.memberId(),
            shareFetchData.partitionMaxBytes().keySet());

        if (!topicPartitionDataFromTryComplete.isEmpty())
            return forceComplete();
        log.info("Can't acquire records for any partition in the share fetch request for group {}, member {}, " +
                "topic partitions {}", shareFetchData.groupId(),

// After
        log.trace("Completing the delayed share fetch request for group {}, member {}, "
            + "topic partitions {}", shareFetchData.groupId(), shareFetchData.memberId(),
            topicPartitionDataFromTryComplete.keySet());
            topicPartitionData, shareFetchData.groupId(), shareFetchData.fetchParams());
        if (!topicPartitionDataFromTryComplete.isEmpty()) {
            boolean completedByMe = forceComplete();
            // If invocation of forceComplete is not successful, then that means the request is already completed
            // hence release the acquired locks.
            if (!completedByMe) {
                releasePartitionLocks(shareFetchData.groupId(), topicPartitionDataFromTryComplete.keySet());
            }
            return completedByMe;
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17848: fixing share purgatory request and locks handling (#17583).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
