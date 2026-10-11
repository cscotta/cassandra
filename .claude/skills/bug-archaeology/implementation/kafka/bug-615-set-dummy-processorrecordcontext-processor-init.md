**Commit**: c635a76480 · **Ticket**: KAFKA-19638
**Subsystem**: streams/src
**Category**: Missing null / bounds check

**Root cause**
This PR fixes a `NullPointerException` in the `Processor#init` when the
topology is initializing. This issue was introduced via KAFKA-13722
(commit reverted for 4.1.0 release). As part of the testing strategy, I added a new test case for the
`ProcessorTopologyTest` class, and also manually tested. Reviewers: Matthias J.

**Before/After**
```java
// Before
            processorContext.setCurrentNode(node);
                processorContext.setCurrentNode(null);

// After
    protected static final long NO_OFFSET = -1;
    protected static final int NO_PARTITION = -1;
            final ProcessorRecordContext initContext = new ProcessorRecordContext(time.milliseconds(), NO_OFFSET, NO_PARTITION, null, new RecordHeaders());
            updateProcessorContext(node, time.milliseconds(), initContext);
                updateProcessorContext(null, ConsumerRecord.NO_TIMESTAMP, null);

    @Test
    public void testTopologyInitializationWithInitialKeyAndValue() {
        final String initialKey = "key1";
        final String initialValue = "value1";
        final StoreBuilder<KeyValueStore<String, String>> storeBuilder =
                Stores.keyValueStoreBuilder(Stores.inMemoryKeyValueStore(DEFAULT_STORE_NAME), Serdes.String(), Serdes.String());
```

**How to spot**
When reviewing streams/src code, look for missing null / bounds check patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19638: set dummy processorrecordcontext for processor init (#20403).

**Test**
Test modified in ProcessorTopologyTest.java covering the corrected behaviour.
