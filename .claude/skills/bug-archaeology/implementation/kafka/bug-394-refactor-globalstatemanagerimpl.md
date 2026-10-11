**Commit**: da8e496291 · **Ticket**: KAFKA-20257
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
The main motivation for this is to better align `GlobalStateManagerImpl`
with `ProcessorStateManager` where we can, as it will make implementing
elements of KIP-1035 easier, and ultimately lead to easier to maintain
code. The primary changes here are:

1. To encapsulate store metadata in
   `GlobalStateManagerImpl.StateStoreMetadata`. 2.

**Before/After**
```java
// Before
        log.info("Restoring state for global store {}", store.name());
        try {
            final Optional<InternalTopologyBuilder.ReprocessFactory<?, ?, ?, ?>> reprocessFactory = topology
                .storeNameToReprocessOnRestore().getOrDefault(store.name(), Optional.empty());
            if (reprocessFactory.isPresent()) {
                reprocessState(
                    topicPartitions,
                    highWatermarks,
                    reprocessFactory.get(),
                    store.name());
            } else {
                restoreState(

// After

    private static class StateStoreMetadata {
        final StateStore stateStore;
        final List<TopicPartition> changelogPartitions;
        final StateRestoreCallback restoreCallback;
        final Optional<InternalTopologyBuilder.ReprocessFactory<?, ?, ?, ?>> reprocessFactory;
        final RecordConverter recordConverter;
        final Map<TopicPartition, Long> highWatermarks;

        StateStoreMetadata(final StateStore stateStore,
                           final List<TopicPartition> changelogPartitions,
                           final Optional<InternalTopologyBuilder.ReprocessFactory<?, ?, ?, ?>> reprocessFactory,
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20257: refactor globalstatemanagerimpl (#21649).

**Test**
Test modified in GlobalStateManagerImplTest.java covering the corrected behaviour.
