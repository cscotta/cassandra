**Commit**: d35ab4d27a · **Ticket**: KAFKA-18713
**Subsystem**: streams/src/main/java/org/apache/kafka/streams/kstream/internals/foreignkeyjoin/SubscriptionSendProcessorSupplier.java
**Category**: Race condition / missing synchronization

**Root cause**: When a left-table row's FK changed from one value to another, the processor sent both a delete subscription for the old FK with `DELETE_KEY_NO_PROPAGATE` and a new subscription for the new FK with `PROPAGATE_NULL_IF_NO_FK_VAL_AVAILABLE`. The delete subscription should not request a response from the right-hand side, but the old code used `DELETE_KEY_AND_PROPAGATE` for the delete in some update paths, which requested a response. If both responses arrived from the right side, they could be processed out of order, causing the wrong join result to be emitted. The fix ensures the "old FK delete" message always uses the no-propagate instruction.

**Before/After**

```java
// Before – delete could request a response, causing two concurrent responses
forward(record, oldForeignKey, DELETE_KEY_NO_PROPAGATE);
forward(record, newForeignKey, PROPAGATE_NULL_IF_NO_FK_VAL_AVAILABLE);

// After – only new FK subscription requests response
// (the refactored branching in the companion fix ensures DELETE_KEY_NO_PROPAGATE
//  is used for old FK and PROPAGATE_NULL_IF_NO_FK_VAL_AVAILABLE for new FK)
```

**How to spot**: In FK join subscription processing, any operation that deletes an old subscription must not simultaneously request a response. Two concurrent response requests for the same left-table key create a race whose winner determines the visible output.

**Test**: `doJoinFromLeftThenUpdateFkThenRevertBack` integration test — updates a left-table row's FK and then reverts it back, asserting that the output topic contains the expected sequence of values in order without spurious extra entries.
