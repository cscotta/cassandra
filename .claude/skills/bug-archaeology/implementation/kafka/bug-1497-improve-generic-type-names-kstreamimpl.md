**Commit**: 0d1e7e04b2 · **Ticket**: KAFKA-18644
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Bill Bejeck <bill@confluent.io>, Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
        final ProcessorSupplier<? super K, ? super V, KOut, VOut> processorSupplier,
        final ProcessorSupplier<? super K, ? super V, KOut, VOut> processorSupplier,
        final FixedKeyProcessorSupplier<? super K, ? super V, VOut> processorSupplier,
        final FixedKeyProcessorSupplier<? super K, ? super V, VOut> processorSupplier,
    KTable<K, V> filter(final Predicate<? super K, ? super V> predicate, final Named named);
    KTable<K, V> filterNot(final Predicate<? super K, ? super V> predicate, final Named named);
    <KR, VR> KGroupedTable<KR, VR> groupBy(final KeyValueMapper<? super K, ? super V, KeyValue<KR, VR>> selector);
    <KR, VR> KGroupedTable<KR, VR> groupBy(final KeyValueMapper<? super K, ? super V, KeyValue<KR, VR>> selector,
                                    final Function<V, KO> foreignKeyExtractor,
                                    final ValueJoiner<V, VO, VR> joiner);
                                    final BiFunction<K, V, KO> foreignKeyExtractor,


// After
        final ProcessorSupplier<? super K, ? super V, ? extends KOut, ? extends VOut> processorSupplier,
        final ProcessorSupplier<? super K, ? super V, ? extends KOut, ? extends VOut> processorSupplier,
        final FixedKeyProcessorSupplier<? super K, ? super V, ? extends VOut> processorSupplier,
        final FixedKeyProcessorSupplier<? super K, ? super V, ? extends VOut> processorSupplier,
    KTable<K, V> filter(final Predicate<? super K, ? super V> predicate,
                        final Named named);
    KTable<K, V> filterNot(final Predicate<? super K, ? super V> predicate,
                           final Named named);
    <KR, VR> KGroupedTable<KR, VR> groupBy(final KeyValueMapper<? super K, ? super V, ? extends KeyValue<? extends KR, ? extends VR>> selector);
    <KR, VR> KGroupedTable<KR, VR> groupBy(final KeyValueMapper<? super K, ? super V, ? extends KeyValue<? extends KR, ? extends VR>> selector,
                                    final Function<? super V, ? extends KO> foreignKeyExtractor,
                                    final ValueJoiner<? super V, ? super VO, ? extends VR> joiner);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18644: improve generic type names for kstreamimpl and ktableimpl (#18722).

**Test**
No test added.
