**Commit**: f12a9d8413 · **Ticket**: KAFKA-19464
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The PR removes unnecessary updates for find next fetch offset. When the
state is in transition and not yet completed then anyways respective
offsets should not be considered for acquisition. The find next fetch
offset is updated finally when transition is completed. Reviewers: Manikumar Reddy <manikumar.reddy@gmail.com>, Abhinav Dixit
<adixit@confluent.io>

**Before/After**
```java
// Before
                if (updateResult == null) {

                // If the maxDeliveryCount limit has been exceeded, the record will be transitioned to ARCHIVED state.
                // This should not change the next fetch offset because the record is not available for acquisition
                if (updateResult.state() != RecordState.ARCHIVED) {
                    updateFindNextFetchOffset(true);
                }

            // If the maxDeliveryCount limit has been exceeded, the record will be transitioned to ARCHIVED state.
            // This should not change the next fetch offset because the record is not available for acquisition
            if (updateResult.state() != RecordState.ARCHIVED) {
                updateFindNextFetchOffset(true);

// After
                if (updateResult == null || updateResult.state() != RecordState.ACQUIRED) {
                // Do not update the next fetch offset as the offset has not completed the transition yet.
            // Do not update the next fetch offset as the batch has not completed the transition yet.
                if (updateResult == null || updateResult.state() != RecordState.ACQUIRED) {
                // Do not update the nextFetchOffset as the offset has not completed the transition yet.
            // Do not update the next fetch offset as the batch has not completed the transition yet.
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19464: remove unnecessary update for find next fetch offset (#20315).

**Test**
No test added.
