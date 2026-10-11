**Commit**: c1ded05067 · **Ticket**: KAFKA-19446

**Subsystem**: storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerAppendInfo.java

**Category**: Logic error in condition

**Root cause**
`ProducerAppendInfo.checkProducerEpoch` used a single `producerEpoch < updatedEntry.producerEpoch()` check for all transaction markers. For Transaction Version 2 (TV2), the coordinator always increments the epoch before writing the final marker, so the invariant is strictly `markerEpoch > currentEpoch` — a marker with `markerEpoch == currentEpoch` is a stale duplicate that must be rejected. The legacy `< ` check accepted stale TV2 markers as valid, potentially conflating transactions under the same epoch and threatening EOS guarantees. The fix adds a `transactionVersion` parameter and applies `<= currentEpoch` rejection for TV2 while retaining `< currentEpoch` for legacy versions.

**Before**
```java
// Before: same epoch check for all versions
private void checkProducerEpoch(short producerEpoch, long offset) {
    if (producerEpoch < updatedEntry.producerEpoch()) {
        // reject
    }
}
```

**After**
```java
// After: strict > check for TV2
private void checkProducerEpoch(short producerEpoch, long offset,
                                short transactionVersion) {
    short current = updatedEntry.producerEpoch();
    boolean invalid = (transactionVersion >= 2)
        ? (producerEpoch <= current)
        : (producerEpoch < current);
    if (invalid) { /* reject */ }
}
```

**How to spot**
In `ProducerAppendInfo`, search for `checkProducerEpoch` calls that do not thread a `transactionVersion` parameter; on a TV2-capable cluster the epoch check must use strict inequality.

**Test**
Write a unit test that appends a TV2 end-transaction marker with `markerEpoch == currentEpoch` and verify it is rejected with an appropriate `InvalidProducerEpochException`.
