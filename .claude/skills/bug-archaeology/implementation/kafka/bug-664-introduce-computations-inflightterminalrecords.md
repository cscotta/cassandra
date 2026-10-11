**Commit**: c31f699e67 · **Ticket**: KAFKA-19796
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This PR is part of

[KIP-1226](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1226%3A+Introducing+Share+Partition+Lag+Persistence+and+Retrieval). It adds the computation logic for inFlightTerminalRecords in share
partitions. This computation helps track records that are ACKNOWLEDGED
or ARCHIVED, forming the basis for accurately determining
share-partition lag.

**Before/After**
```java
// Before
// (no removals)

// After
import java.util.concurrent.atomic.AtomicInteger;
    /**
     * The inFlightTerminalRecords tracks the number of terminal (ACKNOWLEDGED / ARCHIVED) records within the
     * cachedState. This is used in the calculations for determining the current Share Partition lag.
     */
    private final AtomicInteger inFlightTerminalRecords;

        this.inFlightTerminalRecords = new AtomicInteger(0);
                    // During initialization, inFlightTerminalRecords is updated with the number of records that are in the
                    // ACKNOWLEDGED or ARCHIVED state.
                    if (isStateTerminal(RecordState.forId(stateBatch.deliveryState()))) {
                        inFlightTerminalRecords.addAndGet((int) (stateBatch.lastOffset() - stateBatch.firstOffset() + 1));
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19796: introduce computations for inflightterminalrecords (#20787).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
