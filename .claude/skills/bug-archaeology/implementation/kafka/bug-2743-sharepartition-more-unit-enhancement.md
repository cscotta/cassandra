**Commit**: 67e6859632 · **Ticket**: KAFKA-17071
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The PR contains the following -

1. Added minor enhancement to not have a write state RPC call in case we have 0 states to be updated. 2. Removed unutilized function deliveryCount()
3.

**Before/After**
```java
// Before
            Exception exception = Errors.forCode(partitionData.errorCode()).exception();
        // Visible for testing.
        int deliveryCount() {
            return deliveryCount;
        }

        // third fetch request with 5 records starting from offset20.

// After
        if (stateBatches.isEmpty() && updatedStates.isEmpty())
            return;

            Exception exception = Errors.forCode(partitionData.errorCode()).exception(partitionData.errorMessage());
import static org.mockito.ArgumentMatchers.anyList;
        // Third fetch request with 5 records starting from offset 20.
    @Test
    public void testWriteShareGroupStateWithNullResponse() {
        Persister persister = Mockito.mock(Persister.class);
        mockPersisterReadStateMethod(persister);
        SharePartition sharePartition = SharePartitionBuilder.builder().withPersister(persister).build();

```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17071: sharepartition - add more unit tests and minor enhancement (#16530).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
