**Commit**: 80e681790b · **Ticket**: KAFKA-17895
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR implements KIP-1250: Add metric to track size of in-memory state
stores. Today in Kafka Streams, we have the `estimate-num-keys` metric
for RocksDB, which tracks the number of keys in the state store at any
given moment. An equivalent metric is missing for in-memory state
stores, which we aim to fill with this KIP/PR. Reviewers: Bill Bejeck <bbejeck@apache.org>

Jira: https://issues.apache.org/jira/browse/KAFKA-17895  KIP:
https://cwiki.apache.org/confluence/x/noTMFw

**Before/After**
```java
// Before
// (no removals)

// After
    long numEntries() {
        return endTimeMap.values().stream()
            .flatMap(keyMap -> keyMap.values().stream())
            .mapToLong(Map::size)
            .sum();
    }

    long numEntries() {
        return segmentMap.values().stream()
            .mapToLong(Map::size)
            .sum();
    }
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17895: add num-keys metric for in-memory state stores (#21617).

**Test**
Test modified in StateStoreMetrics.java covering the corrected behaviour.
