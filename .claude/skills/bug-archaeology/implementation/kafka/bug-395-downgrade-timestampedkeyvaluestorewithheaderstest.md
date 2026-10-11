**Commit**: ab5a07abad · **Ticket**: KAFKA-20218
**Subsystem**: streams/integration-tests
**Category**: Logic error in condition

**Root cause**
Adding two integration tests to verify whether downgrading from header
key value store to timestamped key value store is possible. Directly
downgrading is not supported; the only option is to delete the store
entirely and restore it from the changelog. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
    private <K, V> void verifyLegacyTimestampedValue(final K key,
                                                     final V value,
                                                     final long timestamp)
        throws Exception {

        TestUtils.waitForCondition(
            () -> {
                try {
                    final ReadOnlyKeyValueStore<K, ValueAndTimestamp<V>> store =
                        IntegrationTestUtils.getStore(STORE_NAME, kafkaStreams, QueryableStoreTypes.timestampedKeyValueStore());

                    if (store == null) {
```

**How to spot**
When reviewing streams/integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20218: add downgrade tests in timestampedkeyvaluestorewithheaderstest (#21667).

**Test**
Test modified in HeadersStoreUpgradeIntegrationTest.java covering the corrected behaviour.
