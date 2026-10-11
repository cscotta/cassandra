**Commit**: d350f603a4 · **Ticket**: KAFKA-18265

**Subsystem**: Share Groups / SharePartition

**Category**: Refactoring correctness (field access → method calls)

**Root cause**
`InFlightBatch`, `InFlightState`, and `RecordState` were moved from the `SharePartition` inner-class scope into a dedicated `share.fetch` package. Direct field accesses (`batch.state`, `batch.deliveryCount`, `batch.lastOffset`) that were valid for package-private inner members became inaccessible; all callers had to be updated to use the new accessor methods (`.state()`, `.deliveryCount()`, `.lastOffset()`, etc.). Missing these updates would cause compilation errors or, if fields were accidentally left public, bypassed encapsulation.

**Before/After**
```java
// Before: direct field access on inner class
if (batch.state == RecordState.ACQUIRED) {
    count += batch.deliveryCount;
}

// After: accessor methods after package extraction
if (batch.state() == RecordState.ACQUIRED) {
    count += batch.deliveryCount();
}
```

**How to spot**
After moving a nested class to its own package, search for any remaining direct field dereferences (`obj.field`) on instances of the moved type; they must be replaced with accessor methods. Compilation will catch most cases, but review code-generated or reflective accesses separately.

**Test**
`SharePartitionTest` exercises acquisition, delivery-count tracking, and state transitions to confirm that the refactored accessors return correct values.
