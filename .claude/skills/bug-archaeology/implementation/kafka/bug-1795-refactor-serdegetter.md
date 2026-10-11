**Commit**: e551cb7bb3 · **Ticket**: KAFKA-13722
**Subsystem**: streams/src
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Bruno Cadonna <cadonna@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    private final org.apache.kafka.streams.processor.ProcessorContext oldProcessorContext;
    private final org.apache.kafka.streams.processor.api.ProcessorContext newProcessorContext;
    private final StateStoreContext stateStorecontext;
    public SerdeGetter(final org.apache.kafka.streams.processor.ProcessorContext context) {
        oldProcessorContext = context;
        newProcessorContext = null;
        stateStorecontext = null;
    }
    public SerdeGetter(final org.apache.kafka.streams.processor.api.ProcessorContext context) {
        oldProcessorContext = null;
        newProcessorContext = context;
        stateStorecontext = null;

// After
import org.apache.kafka.streams.processor.api.ProcessorContext;

import java.util.function.Supplier;
    private final Supplier<Serde<?>> keySerdeSupplier;
    private final Supplier<Serde<?>> valueSerdeSupplier;

    public SerdeGetter(final ProcessorContext<?, ?> context) {
        keySerdeSupplier = context::keySerde;
        valueSerdeSupplier = context::valueSerde;

        keySerdeSupplier = context::keySerde;
        valueSerdeSupplier = context::valueSerde;
```

**How to spot**
When reviewing streams/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-13722: refactor serdegetter (#18242).

**Test**
Test modified in RocksDBTimeOrderedKeyValueBufferTest.java covering the corrected behaviour.
