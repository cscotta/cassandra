**Commit**: b963e58000 · **Ticket**: KAFKA-16407
**Subsystem**: streams/src/main/java/org/apache/kafka/streams/kstream/internals/foreignkeyjoin/SubscriptionSendProcessorSupplier.java
**Category**: Logic error in condition

**Root cause**: `defaultJoinInstructions` checked `record.value().oldValue != null` but then computed `oldForeignKey` with a redundant `oldValue == null ? null :` guard that was dead code. More critically, the branching logic for transitions between null and non-null FK values was incorrect:  
1. When `oldForeignKey` was null (old record had no FK) and `newForeignKey` was non-null, the record was silently skipped instead of propagating the new FK subscription — breaking INNER joins when a row's FK changed from null.  
2. When `newForeignKey` was null (FK became null) the record was not sent with `DELETE_KEY_AND_PROPAGATE`, so the join result was never unset.  
The fix rewrites the branching with explicit cases for (null→non-null), (non-null→null), (different FK), and (same FK).

**Before/After**

```java
// Before – null oldForeignKey caused early return, missing propagation
if (oldForeignKey == null) {
    logSkippedRecordDueToNullForeignKey();
    return;
}

// After – correct dispatch
if (oldForeignKey == null && newForeignKey == null) {
    logSkippedRecordDueToNullForeignKey();
} else if (oldForeignKey == null) {
    forward(record, newForeignKey, PROPAGATE_ONLY_IF_FK_VAL_AVAILABLE);
} else if (newForeignKey == null) {
    forward(record, oldForeignKey, DELETE_KEY_AND_PROPAGATE);
} else if (!Arrays.equals(serialize(newForeignKey), serialize(oldForeignKey))) {
    forward(record, oldForeignKey, DELETE_KEY_NO_PROPAGATE);
    forward(record, newForeignKey, PROPAGATE_NULL_IF_NO_FK_VAL_AVAILABLE);
} else {
    forward(record, newForeignKey, PROPAGATE_ONLY_IF_FK_VAL_AVAILABLE);
}
```

**How to spot**: Multi-way null-transition logic is fragile when written as nested if/else chains. Review every combination of (old-null, new-non-null), (old-non-null, new-null), and (both-same/both-different) explicitly.

**Test**: `innerJoinShouldPropagateChangeFromNullFKToNonNullFK` and `innerJoinShouldDeleteAndPropagateChangeFromNonNullFKToNullFK` — each verifies that exactly one forwarded record with the correct subscription wrapper instruction is produced for the respective FK transition.
