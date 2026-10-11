**Commit**: 09e8fa2dbe · **Ticket**: KAFKA-18026
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Covers wrapping of processors and state stores for KStream-KTable joins

Reviewers: Almog Gavra <almog@responsive.dev>, Guozhang Wang <guozhang.wang.us@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.streams.processor.internals.StoreBuilderWrapper;
            new StreamTableJoinNode<>(name, processorParameters, new String[] {}, null, null, Optional.empty());
        Optional<String> bufferStoreName = Optional.empty();
            bufferStoreName = Optional.of(name + "-Buffer");
            final RocksDBTimeOrderedKeyValueBuffer.Builder<Object, Object> storeBuilder =
                    new RocksDBTimeOrderedKeyValueBuffer.Builder<>(bufferStoreName.get(), joinedInternal.gracePeriod(), name);
            builder.addStateStore(StoreBuilderWrapper.wrapStoreBuilder(storeBuilder));
            bufferStoreName);
            joinedInternal.gracePeriod(),
            bufferStoreName

                      final Optional<String> storeName) {

// After
import org.apache.kafka.streams.state.StoreBuilder;
            new StreamTableJoinNode<>(name, processorParameters, new String[] {}, null, null);
        Optional<StoreBuilder<?>> bufferStoreBuilder = Optional.empty();
            final String bufferName = name + "-Buffer";
            bufferStoreBuilder = Optional.of(new RocksDBTimeOrderedKeyValueBuffer.Builder<>(bufferName, joinedInternal.gracePeriod(), name));
            bufferStoreBuilder
        );
            joinedInternal.gracePeriod()
import org.apache.kafka.streams.state.StoreBuilder;
import java.util.Set;

import static java.util.Collections.singleton;
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18026: kip-1112, migrate stream-table joins to use processersupplier#stores (#18047).

**Test**
Test modified in StreamsBuilderTest.java covering the corrected behaviour.
