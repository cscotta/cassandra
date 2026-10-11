**Commit**: 1ff7cf3d36 · **Ticket**: KAFKA-12832
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Josep Prat <josep.prat@aiven.io>

**Before/After**
```java
// Before
    @Deprecated
    @Override
    public int maxBackgroundCompactions() {
        return dbOptions.maxBackgroundCompactions();
    }

    @Deprecated
    public void setBaseBackgroundCompactions(final int baseBackgroundCompactions) {
        final String message = "This method has been removed from the underlying RocksDB. " +
                "It was not affecting compaction even in earlier versions. " +
                "It is currently a no-op method. " +
                "RocksDB decides the number of background compactions based on the maxBackgroundJobs(...) method";

// After
// (no additions)
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12832 remove deprecated methods in rocksdbgenericoptionstodboptionscolumnfamilyoptionsadapter (#16813).

**Test**
No test added.
