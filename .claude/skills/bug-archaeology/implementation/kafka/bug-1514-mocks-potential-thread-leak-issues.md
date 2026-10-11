**Commit**: dd1f2b8aab · **Ticket**: KAFKA-18653
**Subsystem**: core/src
**Category**: Silent skip on non-match

**Root cause**
Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
    DelayedShareFetch(
    boolean maybeAcquireFetchLock() {
            .withPartitionMaxBytesStrategy(PartitionMaxBytesStrategy.type(PartitionMaxBytesStrategy.StrategyType.UNIFORM))
            .withPartitionMaxBytesStrategy(PartitionMaxBytesStrategy.type(PartitionMaxBytesStrategy.StrategyType.UNIFORM))
            .withPartitionMaxBytesStrategy(PartitionMaxBytesStrategy.type(PartitionMaxBytesStrategy.StrategyType.UNIFORM))
            .withPartitionMaxBytesStrategy(PartitionMaxBytesStrategy.type(PartitionMaxBytesStrategy.StrategyType.UNIFORM))

// After
    /**
     * This function constructs an instance of delayed share fetch operation for completing share fetch requests instantaneously or with delay.
     * @param shareFetch - The share fetch parameters of the share fetch request.
     * @param replicaManager - The replica manager instance used to read from log/complete the request.
     * @param exceptionHandler - The handler to complete share fetch requests with exception.
     * @param sharePartitions - The share partitions referenced in the share fetch request.
     */
    public DelayedShareFetch(
    public boolean maybeAcquireFetchLock() {
        PartitionMaxBytesStrategy partitionMaxBytesStrategy = mockPartitionMaxBytes(Collections.singleton(tp0));

            .withPartitionMaxBytesStrategy(partitionMaxBytesStrategy)
```

**How to spot**
When reviewing core/src code, look for silent skip on non-match patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18653: fix mocks and potential thread leak issues causing silent rejectedexecutionexception in share group broker .

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
