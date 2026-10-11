**Commit**: b154f58ce8 · **Ticket**: KAFKA-12829
**Subsystem**: streams/Topology
**Category**: State not cleaned up

**Root cause**
Two overloads of `Topology.addGlobalStore` accepted the deprecated `org.apache.kafka.streams.processor.ProcessorSupplier` (old PAPI) instead of the new `api.ProcessorSupplier`. They were deprecated in 2.7.0 and existed alongside the new overloads, which meant the processor supplied via the old API path was silently adapted via `ProcessorAdapter.adapt()`. Removing them forces users to migrate to the new API before 4.0, eliminating a hidden compatibility shim that wrapped old processors.

**Before/After**
```java
// Before
@Deprecated
public synchronized <K, V> Topology addGlobalStore(
    final StoreBuilder<?> storeBuilder,
    final String sourceName,
    final Deserializer<K> keyDeserializer,
    final Deserializer<V> valueDeserializer,
    final String topic,
    final String processorName,
    final org.apache.kafka.streams.processor.ProcessorSupplier<K, V> stateUpdateSupplier) {
    // adapts via ProcessorAdapter.adapt()
}

// After
// overloads removed; new ProcessorSupplier overload is the only path
```

**How to spot**
When a deprecated overload silently adapts the old API via an adapter class, removing it forces callers to discover the migration at compile time rather than at runtime. Look for `ProcessorAdapter.adapt()` calls inside deprecated methods — those are always candidates for removal in major versions.

**Test**
`shouldDriveGlobalStore` and related deprecated PAPI tests removed.
