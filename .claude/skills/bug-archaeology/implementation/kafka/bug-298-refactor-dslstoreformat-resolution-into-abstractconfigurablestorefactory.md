**Commit**: 5a94cd695e · **Ticket**: KAFKA-20279
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Move the control flow to pick the default store format vs an overwritten
format into the base class. Reviewers: Matthias J. Sax <matthias@confluent.io>, TengYao Chi
<frankvicky@apache.org>, Alieh Saeedi <asaeedi@confluent.io>

**Before/After**
```java
// Before
    private DslStoreFormat dslStoreFormat;
    public AbstractConfigurableStoreFactory(final DslStoreSuppliers initialStoreSuppliers) {
            dslStoreFormat = DslStoreFormat.HEADERS;
        // else dslStoreFormat remains null and the lower layers decide between PLAIN and TIMESTAMPED
        return dslStoreFormat;
        super(materialized);
        final DslStoreFormat storeFormat = dslStoreFormat() == null ? DslStoreFormat.TIMESTAMPED : dslStoreFormat();
                ? dslStoreSuppliers().keyValueStore(new DslKeyValueParams(materialized.storeName(), storeFormat))
    public MaterializedStoreFactory(final MaterializedInternal<K, V, S> materialized) {
        super(materialized.dslStoreSuppliers().orElse(null));

// After
    private final DslStoreFormat defaultStoreDefaultFormat;
    private DslStoreFormat dslStoreFormatOverwrite;
    public AbstractConfigurableStoreFactory(final DslStoreSuppliers initialStoreSuppliers,
                                            final DslStoreFormat defaultStoreDefaultFormat) {
        this.defaultStoreDefaultFormat = defaultStoreDefaultFormat;
            dslStoreFormatOverwrite = DslStoreFormat.HEADERS;
        return dslStoreFormatOverwrite == null ? defaultStoreDefaultFormat : dslStoreFormatOverwrite;
        super(materialized, DslStoreFormat.TIMESTAMPED);
                ? dslStoreSuppliers().keyValueStore(new DslKeyValueParams(materialized.storeName(), dslStoreFormat()))
import org.apache.kafka.streams.DslStoreFormat;
    public MaterializedStoreFactory(final MaterializedInternal<K, V, S> materialized,
                                    final DslStoreFormat defaultStoreFormat) {
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20279: refactor dslstoreformat resolution into abstractconfigurablestorefactory (#21804).

**Test**
No test added.
