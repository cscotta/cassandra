**Commit**: efe7ccaf77 · **Ticket**: KAFKA-17058
**Subsystem**: group-coordinator/src
**Category**: Race condition / missing synchronization

**Root cause**
The group coordinator has (internal) write operations that could generate a large number of records (e.g. expiring offsets and groups). At the moment, those operations are limited by the maximum message size. If they hit it, they are basically stuck forever.

**Before/After**
```java
// Before
        return new CoordinatorResult<>(records);
        return new CoordinatorResult<>(records);
        this(records, response, null, true);
     * @param replayRecords The replayRecords.
        this(records, null, appendFuture, replayRecords);
     * @param replayRecords The replayRecords.
        this.records = Objects.requireNonNull(records);
        this.response = response;
        this.appendFuture = appendFuture;
        this.replayRecords = replayRecords;
     * Constructs a Result with records and a response.
     * @param records   A non-null list of records.

// After
        return new CoordinatorResult<>(records, false);
        return new CoordinatorResult<>(records, false);
    /**
     * The boolean indicating whether the records must be persisted
     * atomically.
     */
    private final boolean isAtomic;

    /**
     * Constructs a Result with records.
     *
     * @param records   A non-null list of records.
```

**How to spot**
When reviewing group-coordinator/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17058; extend coordinatorruntime to support non-atomic writes (#16498).

**Test**
Test modified in CoordinatorRuntimeTest.java covering the corrected behaviour.
