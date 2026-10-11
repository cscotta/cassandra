**Commit**: ccdf176d05 · **Ticket**: KAFKA-16331
**Subsystem**: streams/test (StreamsProducerTest, RecordCollectorTest, StreamThreadStateStoreProviderTest)
**Category**: State not cleaned up

**Root cause**
`StreamsProducerTest`, `RecordCollectorTest`, and `StreamThreadStateStoreProviderTest` used `MockClientSupplier` to obtain `MockProducer` and other mock clients. Because `StreamsProducer` no longer accepts a client supplier (per-task producer support was removed), the supplier was an indirection that added complexity without benefit: tests had to configure the supplier, call `getProducer()`, and cast the result. Replacing the supplier with directly constructed `MockProducer` instances removes the unnecessary layer, makes test setup explicit, and eliminates the `MockClientSupplier` import from these files.

**Before/After**
```java
// Before
MockClientSupplier supplier = new MockClientSupplier();
supplier.setCluster(cluster);
MockProducer<byte[], byte[]> producer =
    (MockProducer<byte[], byte[]>) supplier.getProducer(config.originals());

// After
MockProducer<byte[], byte[]> producer =
    new MockProducer<>(cluster, true,
        new ByteArraySerializer(), new ByteArraySerializer());
```

**How to spot**
When a production class is refactored to remove a dependency (client supplier), find all tests that still create and configure the removed dependency purely to obtain a mock; replace with direct construction of the mock.

**Test**
Test refactoring is the entire change; `shouldCreateProducer` test removed as it tested the now-removed supplier path.
