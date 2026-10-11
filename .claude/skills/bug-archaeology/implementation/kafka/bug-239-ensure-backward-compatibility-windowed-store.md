**Commit**: 930f4eab43 · **Ticket**: KAFKA-20194
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
WindowHeaders-supplier must implement HeadersBytesStoreSupplier. Reviewers: Bill Bejeck <bbejeck@apache.org>, Alieh Saeedi
 <asaeedi@confluent.io>, TengYao Chi <frankvicky@apache.org>

**Before/After**
```java
// Before
        return persistentWindowStore(name, retentionPeriod, windowSize, retainDuplicates, RocksDbWindowBytesStoreSupplier.WindowStoreTypes.TIMESTAMPED_WINDOW_STORE_WITH_HEADERS);
        return new RocksDbWindowBytesStoreSupplier(
            name,
            retentionMs,
            defaultSegmentInterval,
            windowSizeMs,
            retainDuplicates,
            storeType
        );
    private final String name;
    private final long retentionPeriod;
    private final long segmentInterval;

// After
import org.apache.kafka.streams.state.internals.RocksDbWindowHeadersBytesStoreSupplier;
import static org.apache.kafka.streams.state.internals.RocksDbWindowBytesStoreSupplier.WindowStoreTypes.TIMESTAMPED_WINDOW_STORE_WITH_HEADERS;
        return persistentWindowStore(name, retentionPeriod, windowSize, retainDuplicates, TIMESTAMPED_WINDOW_STORE_WITH_HEADERS);
        if (storeType == TIMESTAMPED_WINDOW_STORE_WITH_HEADERS) {
            return new RocksDbWindowHeadersBytesStoreSupplier(
                name,
                retentionMs,
                defaultSegmentInterval,
                windowSizeMs,
                retainDuplicates
            );
        } else {
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20194: ensure backward compatibility for windowed store (#22004).

**Test**
No test added.
