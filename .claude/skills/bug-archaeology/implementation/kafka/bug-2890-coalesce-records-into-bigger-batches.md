**Commit**: 638844f833 · **Ticket**: KAFKA-16770
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This patch is the continuation of https://github.com/apache/kafka/pull/15964. It introduces the records coalescing to the CoordinatorRuntime. It also introduces a new configuration `group.coordinator.append.linger.ms` which allows administrators to chose the linger time or disable it with zero. The new configuration defaults to 10ms.

**Before/After**
```java
// Before
// (no removals)

// After
    /**
     * Check if we have room for a given number of bytes.
     */
    public boolean hasRoomFor(int estimatedRecordsSize) {
        if (isFull()) return false;
        return this.writeLimit >= estimatedBytesWritten() + estimatedRecordsSize;
    }

    public int maxAllowedBytes() {
        return this.writeLimit - this.batchHeaderSizeInBytes;
    }

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16770; [2/2] coalesce records into bigger batches (#16215).

**Test**
Test modified in GroupCoordinatorConfigTest.java covering the corrected behaviour.
