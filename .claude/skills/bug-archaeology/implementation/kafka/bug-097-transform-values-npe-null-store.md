**Commit**: 295760d3eb · **Ticket**: KAFKA-18917
**Subsystem**: streams/src/main/java/org/apache/kafka/streams/kstream/internals/KTableImpl.java
**Category**: Missing null / bounds check

**Root cause**: `transformValues(ValueTransformerWithKeySupplier, Materialized)` called `new KeyValueStoreMaterializer<>(materializedInternal)` unconditionally and passed the result to `new FactoryWrappingStoreBuilder<>(storeFactory)` even when `queryableStoreName` was `null` (i.e. the `Materialized` instance was not given a queryable store name). `FactoryWrappingStoreBuilder` later dereferenced the store factory name, yielding a `NullPointerException` at runtime. The Kafka 3.9 behaviour was to not materialise when no queryable name was present; the regression was introduced when the store-builder abstraction was added. The fix restores the conditional: only create the `StoreFactory` when `queryableStoreName != null`.

**Before/After**

```java
// Before – always creates store factory, NPE when name is null
final StoreFactory storeFactory = queryableStoreName != null
    ? (new KeyValueStoreMaterializer<>(materializedInternal)) : null;
storeBuilder = Collections.singleton(new FactoryWrappingStoreBuilder<>(storeFactory));

// After – skip materialisation when no queryable name
if (queryableStoreName != null) {
    final StoreFactory storeFactory = new KeyValueStoreMaterializer<>(materializedInternal);
    storeBuilder = Collections.singleton(new FactoryWrappingStoreBuilder<>(storeFactory));
} else {
    storeBuilder = null;
}
```

**How to spot**: When a `null` guard is moved from inside an expression to outside it, verify that the downstream object does not still receive `null` through a different path. A `null`-conditional ternary assigned to a local variable does not prevent the variable's consumer from crashing on `null`.

**Test**: `shouldCalculateCorrectOldValuesIfNotStatefulEvenNotMaterializedNoQueryableName` — calls `transformValues` with `Materialized.with(...)` but no queryable name and verifies that the topology runs without `NullPointerException`.
