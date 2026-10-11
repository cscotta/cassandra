**Commit**: 94fc9adb5e · **Ticket**: KAFKA-20194
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
Adds missing method TopologyTestDriver.getSessionStoreWithHeader(), and
updates  TopologyTestDriverTest for all newly added header store types. Reviewers: TengYao Chi <frankvicky@apache.org>, Alieh Saeedi
 <asaeedi@confluent.io>

**Before/After**
```java
// Before
              files="(EosIntegrationTest|KStreamKStreamJoinTest|RocksDBWindowStoreTest).java"/>
              files="(KStreamKStreamJoinTest|StreamThreadTest|TaskManagerTest|StreamTaskTest).java"/>
    <suppress checks="MethodLength"
              files="(KStreamSlidingWindowAggregateTest|KStreamKStreamLeftJoinTest|KStreamKStreamOuterJoinTest|KTableKTableForeignKeyJoinIntegrationTest).java"/>

            final SessionBytesStoreSupplier supplier,
            final Serde<K> keySerde,
            final Serde<V> valueSerde

                return (List<T>) Collections.singletonList(new GenericReadOnlyKeyValueStoreFacade<>((TimestampedKeyValueStoreWithHeaders<Object, Object>) store, ValueConverters.extractValueFromHeaders()));
                return (List<T>) Collections.singletonList(new GenericReadOnlyKeyValueStoreFacade<>((TimestampedKeyValueStoreWithHeaders<Object, Object>) store, ValueConverters.extractValueAndTimestampFromHeaders()));
            } else {

// After
              files="(EosIntegrationTest|KStreamKStreamJoinTest|KStreamKStreamLeftJoinTest|KStreamKStreamOuterJoinTest|KStreamSlidingWindowAggregateTest|KTableKTableForeignKeyJoinIntegrationTest|RocksDBWindowStoreTest|TopologyTestDriverTest).java"/>

    <suppress checks="ParameterNumber"
              files="(TopologyTestDriverTest).java"/>
              files="(KStreamKStreamJoinTest|StreamTaskTest|StreamThreadTest|TaskManagerTest|TopologyTestDriverTest).java"/>
        final SessionBytesStoreSupplier supplier,
        final Serde<K> keySerde,
        final Serde<V> valueSerde
                return (List<T>) Collections.singletonList(new GenericReadOnlyKeyValueStoreFacade<>((TimestampedKeyValueStoreWithHeaders<?, ?>) store, ValueConverters.extractValueFromHeaders()));
                return (List<T>) Collections.singletonList(new GenericReadOnlyKeyValueStoreFacade<>((TimestampedKeyValueStoreWithHeaders<?, ?>) store, ValueConverters.extractValueAndTimestampFromHeaders()));
            return (Li
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20194: add sesssion-header-store support to tdd (#21956).

**Test**
Test modified in GlobalStateStoreProvider.java covering the corrected behaviour.
