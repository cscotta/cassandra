**Commit**: 18b8b992f9 · **Ticket**: KAFKA-17870
**Subsystem**: metadata/src
**Category**: Logic error in condition

**Root cause**
We fail the entire CreateTopicsRequest action if there are more than 10k total
partitions being created in this topic for this specific request. The usual pattern for
this API to try and succeed with some topics. Since the 10k limit applies to all topics
then no topic should be created if they all exceede it. Reviewers: Colin P.

**Before/After**
```java
// Before
        assertEquals("Unable to perform excessively large batch operation.",

// After
        assertEquals("Excessively large number of partitions per request.",
    static final int MAX_PARTITIONS_PER_BATCH = 10_000;
        validateTotalNumberOfPartitions(request, defaultNumPartitions);

    /**
     * Validates that a batch of topics will create less than {@value MAX_PARTITIONS_PER_BATCH}. Exceeding this number of topics per batch
     * has led to out-of-memory exceptions. We use this validation to fail earlier to avoid allocating the memory.
     * Validates an upper bound number of partitions. The actual number may be smaller if some topics are misconfigured.
     *
     * @param request a batch of topics to create.
     * @param defaultNumPartitions default number of partitions to assign if unspecified.
     * @throws PolicyViolationException if total number of partitions exceeds {@value MAX_PARTITIONS_PER_BATCH}.
```

**How to spot**
When reviewing metadata/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: [kafka-17870] fail createtopicsrequest if total number of partitions exceeds 10k (#17604).

**Test**
Test modified in KRaftClusterTest.scala covering the corrected behaviour.
