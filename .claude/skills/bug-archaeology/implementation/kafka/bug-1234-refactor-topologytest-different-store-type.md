**Commit**: d6ff7b80dd · **Ticket**: KAFKA-13747
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Use `ParameterizedTest` to test both `IN_MEMORY` and `ROCKS_DB` store
type in `TopologyTest`. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
    @Test
    public void kGroupedStreamNamedMaterializedCountShouldPreserveTopologyStructure() {
                .withStoreType(Materialized.StoreType.IN_MEMORY));
        assertThat(topology.internalTopologyBuilder.setApplicationId("test").buildTopology().hasPersistentLocalStore(), is(false));
    @Test
    public void kGroupedStreamAnonymousMaterializedCountShouldPreserveTopologyStructure() {
                // set store type explicitly with default rocksDB
                .withStoreType(Materialized.StoreType.ROCKS_DB));
        assertThat(topology.internalTopologyBuilder.setApplicationId("test").buildTopology().hasPersistentLocalStore(), is(true));
    @Test
    public void kGroupedStreamAnonymousStoreTypedMaterializedCountShouldPreserveTopologyStructure() {
            .count(Materialized.as(Materialized.StoreType.IN_MEMORY));

// After
import org.apache.kafka.streams.kstream.Materialized.StoreType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
    @ParameterizedTest
    @EnumSource(StoreType.class)
    public void kGroupedStreamNamedMaterializedCountShouldPreserveTopologyStructure(final StoreType storeType) {
                .withStoreType(storeType));
        assertThat(topology.internalTopologyBuilder.setApplicationId("test").buildTopology().hasPersistentLocalStore(), is(storeType == StoreType.ROCKS_DB));
    @ParameterizedTest
    @EnumSource(StoreType.class)
    public void kGroupedStreamAnonymousMaterializedCountShouldPreserveTopologyStructure(final StoreType storeType) {
                .withStoreType(storeType));
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-13747: refactor topologytest to test different store type parametrized (#19323).

**Test**
Test modified in TopologyTest.java covering the corrected behaviour.
